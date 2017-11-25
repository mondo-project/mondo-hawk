/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
