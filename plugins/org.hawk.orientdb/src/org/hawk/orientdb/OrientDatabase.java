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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.indexes.OrientEdgeIndex;
import org.hawk.orientdb.indexes.OrientNodeIndex;
import org.hawk.orientdb.indexes.OrientNodeIndex.PostponedIndexAdd;
import org.hawk.orientdb.util.OrientClusterDocumentIterable;
import org.hawk.orientdb.util.OrientNameCleaner;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy;
import com.orientechnologies.orient.core.record.impl.ODocument;
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

	/** Vertex class for the Hawk index store. */
	private static final String VCLASS = "hawkIndexStore";

	/** Prefix for qualifying all vertex types (edge and vertex types share same namespace). */
	static final String VERTEX_TYPE_PREFIX = "V_";

	/** Name of the metamodel index. */
	static final String METAMODEL_IDX_NAME = "hawkMetamodelIndex";

	/** Name of the file index. */
	static final String FILE_IDX_NAME = "hawkFileIndex";

	/** Size threshold for doing a periodic save (needed for huge models). */
	private static final int SIZE_THRESHOLD = 200_000;

	private File storageFolder;
	private File tempFolder;
	private IConsole console;

	private IGraphNodeIndex metamodelIndex;
	private IGraphNodeIndex fileIndex;

	private Mode currentMode;

	private Map<String, OrientNode> dirtyNodes = new HashMap<>(100_000);
	private Map<String, OrientEdge> dirtyEdges = new HashMap<>(100_000);

	private OPartitionedDatabasePool dbPool;
	private OrientIndexStore indexStore;

	protected String dbURL;

	private Set<OrientNodeIndex> postponedIndexes = new HashSet<>();

	public OrientDatabase() {
		// nothing to do
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

		OGlobalConfiguration.WAL_CACHE_SIZE.setValue(10000);
		OGlobalConfiguration.OBJECT_SAVE_ONLY_DIRTY.setValue(true);
		OGlobalConfiguration.SBTREE_MAX_KEY_SIZE.setValue(102_400);

		console.println("Starting database " + iURL);
		this.dbURL = iURL;
		dbPool = new OPartitionedDatabasePool(dbURL, "admin", "admin");

		metamodelIndex = getOrCreateNodeIndex(METAMODEL_IDX_NAME);
		fileIndex = getOrCreateNodeIndex(FILE_IDX_NAME);

		// By default, we're on transactional mode
		exitBatchMode();
	}

	@Override
	public void shutdown() throws Exception {
		shutdown(false);
	}

	@Override
	public void delete() throws Exception {
		shutdown(true);
	}

	private void shutdown(boolean delete) {
		if (delete) {
			discardDirty();
		} else {
			saveDirty();
		}

		ODatabaseDocumentTx db = getGraphAsIs();
		if (!db.isClosed()) {
			/*
			 * We want to completely close the database (e.g. so we can
			 * delete the directory later from the Hawk UI).
			 */
			final OStorage storage = db.getStorage();
			if (delete) {
				db.drop();
			} else {
				db.close();
			}
			if (dbPool != null) {
				dbPool.close();
			}
			storage.close(true, false);
			Orient.instance().unregisterStorage(storage);
		}

		if (delete && storageFolder != null) {
			try {
				deleteRecursively(storageFolder);
			} catch (IOException e) {
				console.printerrln(e);
			}
		}

		metamodelIndex = fileIndex = null;
		storageFolder = tempFolder = null;
		dbPool = null;
	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {
		return new OrientNodeIndex(name, this);
	}

	@Override
	public IGraphEdgeIndex getOrCreateEdgeIndex(String name) {
		return new OrientEdgeIndex(name, this);
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
		ensureWALSetTo(false);
		db = getGraph();
		db.declareIntent(new OIntentMassiveInsert());
		currentMode = Mode.NO_TX_MODE;
	}

	private void ensureWALSetTo(final boolean useWAL) {
		ODatabaseDocumentTx db = getGraph();
		if (useWAL != OGlobalConfiguration.USE_WAL.getValueAsBoolean()) {
			final OStorage storage = db.getStorage();
			db.close();
			storage.close(true, false);
			OGlobalConfiguration.USE_WAL.setValue(useWAL);
			dbPool = new OPartitionedDatabasePool(dbURL, "admin", "admin");
		}
	}

	public void saveDirty() {
		for (Iterator<OrientNode> itNode = dirtyNodes.values().iterator(); itNode.hasNext(); ) {
			OrientNode on = itNode.next();
			on.save();
			itNode.remove();
		}
		for (Iterator<OrientEdge> itEdge = dirtyEdges.values().iterator(); itEdge.hasNext(); ) {
			OrientEdge oe = itEdge.next();
			oe.save();
			itEdge.remove();
		}
	}

	@Override
	public void exitBatchMode() {
		final ODatabaseDocumentTx db = getGraph();
		if (!db.getTransaction().isActive()) {
			saveDirty();
			db.commit();
			ensureWALSetTo(true); // this reopens the DB, so it *must* go before db.begin()
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

	private void ensureClassExists(final String vertexTypeName) {
		final ODatabaseDocumentTx db = getGraph();
		final OSchemaProxy schema = db.getMetadata().getSchema();
		if (!schema.existsClass(vertexTypeName)) {
			final boolean wasInTX = db.getTransaction().isActive();
			if (wasInTX) {
				console.printerrln("Warning: premature commit needed to create class " + vertexTypeName);
				saveDirty();
				db.commit();
			}

			/*
			 * In order to be treated by OrientDB's query layer as a vertex (e.g. the "Graph" viewer
			 * in the OrientDB Studio), the vertex classes need to have V as a superclass and refer
			 * to outgoing edges as out_X and incoming edges as in_X. All edge documents must then
			 * belong to E or a subclass of and use out and in for the source and target of the edge.
			 */
			final OClass newVertexClass = schema.createClass(vertexTypeName);
			if (vertexTypeName.startsWith(VERTEX_TYPE_PREFIX)) {
				OClass baseVertexClass = schema.getClass("V");
				if (baseVertexClass == null) {
					baseVertexClass = schema.createClass("V");
				}
				newVertexClass.setSuperClass(baseVertexClass);
			}

			if (wasInTX) {
				db.begin();
			}
		}
	}

	@Override
	public OrientEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		final OrientNode oStart = (OrientNode)start;
		final OrientNode oEnd = (OrientNode)end;
		final String edgeTypeName = getEdgeTypeName(type);

		ensureClassExists(edgeTypeName);

		OrientEdge newEdge = OrientEdge.create(this, oStart, oEnd, type, edgeTypeName);
		dirtyNodes.put(oStart.getId().toString(), oStart);
		dirtyNodes.put(oEnd.getId().toString(), oEnd);
		saveIfBig();

		return newEdge;
	}

	private void saveIfBig() {
		final int totalSize = dirtyNodes.size() + dirtyEdges.size();
		if (totalSize > SIZE_THRESHOLD) {
			// TODO: should we still do this in tx mode? OrientDB
			// might keep its own copies for the tx anyway.
			saveDirty();
		}
	}

	@Override
	public OrientEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props) {
		OrientEdge e = createRelationship(start, end, type);
		if (props != null) {
			for (Entry<String, Object> entry : props.entrySet()) {
				e.setProperty(entry.getKey(), entry.getValue());
			}
		}
		dirtyEdges.put(e.getId().toString(), e);
		saveIfBig();

		return e;
	}

	private String getVertexTypeName(String label) {
		return OrientNameCleaner.escapeClass(VERTEX_TYPE_PREFIX + label);
	}

	private String getEdgeTypeName(String label) {
		// We don't need edge classes, as there is no allEdges(...) method:
		// this reduces the amount of times we may need to switch back to
		// batch mode if we need to add a new edge type (very common during
		// proxy resolving).

		return "E";
	}

	@Override
	public ODatabaseDocumentTx getGraph() {
		ODatabaseDocumentTx db = getGraphAsIs();
		if (!db.exists()) {
			db.create();
		}
		return db;
	}

	protected ODatabaseDocumentTx getGraphAsIs() {
		if (dbPool != null) {
			return dbPool.acquire();
		} else {
			// Only needed for quick reconnections after a shutdown
			// (e.g. delete() after a shutdown of Hawk in some tests)
			return new ODatabaseDocumentTx(dbURL);
		}
	}

	@Override
	public OrientNode getNodeById(Object id) {
		if (id instanceof String) {
			id = new ORecordId(id.toString());
		}
		OrientNode dirtyNode = dirtyNodes.get(id.toString());
		if (dirtyNode != null) {
			return dirtyNode;
		}

		return new OrientNode((ORID)id, this);
	}

	public OrientEdge getEdgeById(Object id) {
		if (id instanceof String) {
			id = new ORecordId(id.toString());
		}
		OrientEdge dirtyEdge = dirtyEdges.get(id.toString());
		if (dirtyEdge != null) {
			return dirtyEdge;
		}
		return new OrientEdge((ORID)id, this);
	}

	@Override
	public boolean nodeIndexExists(String name) {
		return getIndexStore().getNodeIndexNames().contains(name);
	}

	@Override
	public boolean edgeIndexExists(String name) {
		return getIndexStore().getEdgeIndexNames().contains(name);
	}

	@Override
	public String getType() {
		return OrientDatabase.class.getCanonicalName();
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
		dirtyNodes.put(orientNode.getId().toString(), orientNode);
		saveIfBig();
	}

	public void unmarkNodeAsDirty(OrientNode orientNode) {
		dirtyNodes.remove(orientNode.getId());
	}

	public void markEdgeAsDirty(OrientEdge orientEdge) {
		dirtyEdges.put(orientEdge.getId().toString(), orientEdge);
		saveIfBig();
	}

	public void unmarkEdgeAsDirty(OrientEdge orientEdge) {
		dirtyEdges.remove(orientEdge.getId());
	}

	public void discardDirty() {
		dirtyNodes.clear();
		dirtyEdges.clear();
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
		postponedIndexes.clear();
		getGraph().commit();
	}
}
