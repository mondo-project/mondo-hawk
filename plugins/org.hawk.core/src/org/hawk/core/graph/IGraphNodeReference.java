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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.graph;

import org.hawk.core.query.IQueryEngine;

/**
 * Identifier-based reference to a node in the graph. Query result elements
 * corresponding to model elements should implement this interface.
 */
public interface IGraphNodeReference {
	/**
	 * either return the node from the <code>graph</code>
	 * 
	 * @param graph
	 * @return
	 */
	IGraphNode getNode();

	String getTypeName();

	IQueryEngine getContainerModel();

	/**
	 * If the caller only requires the identifier, otherwise use (
	 * {@link #getNode(IGraphDatabase)}
	 * 
	 * @return
	 */
	String getId();
}