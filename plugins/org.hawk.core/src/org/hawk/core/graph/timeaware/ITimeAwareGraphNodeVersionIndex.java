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

/**
 * Interface for a component that keeps track of versions of interest of various
 * nodes, according to a label. For instance, this could be an index of all node
 * versions where a particular value went below a threshold.
 *
 * TODO: context-free versions of these operations (e.g. all versions of all
 * nodes mentioned in this index).
 */
public interface ITimeAwareGraphNodeVersionIndex {

	/** Adds the current version of the node to the index. */
	void addVersion(ITimeAwareGraphNode n);

	/** Removes the current version of the node from the index. */
	void removeVersion(ITimeAwareGraphNode n);
	
	/** Removes all versions of the node from the index. */
	void removeAllVersions(ITimeAwareGraphNode n);

	/** Returns all the versions of this node that appear in the index, from oldest to newest. */
	Iterable<ITimeAwareGraphNode> getAllVersions(ITimeAwareGraphNode n);

	/**
	 * Returns all the versions of this node that appear in the index, at the
	 * current or later timepoints, from oldest to newest.
	 */
	Iterable<ITimeAwareGraphNode> getVersionsSince(ITimeAwareGraphNode n);

	/**
	 * Returns all the versions of this node that appear in the index, strictly
	 * after the current timepoint, from oldest to newest.
	 */
	Iterable<ITimeAwareGraphNode> getVersionsAfter(ITimeAwareGraphNode n);

	/**
	 * Returns all the versions of this node that appear in the index, at the
	 * current or prior timepoints, from oldest to newest.
	 */
	Iterable<ITimeAwareGraphNode> getVersionsUntil(ITimeAwareGraphNode n);

	/**
	 * Returns all the versions of this node that appear in the index, strictly
	 * before the current timepoint, from oldest to newest.
	 */
	Iterable<ITimeAwareGraphNode> getVersionsBefore(ITimeAwareGraphNode n);

	/** Ensures the current contents of the index are written to disk. */
	void flush();

	/**
	 * Deletes the version index permanently from the storage. All other
	 * {@link ITimeAwareGraphNodeVersionIndex} instances will be invalidated.
	 */
	void delete();

}
