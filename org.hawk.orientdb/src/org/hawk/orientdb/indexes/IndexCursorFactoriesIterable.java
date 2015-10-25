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
package org.hawk.orientdb.indexes;

import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.orientdb.OrientDatabase;

final class IndexCursorFactoriesIterable implements IGraphIterable<IGraphNode> {
	private final Iterable<OIndexCursorFactory> iterFactories;
	private final OrientDatabase graph;

	IndexCursorFactoriesIterable(Iterable<OIndexCursorFactory> iterFactories, OrientDatabase graph) {
		this.iterFactories = iterFactories;
		this.graph = graph;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		final Iterator<OIndexCursorFactory> itFactory = iterFactories.iterator(); 
		return new Iterator<IGraphNode>() {
			Iterator<IGraphNode> currentIterator = null;

			@Override
			public boolean hasNext() {
				if (currentIterator == null || !currentIterator.hasNext()) {
					if (itFactory.hasNext()) {
						currentIterator = new IndexCursorFactoryIterable(itFactory.next(), graph).iterator();
					} else {
						return false;
					}
				}
				return currentIterator.hasNext();
			}

			@Override
			public IGraphNode next() {
				return currentIterator.next();
			}

			@Override
			public void remove() {
				currentIterator.remove();
			}
			
		};
	}

	@Override
	public int size() {
		int count = 0;
		for (OIndexCursorFactory factory : iterFactories) {
			count += new IndexCursorFactoryIterable(factory, graph).size();
		}
		return count;
	}

	@Override
	public IGraphNode getSingle() {
		for (OIndexCursorFactory factory : iterFactories) {
			Iterator<IGraphNode> it = new IndexCursorFactoryIterable(factory, graph).iterator();
			if (it.hasNext()) {
				return it.next();
			}
		}
		return null;
	}
}