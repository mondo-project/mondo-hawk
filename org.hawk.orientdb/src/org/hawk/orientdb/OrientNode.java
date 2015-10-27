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

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

import com.orientechnologies.orient.core.id.ORID;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.util.StringFactory;

public class OrientNode implements IGraphNode {
	private static final String ID_NONRESERVED = "_nonOrientId";
	private OrientDatabase graph;
	private OrientVertex vertex;

	public OrientNode(OrientVertex v, OrientDatabase graph) {
		this.vertex = v;
		this.graph = graph;
	}

	@Override
	public ORID getId() {
		return (ORID)vertex.getId();
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Set<String> keys = new HashSet<>(vertex.getPropertyKeys());
		if (keys.remove(ID_NONRESERVED)) {
			keys.add(StringFactory.ID);
		}
		return keys;
	}

	@Override
	public Object getProperty(String name) {
		return vertex.getProperty(mapToNonReservedProperty(name));
	}

	@Override
	public void setProperty(String name, Object value) {
		vertex.setProperty(mapToNonReservedProperty(name), value);
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		return new OrientEdgeIterable(vertex.getEdges(Direction.BOTH), graph);
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return new OrientEdgeIterable(vertex.getEdges(Direction.BOTH, OrientDatabase.EDGE_TYPE_PREFIX + type), graph);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return new OrientEdgeIterable(vertex.getEdges(Direction.OUT, OrientDatabase.EDGE_TYPE_PREFIX + type), graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return new OrientEdgeIterable(vertex.getEdges(Direction.IN, OrientDatabase.EDGE_TYPE_PREFIX + type), graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return new OrientEdgeIterable(vertex.getEdges(Direction.IN), graph);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return new OrientEdgeIterable(vertex.getEdges(Direction.OUT), graph);
	}

	@Override
	public void delete() {
		vertex.remove();
	}

	@Override
	public IGraphDatabase getGraph() {
		return graph;
	}

	@Override
	public void removeProperty(String name) {
		vertex.removeProperty(mapToNonReservedProperty(name));
	}

	public OrientVertex getVertex() {
		return vertex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((vertex == null) ? 0 : vertex.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrientNode other = (OrientNode) obj;
		if (vertex == null) {
			if (other.vertex != null)
				return false;
		} else if (!vertex.equals(other.vertex))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientNode [" + vertex + "]";
	}

	/**
	 * There are certain reserved property names in OrientDB which we can't use for vertices.
	 * This maps from Hawk property names to OrientDB property names.
	 */
	private String mapToNonReservedProperty(String propertyName) {
		if (StringFactory.ID.equals(propertyName)) {
			return ID_NONRESERVED;
		} else {
			return propertyName;
		}
	}
}
