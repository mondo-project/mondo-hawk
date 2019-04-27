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
package org.hawk.timeaware.queries.operations.patterns;

/**
 * Reducer which evaluates all expressions, and then counts how many times the
 * expression evaluated to <code>true</code>. The number of times should be
 * greater than zero (so it did happen), and less than or equal to the bound.  
 */
public class EventuallyAtMostReducer implements IShortCircuitReducer {

	private final int maxCount;
	private int currentCount;

	public EventuallyAtMostReducer(int maxCount) {
		this.maxCount = maxCount;
	}

	@Override
	public Boolean reduce(boolean element) {
		if (element) {
			currentCount++;
		}
		return null;
	}

	@Override
	public boolean reduce() {
		return currentCount > 0 && currentCount <= maxCount;
	}

}
