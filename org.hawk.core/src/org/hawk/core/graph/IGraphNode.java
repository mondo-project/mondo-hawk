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
package org.hawk.core.graph;

import java.util.Set;

public interface IGraphNode {

	// public GraphNode();

	Object getId();

	Set<String> getPropertyKeys();

	Object getProperty(String name);

	void setProperty(String name, Object value);

	Iterable<IGraphEdge> getEdges();

	Iterable<IGraphEdge> getEdgesWithType(String type);

	Iterable<IGraphEdge> getOutgoingWithType(String type);

	Iterable<IGraphEdge> getIncomingWithType(String type);

	Iterable<IGraphEdge> getIncoming();

	Iterable<IGraphEdge> getOutgoing();

	void delete();

	@Override
	boolean equals(Object o);

	IGraphDatabase getGraph();

	void removeProperty(String name);
}
