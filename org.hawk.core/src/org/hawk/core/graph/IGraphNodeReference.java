/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	IGraphNode getNode(IGraphDatabase graph);

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