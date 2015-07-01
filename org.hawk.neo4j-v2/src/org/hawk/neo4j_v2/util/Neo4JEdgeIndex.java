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

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;

public class Neo4JEdgeIndex implements IGraphEdgeIndex {

	String name;

	Neo4JDatabase graph;

	Index<Relationship> index;
	BatchInserterIndex batchIndex;

	public Neo4JEdgeIndex(String name, Neo4JDatabase neo4jDatabase) {

		graph = neo4jDatabase;
		this.name = name;

		if (neo4jDatabase.getGraph() != null)
			index = neo4jDatabase.getIndexer().forRelationships(
					name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type",
							"exact"));
		else
			batchIndex = neo4jDatabase.getBatchIndexer().relationshipIndex(
					name,
					MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type",
							"exact"));

	}

	public String getName() {
		return name;
	}

	public IGraphIterable<IGraphEdge> query(String key, Object valueExpr) {

		if (index != null) {
			return new Neo4JIterable<IGraphEdge>(index.query(key, valueExpr),
					graph);
		} else {
			return new Neo4JIterable<IGraphEdge>(batchIndex.query(key,
					valueExpr), graph);
		}

	}

	public IGraphIterable<IGraphEdge> get(String key, Object valueExpr) {

		if (index != null) {
			return new Neo4JIterable<IGraphEdge>(index.get(key, valueExpr),
					graph);
		} else {
			return new Neo4JIterable<IGraphEdge>(
					batchIndex.get(key, valueExpr), graph);
		}

	}

}
