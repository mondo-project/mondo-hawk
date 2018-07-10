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

	int NO_SUCH_INSTANT = -1;

	/**
	 * Returns the timepoint that this instance is currently at.
	 */
	long getTime();

	/**
	 * Returns a list with all the distinct versions of the node over time.
	 * Versions should be ordered from newest to oldest.
	 */
	List<ITimeAwareGraphNode> getAllVersions() throws Exception;

	/**
	 * Returns the earliest time instant for this node.
	 */
	long getEarliestInstant() throws Exception;

	/**
	 * Returns the earliest version for this node.
	 */
	default ITimeAwareGraphNode getEarliest() throws Exception {
		return travelInTime(getEarliestInstant());
	}

	/**
	 * Returns the instant for the previous version of this node, if
	 * there is one, or {@link #NO_SUCH_INSTANT} if there isn't.
	 */
	long getPreviousInstant() throws Exception;

	/**
	 * Returns the previous version of this node, if there is any, or
	 * <code>null</code> otherwise.
	 */
	default ITimeAwareGraphNode getPrevious() throws Exception {
		return travelInTime(getPreviousInstant());
	}

	/**
	 * Returns the most recent time instant for of this node.
	 */
	long getLatestInstant() throws Exception;

	/**
	 * Returns the most recent version for this node.
	 */
	default ITimeAwareGraphNode getLatest() throws Exception {
		return travelInTime(getLatestInstant());
	}

	/**
	 * Returns the instant for the version of this node after the current one,
	 * if there is one, or {@link #NO_SUCH_INSTANT} if there isn't. 
	 */
	long getNextInstant() throws Exception;

	/**
	 * Returns the version of this node after the current one, if there is one,
	 * or <code>null</code> if there isn't.
	 */
	default ITimeAwareGraphNode getNext() throws Exception {
		return travelInTime(getNextInstant());
	}

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
