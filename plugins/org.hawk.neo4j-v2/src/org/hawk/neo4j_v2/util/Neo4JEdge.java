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

import java.util.HashSet;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.Relationship;
import org.neo4j.unsafe.batchinsert.BatchRelationship;

public class Neo4JEdge implements IGraphEdge {

	private String type;
	private Neo4JDatabase graph;
	private Relationship rel;
	private BatchRelationship batchrel;
	private Long id;

	public Neo4JEdge(Relationship r, Neo4JDatabase graph) {
		id = r.getId();
		rel = r;
		this.graph = graph;
	}

	public Neo4JEdge(BatchRelationship r, Neo4JDatabase graph) {
		id = r.getId();
		batchrel = r;
		this.graph = graph;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public String getType() {
		checkModeIsTx("getType");
		if (type == null) {
			type = rel.getType().name();
		}
		return type;
	}

	@Override
	public HashSet<String> getPropertyKeys() {
		HashSet<String> ret = new HashSet<>();

		if (graph.getGraph() != null) {
			if (rel == null)
				rel = graph.getGraph().getRelationshipById(id);
			for (String o : rel.getPropertyKeys())
				ret.add(o);
		} else {
			ret.addAll(graph.getBatch().getRelationshipProperties(id).keySet());
		}

		return ret;
	}

	@Override
	public Object getProperty(String name) {
		if (graph.getGraph() != null) {
			if (rel == null)
				rel = graph.getGraph().getRelationshipById(id);
			return rel.hasProperty(name) ? rel.getProperty(name) : null;
		} else {
			return graph.getBatch().getRelationshipProperties(id).get(name);
		}
	}

	@Override
	public void removeProperty(String name) {
		checkModeIsTx("removeProperty");

		if (rel == null)
			rel = graph.getGraph().getRelationshipById(id);
		rel.removeProperty(name);
	}

	@Override
	public void setProperty(String name, Object value) {

		if (graph.getGraph() != null) {
			if (rel == null)
				rel = graph.getGraph().getRelationshipById(id);
			rel.setProperty(name, value);
		} else {
			Map<String, Object> map = graph.getBatch()
					.getRelationshipProperties(id);
			map.put(name, value);
			graph.getBatch().setRelationshipProperties(id, map);
		}

	}

	@Override
	public IGraphNode getStartNode() {
		if (rel != null)
			return new Neo4JNode(rel.getStartNode(), graph);
		else
			return new Neo4JNode(batchrel.getStartNode(), graph);
	}

	@Override
	public IGraphNode getEndNode() {
		if (rel != null)
			return new Neo4JNode(rel.getEndNode(), graph);
		else
			return new Neo4JNode(batchrel.getEndNode(), graph);
	}

	@Override
	public void delete() {
		checkModeIsTx("delete");
		rel.delete();
	}

	protected void checkModeIsTx(String operation) {
		if (graph.currentMode() == Mode.NO_TX_MODE) {
			throw new IllegalStateException(operation + " called on a batch connector to neo4j, exit batch mode first");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Neo4JEdge))
			return false;
		else
			return id.equals(((Neo4JEdge) o).getId())
					&& graph.equals(((Neo4JEdge) o).getGraph());
	}

	private IGraphDatabase getGraph() {
		return graph;
	}

	@Override
	public int hashCode() {
		return id.hashCode() + graph.hashCode();
	}

	@Override
	public String toString() {
		return "Neo4JEdge [type=" + type + ", graph=" + graph + ", rel=" + rel
				+ ", batchrel=" + batchrel + ", id=" + id + "]";
	}

}
