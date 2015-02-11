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
package org.hawk.neo4j_v2.util;

import java.util.HashMap;
import java.util.Map;

import org.hawk.core.graph.*;
import org.hawk.core.model.IHawkIterable;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;

public class Neo4JNodeIndex implements IGraphNodeIndex {

	String name;

	Neo4JDatabase graph;

	Index<Node> index;
	BatchInserterIndex batchIndex;

	public Neo4JNodeIndex(String name, Neo4JDatabase neo4jDatabase) {

		graph = neo4jDatabase;
		this.name = name;

		if (neo4jDatabase.getGraph() != null)
			index = neo4jDatabase.getIndexer().forNodes(
					name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type",
							"exact"));
		else
			batchIndex = neo4jDatabase.getBatchIndexer().nodeIndex(
					name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type",
							"exact"));

	}

	public String getName() {
		return name;
	}

	public IHawkIterable<IGraphNode> query(String key, Object valueExpr) {

		// valueExpr =
		// StringFormatter.escapeLuceneAllSpecalCharacters(valueExpr);

		if (index != null) {
			return new Neo4JIterable<IGraphNode>(index.query(key, valueExpr),
					graph);
		} else {
			return new Neo4JIterable<IGraphNode>(batchIndex.query(key,
					valueExpr), graph);
		}
	}

	public IHawkIterable<IGraphNode> get(String key, Object valueExpr) {

		// valueExpr =
		// StringFormatter.escapeLuceneAllSpecalCharacters(valueExpr);

		if (index != null) {
			return new Neo4JIterable<IGraphNode>(index.get(key, valueExpr),
					graph);
		} else {
			return new Neo4JIterable<IGraphNode>(
					batchIndex.get(key, valueExpr), graph);
		}
	}

	@Override
	public void add(IGraphNode n, String s, Object value) {

		if (index != null) {
			index.add(graph.getGraph().getNodeById((long) n.getId()), s, value);
		} else {
			Map<String, Object> m = new HashMap<>();
			m.put(s, value);
			batchIndex.add((long) n.getId(), m);
		}

	}

	@Override
	public void remove(IGraphNode n) {

		if (index != null) {
			GraphDatabaseService g = graph.getGraph();
			long l = (long) n.getId();
			Node node = null;
			try {
				node = g.getNodeById(l);
			} catch (Exception e) {
				System.err.println("tried to remove node: " + l
						+ " from index " + name + " but it doese not exist");
			}

			if (node != null)
				index.remove(node);

		} else {
			System.err
					.println("invoked remove on a batchindex, this is not supported");
		}

	}

	@Override
	public void add(IGraphNode n, Map<String, Object> m) {

		if (index != null) {

			for (String s : m.keySet()) {
				index.add(graph.getGraph().getNodeById((long) n.getId()), s,
						m.get(s));
			}

		} else {
			batchIndex.add((long) n.getId(), m);
		}

	}

	@Override
	public void flush() {

		if (batchIndex != null)
			batchIndex.flush();

	}
}
