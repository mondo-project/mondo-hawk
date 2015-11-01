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
package org.hawk.orientdb.util;

import java.util.Collections;
import java.util.Iterator;

import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Iterable over the documents on a cluster. Workaround for the fact that
 * while {@link ORecordIteratorCluster} implements both {@link Iterator} and
 * {@link Iterable}, it's not reusable as an Iterable would be. It also reports
 * missing clusters as empty, instead of throwing exceptions.
 */
public class OrientClusterDocumentIterable implements Iterable<ORID> {

	private final String clusterName;
	private final OrientDatabase db;

	public OrientClusterDocumentIterable(String clusterName, OrientDatabase db) {
		this.clusterName = clusterName;
		this.db = db;
	}

	@Override
	public Iterator<ORID> iterator() {
		final int clusterId = db.getGraph().getClusterIdByName(clusterName);
		if (clusterId == -1) {
			return Collections.emptyListIterator();
		}

		final ORecordIteratorCluster<ODocument> it = db.getGraph().browseCluster(clusterName);
		return new Iterator<ORID>(){

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public ORID next() {
				return it.next().getIdentity();
			}
			
		};
	}
}
