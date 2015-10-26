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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.indexes.OrientEdgeIndex;
import org.hawk.orientdb.indexes.OrientIndexStore;
import org.hawk.orientdb.indexes.OrientNodeIndex;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * OrientDB backend for Hawk. Most things work, but it has two limitations:
 * numeric ranges are not supported by the OrientDB+Lucene integration, and edge
 * indexes do not support Lucene queries at the moment (it's a simple SBTree
 * index for now).
 */
public class OrientDatabase implements IGraphDatabase {

	/** Prefix for qualifying all edge types (edge and vertex types share same namespace). */
	public static final String EDGE_TYPE_PREFIX = "E_";

	/** Prefix for qualifying all vertex types (edge and vertex types share same namespace). */
	public static final String VERTEX_TYPE_PREFIX = "V_";

	static final String NOTX_MODE = "batch";
	static final String TX_MODE = "transactional";

	// Names of the default indexes
	static final String METAMODEL_IDX_NAME = "hawkMetamodelIndex";
	static final String FILE_IDX_NAME = "hawkFileIndex";

	private File storageFolder;
	private File tempFolder;

	private IGraphNodeIndex metamodelIndex;
	private IGraphNodeIndex fileIndex;

	private OrientGraphFactory factory;
	private OrientGraphNoTx batchGraph;
	private OrientGraph txGraph;

	public OrientDatabase() {
		// nothing to do
	}

	@Override
	public String getPath() {
		return storageFolder.getPath();
	}

	@Override
	public void run(File parentfolder, IConsole c) {
		this.storageFolder = parentfolder;
		this.tempFolder = new File(storageFolder, "temp");
		this.factory = new OrientGraphFactory("plocal:" + parentfolder.getAbsolutePath()).setupPool(1, 10);

		txGraph = factory.getTx();
		metamodelIndex = getOrCreateNodeIndex(METAMODEL_IDX_NAME);
		fileIndex = getOrCreateNodeIndex(FILE_IDX_NAME);
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
		if (txGraph != null) {
			txGraph.shutdown();
		}
		if (batchGraph != null) {
			batchGraph.shutdown();
		}
	
		if (factory != null) {
			if (delete) {
				factory.drop();
			} else {
				factory.close();
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
		enterBatchMode();
		Index<Edge> idx = batchGraph.getIndex(name, Edge.class);
		if (idx == null) {
			idx = batchGraph.createIndex(name, Edge.class);
		}
		exitBatchMode();
		return new OrientEdgeIndex(name, idx, this);
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
		if (txGraph == null) {
			System.err
					.println("cant make transactions outside transactional mode, entering it now");
			exitBatchMode();
		}

		return new OrientTransaction(txGraph);
	}

	@Override
	public boolean isTransactional() {
		return true;
	}

	@Override
	public void enterBatchMode() {
		if (txGraph != null) {
			txGraph.shutdown();
			txGraph = null;
		}
		if (batchGraph == null) {
			batchGraph = factory.getNoTx();
		}
	}

	@Override
	public void exitBatchMode() {
		if (batchGraph != null) {
			batchGraph.shutdown();
			batchGraph = null;
		}
		if (txGraph == null) {
			txGraph = factory.getTx();
		}
	}

	@Override
	public OrientNodeIterable allNodes(String label) {
		final String vertexTypeName = VERTEX_TYPE_PREFIX + label;
		if (txGraph != null) {
			if (txGraph.getVertexType(vertexTypeName) == null) {
				return new OrientNodeIterable(new ArrayList<Vertex>(), this);
			}
			return new OrientNodeIterable(txGraph.getVerticesOfClass(vertexTypeName), this);
		} else if (batchGraph != null) {
			if (batchGraph.getVertexType(vertexTypeName) == null) {
				return new OrientNodeIterable(new ArrayList<Vertex>(), this);
			}
			return new OrientNodeIterable(batchGraph.getVerticesOfClass(vertexTypeName), this);
		}
		return null;
	}

	@Override
	public OrientNode createNode(Map<String, Object> properties, String label) {
		final String vertexTypeName = VERTEX_TYPE_PREFIX + label;
		OrientVertex v = null;
		if (txGraph != null) {
			ensureVertexTypeExists(vertexTypeName);
			v = txGraph.addVertex(vertexTypeName, (String)null);
		} else if (batchGraph != null) {
			v = batchGraph.addVertex(vertexTypeName, (String)null);
		}

		if (v != null) {
			for (Entry<String, Object> entry : properties.entrySet()) {
				v.setProperty(entry.getKey(), entry.getValue());
			}
		}

		return new OrientNode(v, this);
	}

	public void ensureVertexTypeExists(final String vertexTypeName) {
		if (getGraph().getVertexType(vertexTypeName) == null) {
			// OrientDB exits the transaction to create new types anyway:
			// this prevents having a warning printed to the console about it
			enterBatchMode();
			batchGraph.createVertexType(vertexTypeName);
			exitBatchMode();
		}
	}

	@Override
	public OrientEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		final OrientNode oStart = (OrientNode)start;
		final OrientNode oEnd = (OrientNode)end;
		final String edgeTypeName = EDGE_TYPE_PREFIX + type;

		if (txGraph != null) {
			if (txGraph.getEdgeType(edgeTypeName) == null) {
				enterBatchMode();
				batchGraph.createEdgeType(edgeTypeName);
				exitBatchMode();
			}
			Edge e = txGraph.addEdge(null, oStart.getVertex(), oEnd.getVertex(), edgeTypeName);
			return new OrientEdge(e, this);
		} else if (batchGraph != null) {
			Edge e = batchGraph.addEdge(null, oStart.getVertex(), oEnd.getVertex(), edgeTypeName);
			return new OrientEdge(e, this);
		}
		return null;
	}

	@Override
	public OrientEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props) {
		OrientEdge e = createRelationship(start, end, type);
		if (e != null) {
			for (Entry<String, Object> entry : props.entrySet()) {
				e.setProperty(entry.getKey(), entry.getValue());
			}
		}

		return e;
	}

	@Override
	public OrientBaseGraph getGraph() {
		if (txGraph != null) {
			return txGraph;
		} else {
			return batchGraph;
		}
	}

	@Override
	public IGraphNode getNodeById(Object id) {
		OrientVertex v = null;
		if (txGraph != null) {
			v = txGraph.getVertex(id);
		} else if (batchGraph != null) {
			v = batchGraph.getVertex(id);
		}

		if (v == null) {
			return null;
		} else {
			return new OrientNode(v, this);
		}
	}

	@Override
	public boolean nodeIndexExists(String name) {
		OrientIndexStore store = OrientIndexStore.getInstance(this);
		return store.getNodeIndexNames().contains(name);
	}

	@Override
	public boolean edgeIndexExists(String name) {
		try (OrientTransaction tx = beginTransaction()) {
			final boolean ret = tx.getOrientGraph().getIndex(name, Vertex.class) != null;
			tx.success();
			return ret;
		}
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
	public String currentMode() {
		return batchGraph != null ? NOTX_MODE : TX_MODE;
	}

	@Override
	public Set<String> getNodeIndexNames() {
		return new HashSet<String>(OrientIndexStore.getInstance(this).getNodeIndexNames());
	}

	@Override
	public Set<String> getEdgeIndexNames() {
		return getIndexNames(Edge.class);
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

	public OrientVertex getVertex(Object id) {
		if (txGraph != null) {
			return txGraph.getVertex(id);
		} else if (batchGraph != null) {
			return batchGraph.getVertex(id);
		}
		return null;
	}

	private Set<String> getIndexNames(final Class<?> klass) {
		try (OrientTransaction tx = beginTransaction()) {
			final Set<String> names = new HashSet<>();
			for (Index<?> idx : tx.getOrientGraph().getIndices()) {
				if (klass == idx.getIndexClass()) {
					names.add(idx.getIndexName());
				}
			}
			tx.success();
			return names;
		}
	}
}
