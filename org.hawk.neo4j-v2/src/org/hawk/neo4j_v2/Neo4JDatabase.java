/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkIterable;
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
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

public class Neo4JDatabase implements IGraphDatabase {

	private String loc;
	private String tempdir;
	private String name;

	private GraphDatabaseService graph;
	private IndexManager indexer;

	private BatchInserter batch;
	private BatchInserterIndexProvider batchindexer;

	private IGraphNodeIndex fileindex;
	private IGraphNodeIndex metamodelindex;

	private IAbstractConsole console;

	public Neo4JDatabase() {
	}

	public void run(String name, File location, IAbstractConsole c) {

		console = c;

		this.name = name;

		if (location == null) {
			File runtimeDir = new File("runtime_data");
			runtimeDir.mkdir();
			loc = new File("runtime_data/.metadata").getAbsolutePath()
					.replaceAll("\\\\", "/");
		} else
			loc = location.getAbsolutePath();

		tempdir = loc + "/temp";

		loc += "/" + name;

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
	public void shutdown(boolean delete) throws Exception {

		// if (delete)
		// unregisterPackages();

		try {
			exitBatchMode();
			graph.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (delete) {
			System.gc();
			delete();
		}
	}

	public void delete() throws Exception {

		// System.err.println(getPath());
		// System.err.println(new File(getPath()).exists());

		// System.err.println(
		System.err.println("deleted store(" + name + "): "
				+ FileOperations.deleteFiles(new File(getPath()), true));

	}

	// private void unregisterPackages() throws Exception {
	//
	// try (GraphTransaction t = beginTransaction()) {
	//
	// HawkIterable<GraphNode> it = metamodelindex.query("id", "*");
	//
	// for (GraphNode n : it) {
	//
	// unregister(n.getProperty("id").toString());
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
		return new HashSet<String>(Arrays.asList(indexer.nodeIndexNames()));
	}

	@Override
	public Set<String> getEdgeIndexNames() {
		return new HashSet<String>(Arrays.asList(indexer
				.relationshipIndexNames()));
	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {
		return new Neo4JNodeIndex(name, this);
	}

	@Override
	public boolean nodeIndexExists(String name) {

		boolean found = false;

		if (graph != null) {

			found = indexer.existsForNodes(name);

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

		if (graph != null) {

			Relationship r = graph.getNodeById((long) start.getId())
					.createRelationshipTo(
							graph.getNodeById((long) end.getId()), type);
			for (String s : props.keySet())
				r.setProperty(s, props.get(s));

			return new Neo4JEdge(r, this);

		} else {

			long r = batch.createRelationship((long) start.getId(),
					(long) end.getId(), type, props);

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

	@Override
	public Map<?, ?> getConfig() {

		return ((EmbeddedGraphDatabase) graph).getConfig().getParams();
	}

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

	@Override
	public Set<VcsCommitItem> compareWithLocalFiles(
			Set<VcsCommitItem> reposItems) {

		Set<VcsCommitItem> changed = new HashSet<VcsCommitItem>();
		changed.addAll(reposItems);

		// System.out.println(currentMode());

		if (graph != null) {

			try (IGraphTransaction tx = beginTransaction()) {
				// operations on the graph
				// ...

				IGraphNodeIndex filedictionary = null;

				filedictionary = getFileIndex();

				if (filedictionary != null
						&& filedictionary.query("id", "*").size() > 0) {
					for (VcsCommitItem r : reposItems) {
						String rev = "-2";
						try {

							// System.err.println(r.getPath());
							// for(GraphNode n:filedictionary.query("id", "*"))
							// System.err.println(n.getProperty("id")+" : "+n.getProperty("revision"));

							IHawkIterable<IGraphNode> ret = filedictionary.get(
									"id", r.getPath());

							IGraphNode n = ret.getSingle();

							rev = (String) n.getProperty("revision");

						} catch (Exception e) {

							// console.printerrln("ERROR in accessing: "
							// + r.getPath()
							// + " in the filedictionary of the store, "
							// // +likely store corrupted, please remove
							// // the store
							// // from Hawk and let it re-create it");
							// + "adding it when needed");
							//
							// System.err.println("filedictionary contains:\n-");
							// for (IGraphNode o : filedictionary.query("id",
							// "*")) {
							// System.err.print(o.getProperty("id")
							// + " || containing: ");
							// int i = 0;
							// for (IGraphEdge n : o
							// .getIncomingWithType("file"))
							// i++;
							// System.err.println(i
							// + " incoming edges type 'file'");
							// }
							// System.err.println("-");

							// e.printStackTrace();
							// System.exit(1);

						}
						if (r.getCommit().getRevision().equals(rev))
							changed.remove(r);

						console.printerrln("comparing revisions of: "
								+ r.getPath() + " | "
								+ r.getCommit().getRevision() + " | " + rev);

					}
				}

				tx.success();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		try {
			// graph.shutdown();
		} catch (Exception e) {
		}

		return changed;

	}

	public String currentMode() {
		if (graph != null && batch == null)
			return "TRANSACTIONAL_MODE";
		else if (graph == null && batch != null)
			return "BATCH_MODE";
		else
			return "WARNING_UNKNOWN_MODE";
	}

	@Override
	public void logFull() throws Exception {

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

					str = str + "[" + r.getType() + " --> " + r.getEndNode()
							+ "(" + r.getEndNode().getProperty("id") + ")"
							+ "]";

				}
				w.append(str + "\r\n");
			}

			tx.success();
		}

		w.flush();
		w.close();

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getKnownMMUris() {

		Set<String> ret = new HashSet<>();

		try (IGraphTransaction t = beginTransaction()) {

			IHawkIterable<IGraphNode> mmnodes = metamodelindex.query("*", "*");

			for (IGraphNode n : mmnodes)
				ret.add(n.getProperty("id").toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}
}
