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
package org.hawk.orientdb;

import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;

public abstract class OrientIterable<T, U> implements IGraphIterable<T> {

	protected final Iterable<U> iterable;
	protected final OrientDatabase graph;

	public OrientIterable(Iterable<U> ret, OrientDatabase graph) {
		this.iterable = ret;
		this.graph = graph;
	}

	@Override
	public Iterator<T> iterator() {
		final Iterator<U> it = iterable.iterator();
		return new Iterator<T>(){

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public T next() {
				return convert(it.next());
			}

			@Override
			public void remove() {
				it.remove();
			}
			
		};
	}

	@Override
	public int size() {
		int size = 0;
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			it.next();
			++size;
		}
		return size;
	}

	@Override
	public T getSingle() {
		return iterator().next();
	}

	public OrientDatabase getGraph() {
		return graph;
	}

	protected abstract T convert(U o);

}
