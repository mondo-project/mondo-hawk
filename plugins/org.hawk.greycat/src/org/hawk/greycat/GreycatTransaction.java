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
package org.hawk.greycat;

import org.hawk.core.graph.IGraphTransaction;

public class GreycatTransaction implements IGraphTransaction {

	private final GreycatDatabase db;

	public GreycatTransaction(GreycatDatabase db) {
		this.db = db;
	}

	@Override
	public void success() {
		db.commitLuceneIndex();
		db.save();
	}

	@Override
	public void failure() {
		db.reconnect();
	}

	@Override
	public void close() {
		// nothing to do?
	}

}
