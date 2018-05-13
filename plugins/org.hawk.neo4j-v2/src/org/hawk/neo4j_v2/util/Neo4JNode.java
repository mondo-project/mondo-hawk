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
package org.hawk.neo4j_v2.util;

import java.util.HashSet;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

public class Neo4JNode implements IGraphNode {

	Neo4JDatabase graph;
	Long id;
	Node node;

	public Neo4JNode(Node n, Neo4JDatabase g) {
		graph = g;
		node = n;
		id = n.getId();
	}

	public Neo4JNode(long l, Neo4JDatabase g) {
		graph = g;
		id = l;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public HashSet<String> getPropertyKeys() {

		HashSet<String> ret = new HashSet<>();

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			for (String o : node.getPropertyKeys())
				ret.add(o);
		} else {
			ret.addAll(graph.getBatch().getNodeProperties(id).keySet());
		}

		return ret;
	}

	@Override
	public Object getProperty(String name) {
		if (graph.getGraph() != null) {
			if (node == null) {
				node = graph.getGraph().getNodeById(id);
			}
			return node.getProperty(name, null);
		} else {
			return graph.getBatch().getNodeProperties(id).get(name);
		}
	}

	@Override
	public void removeProperty(String name) {

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			node.removeProperty(name);
		} else {
			System.err.println("cannot remove property in batch mode!");
		}

	}

	@Override
	public void setProperty(String name, Object value) {
		if (value == null) {
			// Neo4j does not allow null values in properties
			removeProperty(name);
		} else if (graph.getGraph() != null) {
			if (node == null) {
				node = graph.getGraph().getNodeById(id);
			}
			node.setProperty(name, value);
		} else {
			Map<String, Object> map = graph.getBatch().getNodeProperties(id);
			map.put(name, value);
			graph.getBatch().setNodeProperties(id, map);
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(() -> node.getRelationships(), graph);
		} else {
			return new Neo4JIterable<IGraphEdge>(() -> graph.getBatch().getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		if (graph.getGraph() != null) {
			if (node == null) {
				node = graph.getGraph().getNodeById(id);
			}
			return new Neo4JIterable<IGraphEdge>(() -> node.getRelationships(getNewRelationshipType(type)), graph);
		} else {
			return new Neo4JIterable<IGraphEdge>(() -> graph.getBatch().getRelationships(id), graph);
		}

	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(
					node.getRelationships(getNewRelationshipType(type), Direction.OUTGOING), graph);
		} else {
			// System.err
			// .println("warning batch neo4j does not support getting outgoing
			// edges with type, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch().getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(
					node.getRelationships(getNewRelationshipType(type), Direction.INCOMING), graph);
		} else {
			// System.err
			// .println("warning batch neo4j does not support getting incoming
			// edges with type, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch().getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(node.getRelationships(Direction.INCOMING), graph);
		} else {
			// System.err
			// .println("warning batch neo4j does not support getting incoming
			// edges, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch().getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(node.getRelationships(Direction.OUTGOING), graph);
		} else {
			// System.err
			// .println("warning batch neo4j does not support getting outgoing
			// edges, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch().getRelationships(id), graph);
		}
	}

	@Override
	public void delete() {
		if (graph.currentMode() == Mode.NO_TX_MODE) {
			throw new IllegalStateException("delete called on a batch connector to neo4j, exit batch mode first");
		}

		// Neo4j will not let us delete the node without removing all its edges first
		for (IGraphEdge edge : getEdges()) {
			edge.delete();
		}
		node.delete();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Neo4JNode))
			return false;
		else
			return id.equals(((Neo4JNode) o).getId()) && graph.equals(((Neo4JNode) o).getGraph());
	}

	@Override
	public int hashCode() {
		return id.hashCode() + graph.hashCode();
	}

	public IGraphDatabase getGraph() {
		return graph;
	}

	public static RelationshipType getNewRelationshipType(final String name) {
		return DynamicRelationshipType.withName(name);
	}

	@Override
	public String toString() {
		return "Node:" + id + " :: properties:" + getProperties();
	}

	private String getProperties() {
		String ret = "[ ";
		for (String key : getPropertyKeys())
			ret += "(" + key + ":::" + getProperty(key) + ")";
		return ret + " ]";
	}

}
