/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb.indexes;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;

final class IndexCursorFactoryNodeIterable<T> implements IGraphIterable<T> {
	private final OIndexCursorFactory factory;
	private final OrientDatabase graph;
	private final Class<T> klass;

	IndexCursorFactoryNodeIterable(OIndexCursorFactory factory, OrientDatabase graph, Class<T> klass) {
		this.factory = factory;
		this.graph = graph;
		this.klass = klass;
	}

	@Override
	public Iterator<T> iterator() {
		final Iterator<OIdentifiable> results = factory.query();

		return new Iterator<T>(){
			@Override
			public boolean hasNext() {
				try {
					return results != null && results.hasNext();
				} catch (ArrayIndexOutOfBoundsException|NoSuchElementException ex) {
					// BUG in OrientDB: hasNext throws exceptions when there are no results
					// 2.0.x - (see LuceneResultSet.java:248 - it uses array[array.length-1]
					// 2.2.x - throws NoSuchElementException instead
					return false;
				}
			}

			@Override
			public T next() {
				final OIdentifiable next = results.next();
				ORID id = next.getIdentity();
				return graph.getElementById(id, klass);
			}

			@Override
			public void remove() {
				results.remove();
			}
			
		};
	}

	@Override
	public int size() {
		final Iterator<T> it = iterator();
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}
		return count;
	}

	@Override
	public T getSingle() {
		return iterator().next();
	}
}