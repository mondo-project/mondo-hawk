/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.core.graph.timeaware;

import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNodeIndex;

/**
 * Abstraction for a graph database that understands the concept of time as a
 * linear sequence, where 0 is the "origin timepoint". Nodes and edges created
 * at moment X are available from then on, until they are invalidated.
 *
 * Operations with the graph should work at the origin timepoint for any
 * "timeless" concepts such as metamodels, and at epoch-based timepoints for
 * any time-aware concepts (model elements).
 */
public interface ITimeAwareGraphDatabase extends IGraphDatabase {

	/**
	 * Returns all the nodes of a certain type, at a certain timepoint.
	 */
	IGraphIterable<? extends ITimeAwareGraphNode> allNodes(String label, long time);

	/**
	 * Returns the currently active timepoint.
	 */
	long getTime();

	/**
	 * Changes the currently active timepoint. Nodes created after this
	 * call will only be visible after this timepoint, until they are
	 * invalidated. Changes to existing nodes will only be visible from
	 * this point onwards, until they are overwritten down the timeline.
	 *
	 * NOTE: it may be a good idea to use a synchronised block on a DB
	 * while relying on a particular timepoint, to ensure that no other
	 * caller will change it in the middle of an operation.
	 */
	void setTime(long time);

	@Override
	ITimeAwareGraphNode createNode(Map<String, Object> props, String label);

	@Override
	ITimeAwareGraphNode getNodeById(Object id);

	@Override
	IGraphIterable<? extends ITimeAwareGraphNode> allNodes(String label);

	@Override
	ITimeAwareGraphNodeIndex getFileIndex();

	@Override
	ITimeAwareGraphNodeIndex getOrCreateNodeIndex(String name);

}
