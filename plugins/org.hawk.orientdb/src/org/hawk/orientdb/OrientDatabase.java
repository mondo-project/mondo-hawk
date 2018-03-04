/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.cache.ORecordCacheGuava;
import org.hawk.orientdb.indexes.OrientNodeIndex;
import org.hawk.orientdb.indexes.OrientNodeIndex.PostponedIndexAdd;
import org.hawk.orientdb.util.OrientClusterDocumentIterable;
import org.hawk.orientdb.util.OrientNameCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.factory.OConfigurableStatefulFactory;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.cache.ORecordCache;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.intent.OIntentMassiveRead;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * OrientDB backend for Hawk. It is based on the Document API as it has better
 * performance than their GraphDB implementation (probably because it tries to
 * mimic Blueprints too much), but it follows the same conventions as their
 * GraphDB, so graphs can be queried and viewed as usual.
 *
 * NOTE: it is recommended to set the {@link OGlobalConfiguration#WAL_LOCATION}
 * system property to a different filesystem, to avoid delaying regular I/O due
 * to WAL I/O. This can be done with something like "-Dstorage.wal.path=...".
 */
public class OrientDatabase implements IGraphDatabase {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrientDatabase.class);

	private final class OrientConnectionFactory extends BasePooledObjectFactory<ODatabaseDocumentTx> {
		@Override
		public void destroyObject(PooledObject<ODatabaseDocumentTx> pooled) throws Exception {
			final ODatabaseDocumentTx db = pooled.getObject();
			allConns.remove(db);
			db.activateOnCurrentThread();
			db.close();
		}

		@Override
		public void passivateObject(PooledObject<ODatabaseDocumentTx> p) throws Exception {
			super.passivateObject(p);
			dbConn.set(null);
		}

		@Override
		public void activateObject(PooledObject<ODatabaseDocumentTx> p) throws Exception {
			super.activateObject(p);

			final ODatabaseDocumentTx db = p.getObject();
			dbConn.set(db);
			db.activateOnCurrentThread();
		}

		@Override
		public ODatabaseDocumentTx create() throws Exception {
			ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbURL);
			if (exists(db)) {
				db.open("admin", "admin");
			}
			db.declareIntent(currentMode == Mode.NO_TX_MODE ? new OIntentMassiveInsert() : new OIntentMassiveRead());
			allConns.add(db);
			return db;
		}

		@Override
		public PooledObject<ODatabaseDocumentTx> wrap(ODatabaseDocumentTx db) {
			return new DefaultPooledObject<>(db);
		}
	}

	/** Name of the Orient document class for edges. */
	private static final String EDGE_TYPE = "E";

	/** Vertex class for the Hawk index store. */
	private static final String VCLASS = "hawkIndexStore";

	/** Prefix for qualifying all vertex types (edge and vertex types share same namespace). */
	static final String VERTEX_TYPE_PREFIX = "V_";

	/** Name of the metamodel index. */
	static final String METAMODEL_IDX_NAME = "hawkMetamodelIndex";

	/** Name of the file index. */
	static final String FILE_IDX_NAME = "hawkFileIndex";

	private static final String SIZE_THRESHOLD_PROPERTY = "hawk.orient.periodicSaveThreshold";
	private static final int DEFAULT_SIZE_THRESHOLD = 200_000;

	/**
	 * Size threshold for doing a periodic save (needed to avoid hitting disk
	 * with constant writes in non-transactional mode). Higher increases
	 * performance, at the cost of using more memory.
	 */
	private static final int SIZE_THRESHOLD;
	static {
		final String sPropThreshold = System.getProperty(SIZE_THRESHOLD_PROPERTY);
		int threshold = DEFAULT_SIZE_THRESHOLD;
		if (sPropThreshold != null) {
			try {
				threshold = Integer.valueOf(sPropThreshold);
			} catch (NumberFormatException ex) {
				LOGGER.error(String.format(
					"Invalid value for -D%s: '%s' is not an integer",
					SIZE_THRESHOLD_PROPERTY, sPropThreshold));
			}
		}

		SIZE_THRESHOLD = threshold;
	}

	private File storageFolder;
	private File tempFolder;
	private IConsole console;

	private IGraphNodeIndex metamodelIndex;
	private IGraphNodeIndex fileIndex;

	private Mode currentMode;

	private Map<String, OrientNode> dirtyNodes = new HashMap<>(SIZE_THRESHOLD);
	private Map<String, OrientEdge> dirtyEdges = new HashMap<>(SIZE_THRESHOLD);

	// Currently held database connection in this thread (may be released)
	private final ThreadLocal<ODatabaseDocumentTx> dbConn = new ThreadLocal<>();

	// Database connection pool (to limit memory usage by concurrent threads)
	// - semaphore blocks threads until connections are released
	// - the pool is a simple concurrent queue
	// - allConns keeps a list of all the connections available, e.g. for global shutdown/cache invalidation
	// - system property allows for limiting the number of connections per Orient backend (default is processors*2)
	private GenericObjectPool<ODatabaseDocumentTx> pool;
	private final Set<ODatabaseDocumentTx> allConns = Collections.newSetFromMap(
		new ConcurrentHashMap<ODatabaseDocumentTx, Boolean>(Runtime.getRuntime().availableProcessors() * 2, 0.9f, 1));
	private static final String POOL_SIZE_PROPERTY = "hawk.orient.maxConnections";

	protected String dbURL;

	private Set<OrientNodeIndex> postponedIndexes = new HashSet<>();

	private OrientIndexStore indexStore;

	static {
		OGlobalConfiguration.OBJECT_SAVE_ONLY_DIRTY.setValue(true);
		OGlobalConfiguration.SBTREE_MAX_KEY_SIZE.setValue(102_400);

		// Add a Guava-based Orient cache as default unless user specified something else
		@SuppressWarnings("unchecked")
		OConfigurableStatefulFactory<String, ORecordCache> factory =
			(OConfigurableStatefulFactory<String, ORecordCache>) Orient.instance().getLocalRecordCache();
		factory.register(ORecordCacheGuava.class.getName(), ORecordCacheGuava.class);
		if (System.getProperty(OGlobalConfiguration.CACHE_LOCAL_IMPL.getKey()) == null) {
			OGlobalConfiguration.CACHE_LOCAL_IMPL.setValue(ORecordCacheGuava.class.getName());
		}
	}

	@Override
	public String getPath() {
		return storageFolder.getPath();
	}

	@Override
	public void run(File parentfolder, IConsole c) {
		try {
			run("plocal:" + parentfolder.getAbsolutePath(), parentfolder, c);
		} catch (Exception e) {
			c.printerrln(e);
		}
	}

	public void run(String iURL, File parentfolder, IConsole c) throws Exception {
		this.storageFolder = parentfolder;
		this.tempFolder = new File(storageFolder, "temp");
		this.console = c;

		console.println("Starting database " + iURL);
		this.dbURL = iURL;

		pool = new GenericObjectPool<>(new OrientConnectionFactory());
		pool.setMinIdle(0);
		pool.setMaxIdle(2);
		pool.setMaxTotal(getPoolSize());
		pool.setMinEvictableIdleTimeMillis(20_000);
		pool.setBlockWhenExhausted(true);

		metamodelIndex = getOrCreateNodeIndex(METAMODEL_IDX_NAME);
		fileIndex = getOrCreateNodeIndex(FILE_IDX_NAME);

		// By default, we're on transactional mode
		exitBatchMode();
	}

	protected int getPoolSize() {
		int poolSize = Runtime.getRuntime().availableProcessors() * 2;
		String sPoolSize = System.getProperty(POOL_SIZE_PROPERTY);
		if (sPoolSize != null) {
			try {
				poolSize = Math.max(1, Integer.valueOf(sPoolSize));
			} catch (NumberFormatException ex) {
				LOGGER.error("{} has invalid value '{}': falling back to {}", POOL_SIZE_PROPERTY, sPoolSize, poolSize);
			}
		}
		return poolSize;
	}

	@Override
	public void shutdown() throws Exception {
		shutdown(false);
	}

	@Override
	public void delete() throws Exception {
		shutdown(true);
	}

	private void shutdown(boolean delete) throws Exception {
		if (pool == null || pool.isClosed()) {
			return;
		}

		ODatabaseDocumentTx db = getGraphNoCreate();
		if (delete) {
			discardDirty();
		} else {
			saveDirty();
		}

		synchronized (allConns) {
			// Close all other connections
			for (ODatabaseDocumentTx conn : allConns) {
				if (conn != db) {
					pool.invalidateObject(conn);
				}
			}
			dbConn.get().activateOnCurrentThread();

			/*
			 * We want to completely close the database (e.g. so we can delete
			 * the directory later from the Hawk UI).
			 */
			final OStorage storage = db.getStorage();
			if (delete) {
				db.drop();
			} else {
				db.close();
			}
			storage.close(true, false);
			Orient.instance().unregisterStorage(storage);
			pool.invalidateObject(db);

			if (delete && storageFolder != null) {
				try {
					deleteRecursively(storageFolder);
				} catch (IOException e) {
					console.printerrln(e);
				}
			}

			pool.clear();
		}

		metamodelIndex = fileIndex = null;
		storageFolder = tempFolder = null;
	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {
		return new OrientNodeIndex(name, this);
	}

	@Override
	public IGraphNodeIndex getMetamodelIndex() {
		return metamodelIndex;
	}

	@Override
	public IGraphNodeIndex getFileIndex() {
		return fileIndex;
	}

	@Override
	public OrientTransaction beginTransaction() {
		if (!getGraph().getTransaction().isActive()) {
			exitBatchMode();
		}
		return new OrientTransaction(this);
	}

	@Override
	public boolean isTransactional() {
		return true;
	}

	@Override
	public void enterBatchMode() {
		ODatabaseDocumentTx db = getGraph();
		if (db.getTransaction().isActive()) {
			saveDirty();
			db.commit();
		}

		currentMode = Mode.NO_TX_MODE;
	}

	/**
	 * Closes the connection currently open to the DB in the current thread.
	 */
	protected void releaseConnection() {
		final ODatabaseDocumentTx db = dbConn.get();
		if (db != null) {
			if (db.getTransaction().isActive()) {
				db.getTransaction().close();
			}
			pool.returnObject(db);
		}
	}

	public void saveDirty() {
		final boolean hadDirty = !dirtyNodes.isEmpty() || !dirtyEdges.isEmpty();
		for (Iterator<OrientNode> itNode = dirtyNodes.values().iterator(); itNode.hasNext();) {
			OrientNode on = itNode.next();
			on.save();
			itNode.remove();
		}
		for (Iterator<OrientEdge> itEdge = dirtyEdges.values().iterator(); itEdge.hasNext();) {
			OrientEdge oe = itEdge.next();
			oe.save();
			itEdge.remove();
		}

		if (hadDirty) {
			// We changed the database permanently - invalidate all the other caches
			for (ODatabaseDocumentTx conn : allConns) {
				conn.activateOnCurrentThread();
				conn.getLocalCache().invalidate();
			}
			ODatabaseDocumentTx conn = dbConn.get();
			if (conn != null) {
				conn.activateOnCurrentThread();
			}
		}
	}

	@Override
	public void exitBatchMode() {
		final ODatabaseDocumentTx db = getGraph();
		if (!db.getTransaction().isActive()) {
			saveDirty();
			getGraph().commit();
		}
		currentMode = Mode.TX_MODE;
	}

	@Override
	public OrientNodeIterable allNodes(String label) {
		final String vertexTypeName = getVertexTypeName(label);
		return new OrientNodeIterable(
				new OrientClusterDocumentIterable(vertexTypeName, this), this);
	}

	@Override
	public OrientNode createNode(Map<String, Object> properties, String label) {
		final String vertexTypeName = getVertexTypeName(label);

		ensureClassExists(vertexTypeName);
		ODocument newDoc = new ODocument(vertexTypeName);
		if (properties != null) {
			OrientNode.setProperties(newDoc, properties);
		}
		newDoc.save(vertexTypeName);

		if (newDoc.getIdentity().isPersistent()) {
			return new OrientNode(newDoc.getIdentity(), this);
		} else {
			return new OrientNode(newDoc, this);
		}
	}

	private void ensureClassExists(final String className) {
		ODatabaseDocumentTx db = getGraph();
		final OSchemaProxy schema = db.getMetadata().getSchema();
		if (!schema.existsClass(className)) {
			final boolean wasInTX = db.getTransaction().isActive();
			if (wasInTX) {
				LOGGER.warn("Warning: premature commit needed to create class {}", className);
				saveDirty();
				getGraph().commit();
			}

			/*
			 * In order to be treated by OrientDB's query layer as a vertex (e.g. the "Graph" viewer
			 * in the OrientDB Studio), the vertex classes need to have V as a superclass and refer
			 * to outgoing edges as out_X and incoming edges as in_X. All edge documents must then
			 * belong to E or a subclass of and use out and in for the source and target of the edge.
			 */
			final OClass oClass = schema.createClass(className);
			if (className.startsWith(VERTEX_TYPE_PREFIX)) {
				OClass baseVertexClass = schema.getClass("V");
				if (baseVertexClass == null) {
					baseVertexClass = schema.createClass("V");
					baseVertexClass.setOverSize(2);
				}
				oClass.addSuperClass(baseVertexClass);
				OrientNode.setupDocumentClass(oClass);
			} else if (EDGE_TYPE.equals(className)) {
				OrientEdge.setupDocumentClass(oClass);
			}

			if (wasInTX) {
				db.begin();
			}
		}
	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		Map<String, Object> props = Collections.emptyMap();
		return createRelationship(start, end, type, props);
	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props) {
		final OrientNode oStart = (OrientNode)start;
		final OrientNode oEnd = (OrientNode)end;
		final String edgeTypeName = getEdgeTypeName(type);

		ensureClassExists(edgeTypeName);

		IGraphEdge newEdge = OrientEdge.create(this, oStart, oEnd, type, edgeTypeName, props);
		dirtyNodes.put(oStart.getId().toString(), oStart);
		dirtyNodes.put(oEnd.getId().toString(), oEnd);
		saveIfBig();

		return newEdge;
	}

	private void saveIfBig() {
		final int totalSize = dirtyNodes.size() + dirtyEdges.size();
		if (totalSize > SIZE_THRESHOLD) {
			saveDirty();
		}
	}

	private String getVertexTypeName(String label) {
		return OrientNameCleaner.escapeClass(VERTEX_TYPE_PREFIX + label);
	}

	private String getEdgeTypeName(String label) {
		// We don't need edge classes, as there is no allEdges(...) method:
		// this reduces the amount of times we may need to switch back to
		// batch mode if we need to add a new edge type (very common during
		// proxy resolving).

		return EDGE_TYPE;
	}

	@Override
	public ODatabaseDocumentTx getGraph() {
		ODatabaseDocumentTx db = getGraphNoCreate();
		if (!exists(db)) {
			db.create();

			// Enable lightweight edges by default
			db.command(new OCommandSQL("ALTER DATABASE CUSTOM useLightweightEdges = true")).execute();
		}
		return db;
	}

	/**
	 * Returns <code>true</code> if the database exists, <code>false</code>
	 * otherwise.
	 */
	protected boolean exists(ODatabaseDocumentTx db) {
		return db.exists();
	}

	protected ODatabaseDocumentTx getGraphNoCreate() {
		final ODatabaseDocumentTx conn = dbConn.get();
		if (conn != null) {
			conn.activateOnCurrentThread();
			if (!conn.isClosed()) {
				return conn;
			}
		}

		try {
			return pool.borrowObject();
		} catch (Exception e) {
			LOGGER.error("Error opening connection to Orient", e);
			return null;
		}
	}

	@Override
	public OrientNode getNodeById(Object id) {
		if (id instanceof String) {
			id = new ORecordId(id.toString());
		}

		String sID = id instanceof ODocument ? ((ODocument)id).getIdentity().toString() : id.toString();
		OrientNode result = dirtyNodes.get(sID);

		if (result == null) {
			if (id instanceof ODocument) {
				/*
				 * Do not reuse existing documents - for instance, if someone
				 * fetches a node and manipulates it, and then someone retrieves
				 * the same node through some other path (e.g. following an
				 * edge), we should always work with the most recent version to
				 * avoid MVCC exceptions.
				 */
				result = new OrientNode(((ODocument)id).getIdentity(), this);
			} else {
				result = new OrientNode((ORID)id, this);
			}
		}

		return result;
	}

	public OrientEdge getEdgeById(Object id) {
		if (id instanceof String) {
			id = new ORecordId(id.toString());
		}

		String sID = id instanceof ODocument ? ((ODocument)id).getIdentity().toString() : id.toString();
		OrientEdge dirtyEdge = dirtyEdges.get(sID);
		if (dirtyEdge != null) {
			return dirtyEdge;
		} else if (id instanceof ODocument) {
			return new OrientEdge((ODocument)id, this);
		} else {
			return new OrientEdge((ORID)id, this);
		}
	}

	@Override
	public boolean nodeIndexExists(String name) {
		return getIndexStore().getNodeIndexNames().contains(name);
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public String getHumanReadableName() {
		return "OrientDB";
	}

	@Override
	public String getTempDir() {
		return tempFolder.getAbsolutePath();
	}

	@Override
	public File logFull() throws Exception {
		File logFolder = new File(storageFolder, "logs");
		logFolder.mkdir();
		// TODO print something here
		return logFolder;
	}

	@Override
	public Mode currentMode() {
		return currentMode;
	}

	@Override
	public Set<String> getNodeIndexNames() {
		return new HashSet<String>(getIndexStore().getNodeIndexNames());
	}

	@Override
	public Set<String> getEdgeIndexNames() {
		return new HashSet<String>(getIndexStore().getEdgeIndexNames());
	}

	@Override
	public Set<String> getKnownMMUris() {
		final Set<String> mmURIs = new HashSet<>();
		for (IGraphNode node : getMetamodelIndex().query("*", "*")) {
			String mmURI = (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
			mmURIs.add(mmURI);
		}
		return mmURIs;
	}

	public OrientIndexStore getIndexStore() {
		if (indexStore != null) {
			return indexStore;
		}

		ODocument idIndexStore = getGraph().getDictionary().get(VCLASS);
		OrientNode vIndexStore;
		if (idIndexStore == null) {
			final HashMap<String, Object> idxStoreProps = new HashMap<>();
			vIndexStore = createNode(idxStoreProps, VCLASS);
			getGraph().getDictionary().put(VCLASS, vIndexStore.getDocument());
			indexStore = new OrientIndexStore(vIndexStore);
		} else {
			indexStore = new OrientIndexStore(new OrientNode(idIndexStore.getIdentity(), this));
		}
		return indexStore;
	}

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists()) return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	@SuppressWarnings("unchecked")
	public <T> T getElementById(ORID id, Class<T> klass) {
		if (klass == OrientEdge.class || klass == IGraphEdge.class) {
			return (T) getEdgeById(id);
		} else if (klass == OrientNode.class || klass == IGraphNode.class) {
			return (T) getNodeById(id);
		} else {
			return null;
		}
	}

	public void markNodeAsDirty(OrientNode orientNode) {
		final ORID id = orientNode.getId();
		dirtyNodes.put(id.toString(), orientNode);
		deleteFromAllCaches(id);
		saveIfBig();
	}

	public void unmarkNodeAsDirty(OrientNode orientNode) {
		dirtyNodes.remove(orientNode.getId() + "");
	}

	public void markEdgeAsDirty(OrientEdge orientEdge) {
		final ORID id = orientEdge.getId();
		dirtyEdges.put(id.toString(), orientEdge);
		deleteFromAllCaches(id);
		saveIfBig();
	}

	public void unmarkEdgeAsDirty(OrientEdge orientEdge) {
		dirtyEdges.remove(orientEdge.getId() + "");
	}

	public void discardDirty() {
		dirtyNodes.clear();
		dirtyEdges.clear();
	}

	protected void deleteFromAllCaches(final ORID id) {
		for (ODatabaseDocumentTx conn : allConns) {
			conn.activateOnCurrentThread();
			conn.getLocalCache().deleteRecord(id);
		}
	}

	public IConsole getConsole() {
		return console;
	}

	public void addPostponedIndex(OrientNodeIndex orientNodeIndex) {
		postponedIndexes.add(orientNodeIndex);
	}

	public void clearPostponedIndexes() {
		for (OrientNodeIndex idx : postponedIndexes) {
			idx.getPostponedIndexAdditions().clear();
		}
		postponedIndexes.clear();
	}

	public void processPostponedIndexes() {
		if (postponedIndexes.isEmpty()) {
			return;
		}

		for (OrientNodeIndex idx : postponedIndexes) {
			for (PostponedIndexAdd addition : idx.getPostponedIndexAdditions()) {
				addition.getIndex().put(addition.getKey(), addition.getValue());
			}
			idx.getPostponedIndexAdditions().clear();
		}
	}
}
