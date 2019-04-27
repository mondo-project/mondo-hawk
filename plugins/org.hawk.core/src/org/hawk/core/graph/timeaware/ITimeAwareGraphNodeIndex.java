/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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

import org.hawk.core.graph.IGraphNodeIndex;

/**
 * Variant of a node index which can travel in time and do queries over the
 * timeline of a node.
 */
public interface ITimeAwareGraphNodeIndex extends IGraphNodeIndex {

	/**
	 * Returns a new version of this index, which is forced to answer queries in a
	 * certain timepoint rather than the current timepoint of the graph.
	 */
	ITimeAwareGraphNodeIndex travelInTime(long timepoint);

	/**
	 * Returns all the timepoints of a node which have a certain value
	 * associated with a query, from latest to earliest.
	 */
	List<Long> getVersions(ITimeAwareGraphNode n, String key, Object exactValue);

	/**
	 * Returns the earliest timepoint since the current timepoint of <code>taNode</code>
	 * for which the key was equal to the specified value, or <code>null</code> if it
	 * does not exist.
	 */
	Long getEarliestVersionSince(ITimeAwareGraphNode taNode, String key, Object exactValue);

}
