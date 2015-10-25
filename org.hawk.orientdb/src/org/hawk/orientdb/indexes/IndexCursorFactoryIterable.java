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
import org.hawk.orientdb.OrientNode;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndexCursor;

final class IndexCursorFactoryIterable implements IGraphIterable<IGraphNode> {
	private OIndexCursorFactory factory;
	private OrientDatabase graph;

	IndexCursorFactoryIterable(OIndexCursorFactory factory, OrientDatabase graph) {
		this.factory = factory;
		this.graph = graph;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		final Iterator<OIdentifiable> results = factory.query();

		return new Iterator<IGraphNode>(){
			@Override
			public boolean hasNext() {
				return results != null && results.hasNext();
			}

			@Override
			public IGraphNode next() {
				ORID id = results.next().getIdentity();
				return new OrientNode(graph.getVertex(id), graph);
			}

			@Override
			public void remove() {
				results.remove();
			}
			
		};
	}

	@Override
	public int size() {
		final Iterator<OIdentifiable> results = factory.query();
		int count = 0;
		while (results != null && results.hasNext()) {
			count++;
			results.next();
		}
		return count;
	}

	@Override
	public IGraphNode getSingle() {
		final Iterator<IGraphNode> it = iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}
}