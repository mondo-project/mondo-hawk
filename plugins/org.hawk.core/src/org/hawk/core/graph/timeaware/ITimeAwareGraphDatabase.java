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

import org.hawk.core.graph.IGraphDatabase;

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
	 * Returns the currently active timepoint.
	 */
	long getTime();

	/**
	 * Changes the currently active timepoint. Nodes created after this
	 * call will only be visible after this timepoint, until they are
	 * invalidated. Changes to existing nodes will only be visible from
	 * this point onwards, until they are overwritten down the timeline.
	 */
	void setTime(long time);
}
