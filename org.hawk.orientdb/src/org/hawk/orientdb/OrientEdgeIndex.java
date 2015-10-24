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

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;

public class OrientEdgeIndex implements IGraphEdgeIndex {

	private String name;
	private Index<Edge> index;
	private OrientDatabase graph;

	public OrientEdgeIndex(String name, Index<Edge> idx, OrientDatabase graph) {
		this.name = name;
		this.index = idx;
		this.graph = graph;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IGraphIterable<IGraphEdge> query(String key, Object valueExpr) {
		return new OrientEdgeIterable(index.query(key, valueExpr), graph);
	}

	@Override
	public IGraphIterable<IGraphEdge> get(String key, Object valueExpr) {
		return new OrientEdgeIterable(index.get(key, valueExpr), graph);
	}

}
