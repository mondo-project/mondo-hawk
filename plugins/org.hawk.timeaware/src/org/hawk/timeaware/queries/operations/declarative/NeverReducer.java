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
 * Reducer which immediately returns <code>false</code> if the expression ever
 * evaluates to <code>true</code>, and otherwise returns <code>true</code>.
 */
public class NeverReducer implements IShortCircuitReducer {
	@Override
	public Boolean reduce(boolean element) {
		return element ? false : null;
	}

	@Override
	public boolean reduce() {
		return true;
	}
}