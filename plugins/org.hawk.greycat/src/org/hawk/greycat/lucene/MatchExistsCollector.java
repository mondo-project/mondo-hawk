/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
package org.hawk.greycat.lucene;

import java.io.IOException;

import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.SimpleCollector;

public class MatchExistsCollector extends SimpleCollector {
	private boolean matchFound = false;

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public void collect(int doc) throws IOException {
		matchFound = true;
		throw new CollectionTerminatedException();
	}

	public boolean isMatchFound() {
		return matchFound;
	}
}