/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.neo4j_v2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.FileOperations;
import org.hawk.neo4j_v2.util.Neo4JBatchUtil;
import org.hawk.neo4j_v2.util.Neo4JEdge;
import org.hawk.neo4j_v2.util.Neo4JEdgeIndex;
import org.hawk.neo4j_v2.util.Neo4JIterable;
import org.hawk.neo4j_v2.util.Neo4JNode;
import org.hawk.neo4j_v2.util.Neo4JNodeIndex;
import org.hawk.neo4j_v2.util.Neo4JTransaction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

public class Neo4JDatabase implements IGraphDatabase {

	private String loc;
	private String tempdir;

	private GraphDatabaseService graph;
	private IndexManager indexer;

	private BatchInserter batch;
	private BatchInserterIndexProvider batchindexer;

	private IGraphNodeIndex fileindex;
	private IGraphNodeIndex metamodelindex;

	// private IAbstractConsole console;

	public Neo4JDatabase() {
	}

	public void run(File location, IAbstractConsole c) {

		// console = c;

		if (location == null) {
			File runtimeDir = new File("runtime_data");
			runtimeDir.mkdir();
			loc = new File("runtime_data/.metadata").getAbsolutePath()
					.replaceAll("\\\\", "/");
		} else
			loc = location.getAbsolutePath();

		tempdir = loc + "/temp";

		loc += "/" + databaseName;

		// init it
		graph = Neo4JBatchUtil.createGraphService(loc);
		indexer = graph.index();

		try (IGraphTransaction t = beginTransaction()) {

			metamodelindex = new Neo4JNodeIndex(metamodelIndexName, this);
			fileindex = new Neo4JNodeIndex(fileIndexName, this);

			t.success();
		} catch (Exception e) {
			System.err.println("error initialising neo4j database: ");
			e.printStackTrace();
		}
	}

	@Override
	public void enterBatchMode() {

		if (graph != null)
			graph.shutdown();
		graph = null;
		indexer = null;

		if (batch == null)
			batch = Neo4JBatchUtil.getGraph(loc);
		if (batchindexer == null)
			batchindexer = new LuceneBatchInserterIndexProvider(batch);

		metamodelindex = new Neo4JNodeIndex(metamodelIndexName, this);
		fileindex = new Neo4JNodeIndex(fileIndexName, this);

	}

	@Override
	public void exitBatchMode() {

		try {
			batchindexer.shutdown();
		} catch (Exception e) {
		}
		try {
			batch.shutdown();
		} catch (Exception e) {
		}

		batch = null;
		batchindexer = null;

		//

		if (graph == null) {

			graph = Neo4JBatchUtil.createGraphService(loc);
			indexer = graph.index();

			try (IGraphTransaction t = beginTransaction()) {

				metamodelindex = new Neo4JNodeIndex(metamodelIndexName, this);
				fileindex = new Neo4JNodeIndex(fileIndexName, this);

				t.success();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getPath() {
		return loc;
	}

	@Override
	public void shutdown() throws Exception {
		try {
			exitBatchMode();
			graph.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void delete() throws Exception {
		// unregisterPackages();
		shutdown();
		System.gc();

		final boolean deleted = FileOperations.deleteFiles(
				new File(getPath()).getParentFile(), true);
		System.err.println("deleted store(" + databaseName + "): " + deleted);

	}

	// private void unregisterPackages() throws Exception {
	//
	// try (GraphTransaction t = beginTransaction()) {
	//
	// HawkIterable<GraphNode> it = metamodelindex.query("id", "*");
	//
	// for (GraphNode n : it) {
	//
	// unregister(n.getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString());
	//
	// }
	// t.success();
	// }
	// }

	// private void unregister(String property) {
	//
	// ix.getremovePackage(property);
	//
	// }

	@Override
	public Set<String> getNodeIndexNames() {

		Set<String> ret = new HashSet<>();

		for (String s : indexer.nodeIndexNames()) {
			for (Map.Entry<String, String> entry : FILEPATHENCODING.entrySet())
				s = s.replace(entry.getValue(), entry.getKey());
			ret.add(s);
		}

		return ret;

	}

	@Override
	public Set<String> getEdgeIndexNames() {
		return new HashSet<String>(Arrays.asList(indexer
				.relationshipIndexNames()));
	}

	private final static Map<String, String> FILEPATHENCODING = new HashMap<String, String>();

	static {
		FILEPATHENCODING.put("\\", "$hslash&");
		FILEPATHENCODING.put("/", "$hfslash&");
		FILEPATHENCODING.put(":", "$hcolon&");
		FILEPATHENCODING.put("*", "$hstar&");
		FILEPATHENCODING.put("?", "$hqmark&");
		FILEPATHENCODING.put("\"", "$hquote&");
		FILEPATHENCODING.put("<", "$hless&");
		FILEPATHENCODING.put(">", "$hmore&");
		FILEPATHENCODING.put("|", "$hor&");
	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {

		name = encodeIndexName(name);

		return new Neo4JNodeIndex(name, this);

	}

	private String encodeIndexName(String name) {
		for (Map.Entry<String, String> entry : FILEPATHENCODING.entrySet()) {
			name = name.replace(entry.getKey(), entry.getValue());
		}
		return name;
	}

	@Override
	public boolean nodeIndexExists(String name) {

		boolean found = false;

		if (graph != null) {

			found = indexer.existsForNodes(encodeIndexName(name));

		} else {

			// no way to find out if exists return false

		}

		return found;

	}

	@Override
	public boolean edgeIndexExists(String name) {

		boolean found = false;

		if (graph != null) {

			found = indexer.existsForRelationships(name);

		} else {

			// no way to find out if exists return false

		}

		return found;

	}

	@Override
	public IGraphEdgeIndex getOrCreateEdgeIndex(String name) {
		return new Neo4JEdgeIndex(name, this);
	}

	@Override
	public IGraphNodeIndex getMetamodelIndex() {
		return metamodelindex;
	}

	@Override
	public IGraphNodeIndex getFileIndex() {
		return fileindex;
	}

	@Override
	public boolean isTransactional() {
		return true;
	}

	// @Override
	// public boolean currentTransactionalState() {
	// return graph != null;
	// }

	@Override
	public IGraphTransaction beginTransaction() throws Exception {

		// System.err.println(graph);

		if (graph == null) {

			System.err
					.println("cant make transactions outside transactional mode, entering it now");
			exitBatchMode();

		}
		return new Neo4JTransaction(this);
	}

	public GraphDatabaseService getGraph() {
		return graph;
	}

	@Override
	public IGraphNode createNode(Map<String, Object> map, String type) {

		if (graph != null) {
			Node n = graph.createNode(Neo4JBatchUtil.createLabel(type));
			for (String s : map.keySet())
				n.setProperty(s, map.get(s));
			return new Neo4JNode(n, this);

		} else {
			long l = batch.createNode(map, Neo4JBatchUtil.createLabel(type));
			return new Neo4JNode(l, this);
		}

	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end,
			String t) {

		RelationshipType type = DynamicRelationshipType.withName(t);

		if (graph != null) {

			Relationship r = graph.getNodeById((long) start.getId())
					.createRelationshipTo(
							graph.getNodeById((long) end.getId()), type);

			return new Neo4JEdge(r, this);

		} else {

			long r = batch.createRelationship((long) start.getId(),
					(long) end.getId(), type, null);

			return new Neo4JEdge(batch.getRelationshipById(r), this);
		}
	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end,
			String t, Map<String, Object> props) {

		RelationshipType type = DynamicRelationshipType.withName(t);

		final long startId = (long) start.getId();
		final long endId = (long) end.getId();
		if (graph != null) {
			final Node startNode = graph.getNodeById(startId);
			final Node endNode = graph.getNodeById(endId);
			Relationship r = startNode.createRelationshipTo(endNode, type);
			for (String s : props.keySet()) {
				r.setProperty(s, props.get(s));
			}

			return new Neo4JEdge(r, this);
		} else {
			final long r = batch
					.createRelationship(startId, endId, type, props);
			return new Neo4JEdge(batch.getRelationshipById(r), this);
		}

	}

	public IndexManager getIndexer() {
		return indexer;
	}

	public BatchInserter getBatch() {
		return batch;
	}

	public BatchInserterIndexProvider getBatchIndexer() {
		return batchindexer;
	}

	@Override
	public Iterable<IGraphNode> allNodes(String label) {

		if (graph != null) {

			GlobalGraphOperations g = GlobalGraphOperations.at(graph);

			return (label == null) ? (new Neo4JIterable<IGraphNode>(
					g.getAllNodes(), this)) : (new Neo4JIterable<IGraphNode>(
					g.getAllNodesWithLabel(Neo4JBatchUtil.createLabel(label)),
					this));

		} else {
			System.err
					.println("allNodes called in a batch isert mode, please exit batch insert first, returning null");
			return null;
		}
	}

	@Override
	public IGraphNode getNodeById(Object id) {

		Long numericid = -1L;

		if (id instanceof String)
			numericid = Long.parseLong((String) id);
		else if (id instanceof Long)
			numericid = (Long) id;
		else if (id instanceof Integer)
			numericid = ((Integer) id).longValue();

		if (numericid != -1) {
			try {
				Node n = graph.getNodeById(numericid);
				return new Neo4JNode(n, this);
			} catch (Exception e) {
				return null;
			}
		} else
			return null;

	}

	// @Override
	// public Map<?, ?> getConfig() {
	//
	// return ((EmbeddedGraphDatabase) graph).getConfig().getParams();
	// }

	@Override
	public String getType() {
		return "org.hawk.neo4j_v2.Neo4JDatabase";
	}

	@Override
	public String getHumanReadableName() {
		return "Neo4J (Version 2) Graph Database";
	}

	@Override
	public String getTempDir() {

		return tempdir;

	}

	public String currentMode() {
		if (graph != null && batch == null)
			return transactional;
		else if (graph == null && batch != null)
			return nonTransactional;
		else
			return "WARNING_UNKNOWN_MODE";
	}

	@Override
	public File logFull() throws Exception {

		File logFolder = new File(Activator.getInstance().getStateLocation()
				.toString()
				+ "/logs");
		logFolder.mkdir();

		File log = new File(logFolder + "/log-" + "name" + "-0.txt");
		int count = 0;
		while (log.exists()) {
			count++;
			log = new File(logFolder + "/log-" + "name" + "-" + count + ".txt");
		}

		BufferedWriter w = new BufferedWriter(new FileWriter(log));

		String str = "";

		try (IGraphTransaction tx = beginTransaction()) {
			// operations on the graph
			// ...

			for (IGraphNode n : allNodes(null)) {

				str = n + " :: ";

				for (String s : n.getPropertyKeys()) {

					str = str + "[" + s + " | " + n.getProperty(s) + "]";

				}

				str = str + " ::: ";

				for (IGraphEdge r : n.getOutgoing()) {

					str = str
							+ "["
							+ r.getType()
							+ " --> "
							+ r.getEndNode()
							+ "("
							+ r.getEndNode().getProperty(
									IModelIndexer.IDENTIFIER_PROPERTY) + ")"
							+ "]";

				}
				w.append(str + "\r\n");
			}

			tx.success();
		}

		w.flush();
		w.close();

		return logFolder;
	}

	@Override
	public Set<String> getKnownMMUris() {

		Set<String> ret = new HashSet<>();

		try (IGraphTransaction t = beginTransaction()) {

			IGraphIterable<IGraphNode> mmnodes = metamodelindex.query("*", "*");

			for (IGraphNode n : mmnodes)
				ret.add(n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}
	
	// @Override
	// public Set<IGraphNode> retainExisting(Set<IGraphNode> nodes) {
	// for (Iterator<IGraphNode> it = nodes.iterator(); it.hasNext();) {
	// Node n;
	// if ((n = graph.getNodeById((long) it.next().getId())) == null)
	// nodes.remove(n);
	// }
	// return nodes;
	// }
}
