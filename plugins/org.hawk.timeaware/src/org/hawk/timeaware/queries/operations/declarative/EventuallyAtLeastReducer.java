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

/**
 * Reducer which stops as soon as the expression has evaluated to <code>true</code>
 * a minimum number of times. If the expression has not evaluated that number of
 * times before the elements run out, it will reduce to <code>false</code>.
 */
public class EventuallyAtLeastReducer implements IShortCircuitReducer {

	private final int minCount;
	private int currentCount;

	public EventuallyAtLeastReducer(int minCount) {
		this.minCount = minCount;
	}

	@Override
	public Boolean reduce(boolean element) {
		if (element) {
			currentCount++;
			if (currentCount >= minCount) {
				return true;
			}
		}
		return null;
	}

	@Override
	public boolean reduce() {
		return currentCount >= minCount;
	}

}
