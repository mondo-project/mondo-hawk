/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
package org.hawk.orientdb.indexes;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.db.record.OIdentifiable;

final class ResultSetIterable<T> implements IGraphIterable<T> {
	private final Collection<OIdentifiable> resultSet;
	private final Class<T> klass;
	private final OrientDatabase db;

	public ResultSetIterable(Collection<OIdentifiable> resultSet, OrientDatabase db, Class<T> klass) {
		this.resultSet = resultSet;
		this.klass = klass;
		this.db = db;
	}

	@Override
	public Iterator<T> iterator() {
		if (resultSet == null || resultSet.isEmpty()) {
			return Collections.emptyListIterator();
		} else {
			final Iterator<OIdentifiable> itIdentifiable = resultSet.iterator();
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return itIdentifiable.hasNext();
				}

				@Override
				public T next() {
					return db.getElementById(itIdentifiable.next().getIdentity(), klass);
				}

				@Override
				public void remove() {
					itIdentifiable.remove();
				}
			};
		}
	}

	@Override
	public int size() {
		return resultSet.size();
	}

	@Override
	public T getSingle() {
		return iterator().next();
	}
}