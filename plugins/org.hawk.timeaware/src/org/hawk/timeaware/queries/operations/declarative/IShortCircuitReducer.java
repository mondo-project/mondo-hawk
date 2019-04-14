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

public interface IShortCircuitReducer {
	/**
	 * Takes the result of evaluating the expression on the next element, and
	 * returns either <code>true</code> or <code>false</code> (which shortcircuits
	 * the evaluation), or <code>null</code>, signalling that more elements must be
	 * evaluated.
	 */
	Boolean reduce(boolean element);

	/**
	 * Returns the final conclusion once no more elements are available. This should
	 * only be invoked after {@link #reduce(Boolean)} has been invoked for all the
	 * elements.
	 */
	boolean reduce();
}