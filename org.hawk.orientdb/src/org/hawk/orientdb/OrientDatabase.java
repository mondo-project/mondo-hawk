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
import java.util.ArrayList;
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

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * OrientDB backend for Hawk. Most things work, but it has two limitations:
 * numeric ranges are not supported by the OrientDB+Lucene integration, and edge
 * indexes do not support Lucene queries at the moment (it's a simple SBTree
 * index for now).
 *
 * NOTE: it is recommended to set the {@link OGlobalConfiguration#WAL_LOCATION}
 * system property to a different filesystem, to avoid delaying regular I/O due
 * to WAL I/O. This can be done with something like "-Dstorage.wal.path=...".
 */
public class OrientDatabase implements IGraphDatabase {

	private static final class ORIDMapper implements Iterable<ORID> {
		private final Iterable<ODocument> nodeDocs;

		private ORIDMapper(Iterable<ODocument> nodeDocs) {
			this.nodeDocs = nodeDocs;
		}

		@Override
		public Iterator<ORID> iterator() {
			final Iterator<ODocument> it = nodeDocs.iterator();
			return new Iterator<ORID>(){

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public ORID next() {
					return it.next().getIdentity();
				}

				@Override
				public void remove() {
					it.remove();
				}
			};
		}
	}

	/** Vertex class for the Hawk index store. */
	private static final String VCLASS = "hawkIndexStore";

	/** Prefix for qualifying all edge types (edge and vertex types share same namespace). */
	static final String EDGE_TYPE_PREFIX = "E_";

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
	private ODatabaseDocumentTx db, dbTx;

	private Map<String, OrientNode> dirtyNodes = new HashMap<>(100_000);
	private Map<String, OrientEdge> dirtyEdges = new HashMap<>(100_000);

	private OrientIndexStore indexStore;

	private String dbURL;

	public OrientDatabase() {
		// nothing to do
	}

	@Override
	public String getPath() {
		return storageFolder.getPath();
	}

	@Override
	public void run(File parentfolder, IConsole c) {
		run("plocal:" + parentfolder.getAbsolutePath(), parentfolder, c);
	}

	public void run(String iURL, File parentfolder, IConsole c) {
		this.storageFolder = parentfolder;
		this.tempFolder = new File(storageFolder, "temp");
		this.console = c;

		OGlobalConfiguration.WAL_CACHE_SIZE.setValue(10000);
		OGlobalConfiguration.WAL_SYNC_ON_PAGE_FLUSH.setValue(false);
		OGlobalConfiguration.OBJECT_SAVE_ONLY_DIRTY.setValue(true);

		console.println("Starting database " + iURL);
		this.dbURL = iURL;
		this.db = new ODatabaseDocumentTx(iURL);
		if (db.exists()) {
			db.open("admin", "admin");
		} else {
			db.create();
		}

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
		if (!delete) {
			saveDirty();
			if (!getGraph().isClosed()) {
				/*
				 * We want to completely close the database (e.g. so we can
				 * delete the directory later from the Hawk UI).
				 */
				final OStorage storage = getGraph().getStorage();
				getGraph().close();
				storage.close(true, false);
			}
		} else {
			discardDirty();
			if (!getGraph().isClosed()) {
				getGraph().drop();
			}
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
		getGraph(); // make sure the thread local to the OrientDB instance is set
		if (dbTx == null) {
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
		if (dbTx != null) {
			saveDirty();
			dbTx.commit();
			dbTx = null;
			reopenWithWALSetTo(false);
		}
		ODatabaseRecordThreadLocal.INSTANCE.set(db);
		db.declareIntent(new OIntentMassiveInsert());
	}

	private void reopenWithWALSetTo(final boolean useWAL) {
		db.getStorage().close(true, false);
		db.close();
		OGlobalConfiguration.USE_WAL.setValue(useWAL);
		db = new ODatabaseDocumentTx(dbURL);
		db.open("admin", "admin");
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
		if (dbTx == null) {
			saveDirty();
			reopenWithWALSetTo(true);
			dbTx = db.begin();
		}
		ODatabaseRecordThreadLocal.INSTANCE.set(dbTx);
	}

	@Override
	public OrientNodeIterable allNodes(String label) {
		final String vertexTypeName = getVertexTypeName(label);
		return allNodes(vertexTypeName, dbTx != null ? dbTx : db);
	}

	private OrientNodeIterable allNodes(final String vertexTypeName, final ODatabaseDocumentTx dbDoc) {
		final int clusterId = dbDoc.getClusterIdByName(vertexTypeName);
		if (clusterId == -1) {
			return new OrientNodeIterable(new ArrayList<ORID>(), this);
		}

		final Iterable<ODocument> nodeDocs = dbDoc.browseCluster(vertexTypeName);
		final Iterable<ORID> nodeIds = new ORIDMapper(nodeDocs);
		return new OrientNodeIterable(nodeIds, this);
	}

	@Override
	public OrientNode createNode(Map<String, Object> properties, String label) {
		final String vertexTypeName = getVertexTypeName(label);

		if (!db.getMetadata().getSchema().existsClass(vertexTypeName) && dbTx != null) {
			enterBatchMode();
			db.getMetadata().getSchema().createClass(vertexTypeName);
			exitBatchMode();
		}
		ODocument newDoc = new ODocument(vertexTypeName);
		if (properties != null) {
			OrientNode.setProperties(newDoc, properties);
		}
		newDoc.save();

		return new OrientNode(newDoc.getIdentity(), this);
	}

	@Override
	public OrientEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		final OrientNode oStart = (OrientNode)start;
		final OrientNode oEnd = (OrientNode)end;
		final String edgeTypeName = getEdgeTypeName(type);

		if (!db.getMetadata().getSchema().existsClass(edgeTypeName) && dbTx != null) {
			enterBatchMode();
			db.getMetadata().getSchema().createClass(edgeTypeName);
			exitBatchMode();
		}

		OrientEdge newEdge = OrientEdge.create(this, oStart, oEnd, type, edgeTypeName);
		dirtyNodes.put(oStart.getId().toString(), oStart);
		dirtyNodes.put(oEnd.getId().toString(), oEnd);
		saveIfBig();

		return new OrientEdge(newEdge.getId(), this);
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
		if (e != null) {
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
		return OrientNameCleaner.escapeClass(EDGE_TYPE_PREFIX + label);
	}

	@Override
	public ODatabaseDocumentTx getGraph() {
		if (dbTx != null) {
			ODatabaseRecordThreadLocal.INSTANCE.set(dbTx);
			return dbTx;
		} else {
			ODatabaseRecordThreadLocal.INSTANCE.set(db);
			return db;
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
		return dbTx == null ? Mode.NO_TX_MODE : Mode.TX_MODE;
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

	public void deleteEdge(OrientEdge orientEdge) {
		final OrientNode startNode = orientEdge.getStartNode();
		final OrientNode endNode = orientEdge.getEndNode();
		startNode.removeOutgoing(orientEdge);
		endNode.removeIncoming(orientEdge);

		dirtyNodes.put(startNode.getId().toString(), startNode);
		dirtyNodes.put(endNode.getId().toString(), endNode);
		getGraph().delete(orientEdge.getId());
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

	public void deleteNode(ORID id) {
		getGraph().delete(id);
		dirtyNodes.remove(id.toString());
	}
}
