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

import java.util.List;

import org.hawk.core.graph.IGraphNode;

public interface ITimeAwareGraphNode extends IGraphNode {

	/**
	 * Returns the timepoint that this instance is currently at.
	 */
	long getTime();

	/**
	 * Returns a list with all the distinct versions of the node over time.
	 */
	List<ITimeAwareGraphNode> getAllVersions() throws Exception;

	/**
	 * Ends the lifespan of the node at its current timepoint.
	 */
	void end();

	/**
	 * Tries to travel in time to another point in the timeline. Returns
	 * <code>null</code> if this node was not alive at that moment.
	 */
	ITimeAwareGraphNode travelInTime(long time);

}
