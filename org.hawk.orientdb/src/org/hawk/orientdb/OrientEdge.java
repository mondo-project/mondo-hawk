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
		return edge.getLabel();
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
		return new OrientNode(edge.getVertex(Direction.OUT), db);
	}

	public IGraphNode getEndNode() {
		return new OrientNode(edge.getVertex(Direction.IN), db);
	}

	public void delete() {
		edge.remove();
	}

	public void removeProperty(String name) {
		edge.removeProperty(name);
	}
}
