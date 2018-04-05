/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.neo4j_v2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.FileOperations;
import org.hawk.neo4j_v2.util.Neo4JBatchUtil;
import org.hawk.neo4j_v2.util.Neo4JEdge;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4JDatabase implements IGraphDatabase {

	private static final String FILEIDX_NAME = "FILEINDEX";
	private static final String MMIDX_NAME = "METAMODELINDEX";
	private static final String DB_NAME = "db";

	private static final Logger LOGGER = LoggerFactory.getLogger(Neo4JDatabase.class);

	private String loc;
	private String tempdir;

	private GraphDatabaseService graph;
	private IndexManager indexer;

	private BatchInserter batch;
	private BatchInserterIndexProvider batchindexer;

	private IGraphNodeIndex fileindex;
	private IGraphNodeIndex metamodelindex;

	// private IConsole console;

	public Neo4JDatabase() {
	}

	public void run(File location, IConsole c) {

		// console = c;

		if (location == null) {
			File runtimeDir = new File("runtime_data");
			runtimeDir.mkdir();
			loc = new File("runtime_data/.metadata").getAbsolutePath()
					.replaceAll("\\\\", "/");
		} else
			loc = location.getAbsolutePath();

		tempdir = loc + "/temp";

		loc += "/" + DB_NAME;

		// init it
		graph = Neo4JBatchUtil.createGraphService(loc);
		indexer = graph.index();

		try (IGraphTransaction t = beginTransaction()) {

			metamodelindex = new Neo4JNodeIndex(MMIDX_NAME, this);
			fileindex = new Neo4JNodeIndex(FILEIDX_NAME, this);

			t.success();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
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

		metamodelindex = new Neo4JNodeIndex(MMIDX_NAME, this);
		fileindex = new Neo4JNodeIndex(FILEIDX_NAME, this);

	}

	@Override
	public void exitBatchMode() {

		if (batchindexer != null) {
			try {
				batchindexer.shutdown();
			} catch (Exception e) {
			}
			batchindexer = null;
		}

		if (batch != null) {
			try {
				batch.shutdown();
			} catch (Exception e) {
			}
			batch = null;
		}

		if (graph == null) {

			graph = Neo4JBatchUtil.createGraphService(loc);
			indexer = graph.index();

			try (IGraphTransaction t = beginTransaction()) {

				metamodelindex = new Neo4JNodeIndex(MMIDX_NAME, this);
				fileindex = new Neo4JNodeIndex(FILEIDX_NAME, this);

				t.success();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
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
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public void delete() throws Exception {
		// unregisterPackages();
		shutdown();
		System.gc();

		final boolean deleted = FileOperations.deleteFiles(new File(getPath()).getParentFile(), true);
		LOGGER.info(deleted ? "Successfully deleted store {}" : "Failed to delete store {}", DB_NAME);
	}

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

	@Override
	public IGraphTransaction beginTransaction() throws Exception {
		if (graph == null) {
			LOGGER.warn("Cannot begin transactions outside transactional mode, entering it now");
			exitBatchMode();
		}
		return new Neo4JTransaction(this);
	}

	public GraphDatabaseService getGraph() {
		return graph;
	}

	@Override
	public IGraphNode createNode(Map<String, Object> map, String type) {
		if (map == null) {
			map = Collections.emptyMap();
		}

		if (graph != null) {
			Node n = graph.createNode(Neo4JBatchUtil.createLabel(type));
			for (String s : map.keySet()) {
				n.setProperty(s, map.get(s));
			}
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
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String t, Map<String, Object> props) {
		RelationshipType type = DynamicRelationshipType.withName(t);
		if (props == null) {
			props = Collections.emptyMap();
		}

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
	public IGraphIterable<IGraphNode> allNodes(String label) {

		if (graph != null) {

			GlobalGraphOperations g = GlobalGraphOperations.at(graph);

			return (label == null) ? (new Neo4JIterable<IGraphNode>(
					g.getAllNodes(), this)) : (new Neo4JIterable<IGraphNode>(
					g.getAllNodesWithLabel(Neo4JBatchUtil.createLabel(label)),
					this));

		} else {
			LOGGER.warn("allNodes called in a batch isert mode, please exit batch insert first, returning null");
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

	public Mode currentMode() {
		if (graph != null && batch == null)
			return Mode.TX_MODE;
		else if (graph == null && batch != null)
			return Mode.NO_TX_MODE;
		else {
			LOGGER.warn("Unknown database mode!");
			return Mode.UNKNOWN;
		}
	}

	public File logFull() throws Exception {
		File logFolder = new File(loc + "/logs");
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

			t.success();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return ret;
	}

}
