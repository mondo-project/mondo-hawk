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
package org.hawk.neo4j_v2.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.queryParser.QueryParser;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;
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
			index = neo4jDatabase.getIndexer().forNodes(name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact"));
		else
			batchIndex = neo4jDatabase.getBatchIndexer().nodeIndex(name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "exact"));

	}

	@Override
	public void delete() {
		if (index != null)
			index.delete();
		else if (batchIndex != null)
			System.err.println("invoked delete(self) on a batchindex, this is not supported");
		else
			System.err.println("invoked delete(self) on a null index, nothing happened.");
	}

	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hawk.core.graph.IGraphNodeIndex#query(java.lang.String,
	 * java.lang.Object) Only treats * as a special character -- escapes the
	 * rest
	 */
	public IGraphIterable<IGraphNode> query(String key, final Object valueExpr) {
		if (valueExpr instanceof Number) {
			Number n = (Number)valueExpr;
			return new Neo4JIterable<IGraphNode>(() -> query(key, n, n, true, true), graph);
		}

		final Object escapedValue = QueryParser.escape(valueExpr.toString()).replace("\\*", "*");

		if (index != null) {
			return new Neo4JIterable<IGraphNode>(() -> index.query(key, escapedValue), graph);
		} else {
			return new Neo4JIterable<IGraphNode>(() -> batchIndex.query(key, escapedValue), graph);
		}
	}

	public IGraphIterable<IGraphNode> get(String key, Object valueExpr) {
		if (valueExpr instanceof Number) {
			Number n = (Number)valueExpr;
			return new Neo4JIterable<IGraphNode>(() -> query(key, n, n, true, true), graph);
		}

		if (index != null) {
			return new Neo4JIterable<IGraphNode>(() -> index.get(key, valueExpr), graph);
		} else {
			return new Neo4JIterable<IGraphNode>(() -> batchIndex.get(key, valueExpr), graph);
		}
	}

	@Override
	public void add(IGraphNode n, String s, Object value) {
		Object wrappedValue = value;
		if (wrappedValue == null) {
			return;
		}

		if (value instanceof Integer || value instanceof Long || value instanceof Double)
			wrappedValue = new ValueContext(value).indexNumeric();

		if (index != null) {
			index.add(graph.getGraph().getNodeById((long) n.getId()), s, wrappedValue);
		} else {
			Map<String, Object> m = new HashMap<>();
			m.put(s, wrappedValue);
			batchIndex.add((long) n.getId(), m);
		}

	}

	@Override
	public void remove(IGraphNode n) {
		remove(null, null, n);
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> m) {
		if (m == null) {
			m = Collections.emptyMap();
		}

		if (index != null) {
			for (String s : m.keySet()) {
				Object wrappedValue = m.get(s);
				if (wrappedValue == null) {
					continue;
				}

				if (wrappedValue instanceof Integer || wrappedValue instanceof Long || wrappedValue instanceof Double)
					wrappedValue = new ValueContext(m.get(s)).indexNumeric();

				try {
					index.add(graph.getGraph().getNodeById((long) n.getId()), s, wrappedValue);
				} catch (NullPointerException ex) {
					System.err.println("NPE!");
				}
			}

		} else {

			Map<String, Object> wrappedMap = new HashMap<>();

			for (String s : m.keySet()) {
				Object wrappedValue = m.get(s);
				if (wrappedValue == null) {
					continue;
				}

				if (wrappedValue instanceof Integer || wrappedValue instanceof Long || wrappedValue instanceof Double)
					wrappedValue = new ValueContext(m.get(s)).indexNumeric();

				wrappedMap.put(s, wrappedValue);
			}

			batchIndex.add((long) n.getId(), wrappedMap);
		}

	}

	@Override
	public void flush() {

		if (batchIndex != null) {
			// System.err.println("WARNING flush called on: " + batchIndex
			// + "(batch mode) this will affect performance.");
			batchIndex.flush();
		}
	}

	@Override
	public IGraphIterable<IGraphNode> query(String key, Number from, Number to, boolean fromInclusive, boolean toInclusive) {
		if (index != null) {
			return new Neo4JIterable<IGraphNode>(
					() -> index.query(QueryContext.numericRange(key, from, to, fromInclusive, toInclusive)), graph);
		} else {
			return new Neo4JIterable<IGraphNode>(
					() -> batchIndex.query(QueryContext.numericRange(key, from, to, fromInclusive, toInclusive)), graph);
		}

	}

	@Override
	public void remove(String key, Object value, IGraphNode n) {

		if (index != null) {
			GraphDatabaseService g = graph.getGraph();
			long l = (long) n.getId();
			Node node = null;
			try {
				node = g.getNodeById(l);
			} catch (Exception e) {
				System.err.println("tried to remove node: " + l + " from index " + name + " but it does not exist");
			}

			if (node != null) {

				if (key == null && value == null) {
					index.remove(node);
				} else if (key == null) {
					throw new UnsupportedOperationException("Removing key * does not work");
				} else if (value == null) {
					index.remove(node, key);
				} else {
					index.remove(node, key, value);
				}

			} else {
				System.err.println("tried to remove node: " + l + " from index " + name + " but it does not exist");
			}
		} else {
			System.err.println("invoked remove on a batchindex, this is not supported");
		}

	}
}
