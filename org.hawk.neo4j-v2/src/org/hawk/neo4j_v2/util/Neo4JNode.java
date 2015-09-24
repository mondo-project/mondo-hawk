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
import java.util.Iterator;
import java.util.Map;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.ModelElementNode;
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
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return node.hasProperty(name) ? node.getProperty(name) : null;
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

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
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
			return new Neo4JIterable<IGraphEdge>(node.getRelationships(), graph);
		} else {
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}

	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(
					node.getRelationships(getNewRelationshipType(type)), graph);
		} else {
			System.err
					.println("warning batch neo4j does not support getting edges with type, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}

	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(node.getRelationships(
					getNewRelationshipType(type), Direction.OUTGOING), graph);
		} else {
			System.err
					.println("warning batch neo4j does not support getting outgoing edges with type, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {

		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(node.getRelationships(
					getNewRelationshipType(type), Direction.INCOMING), graph);
		} else {
			System.err
					.println("warning batch neo4j does not support getting incoming edges with type, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(
					node.getRelationships(Direction.INCOMING), graph);
		} else {
			System.err
					.println("warning batch neo4j does not support getting incoming edges, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		if (graph.getGraph() != null) {
			if (node == null)
				node = graph.getGraph().getNodeById(id);
			return new Neo4JIterable<IGraphEdge>(
					node.getRelationships(Direction.OUTGOING), graph);
		} else {
			System.err
					.println("warning batch neo4j does not support getting outgoing edges, returning all edges instead");
			return new Neo4JIterable<IGraphEdge>(graph.getBatch()
					.getRelationships(id), graph);
		}
	}

	@Override
	public void delete() {

		if (graph.getGraph() != null)
			node.delete();
		else
			System.err
					.println("delete called on a batch connector to neo4j, exit batch mode first");

	}

	@Override
	public boolean equals(Object o) {

		if (!(o instanceof Neo4JNode))
			return false;
		else
			return id.equals(((Neo4JNode) o).getId())
					&& graph.equals(((Neo4JNode) o).getGraph());
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

		Iterator<IGraphEdge> it = getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator();

		return "Node:"
				+ id
				+ " :: type: "
				+ (it.hasNext() ? it.next().getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						: "NONE(not model element)") + " :: properties:"
				+ getProperties();
	}

	private String getProperties() {
		String ret = "[ ";
		for (String key : getPropertyKeys())
			ret += "(" + key + ":::" + getProperty(key) + ")";
		return ret + " ]";
	}

}
