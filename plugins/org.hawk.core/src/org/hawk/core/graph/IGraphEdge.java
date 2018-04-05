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
package org.hawk.core.graph;

import java.util.Set;

public interface IGraphEdge {

	// public GraphEdge(GraphNode start, GraphNode end, String type)

	Object getId();

	String getType();

	Set<String> getPropertyKeys();

	Object getProperty(String name);

	void setProperty(String name, Object value);

	IGraphNode getStartNode();

	IGraphNode getEndNode();

	void delete();

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

	void removeProperty(String name);

}
