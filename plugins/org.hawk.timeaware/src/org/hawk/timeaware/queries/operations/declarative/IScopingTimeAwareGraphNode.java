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
package org.hawk.timeaware.queries.operations.declarative;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Time-aware graph node that operates within a limited scope, and allows users
 * to escape that scope on demand.
 */
public interface IScopingTimeAwareGraphNode extends ITimeAwareGraphNode {

	/**
	 * Returns a version of the graph node in the same timepoint, but without
	 * any scoping of its history.
	 */
	ITimeAwareGraphNode unscope();

}
