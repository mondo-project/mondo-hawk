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

import java.util.Set;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class OrientEdge implements IGraphEdge {

	private Edge edge;
	private OrientDatabase db;

	public OrientEdge(Edge e, OrientDatabase orientDatabase) {
		this.edge = e;
		this.db = orientDatabase;
	}

	public Object getId() {
		return edge.getId();
	}

	public String getType() {
		return edge.getLabel().replaceFirst(OrientDatabase.EDGE_TYPE_PREFIX, "");
	}

	public Set<String> getPropertyKeys() {
		return edge.getPropertyKeys();
	}

	public Object getProperty(String name) {
		return edge.getProperty(name);
	}

	public void setProperty(String name, Object value) {
		edge.setProperty(name, value);
	}

	public IGraphNode getStartNode() {
		return new OrientNode((OrientVertex) edge.getVertex(Direction.OUT), db);
	}

	public IGraphNode getEndNode() {
		return new OrientNode((OrientVertex) edge.getVertex(Direction.IN), db);
	}

	public void delete() {
		edge.remove();
	}

	public void removeProperty(String name) {
		edge.removeProperty(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edge == null) ? 0 : edge.hashCode());
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
		OrientEdge other = (OrientEdge) obj;
		if (edge == null) {
			if (other.edge != null)
				return false;
		} else if (!edge.equals(other.edge))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientEdge [" + edge + "]";
	}

}
