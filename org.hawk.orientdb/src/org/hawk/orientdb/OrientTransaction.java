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

import org.hawk.core.graph.IGraphTransaction;

import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;

public class OrientTransaction implements IGraphTransaction {

	private OrientDatabase graph;

	public OrientTransaction(OrientDatabase orientDatabase) {
		this.graph = orientDatabase;
	}

	@Override
	public void success() {
		graph.getGraph().commit();
	}

	@Override
	public void failure() {
		graph.getGraph().rollback();
	}

	@Override
	public void close() {
		// graph.shutdown();
	}

	public OrientBaseGraph getOrientGraph() {
		return graph.getGraph();
	}
}
