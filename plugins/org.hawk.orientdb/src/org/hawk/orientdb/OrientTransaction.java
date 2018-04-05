/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
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
package org.hawk.orientdb;

import org.hawk.core.graph.IGraphTransaction;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class OrientTransaction implements IGraphTransaction {
	private OrientDatabase graph;

	public OrientTransaction(OrientDatabase orientDatabase) {
		this.graph = orientDatabase;

		final ODatabaseDocumentTx db = graph.getGraph();
		if (!db.getTransaction().isActive()) {
			// OrientDB does not support nested transactions: a begin() will
			// *roll back* any transaction that we had from before!
			graph.clearPostponedIndexes();
			db.begin();
		}
	}

	@Override
	public void success() {
		graph.saveDirty();
		graph.processPostponedIndexes();
		graph.getGraph().commit();
	}

	@Override
	public void failure() {
		graph.discardDirty();
		graph.clearPostponedIndexes();
		graph.getGraph().rollback();
	}

	@Override
	public void close() {
		graph.releaseConnection();
	}

	public ODatabaseDocumentTx getOrientGraph() {
		return graph.getGraph();
	}
}
