/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.neo4j_v2.util;

import java.util.Iterator;

import org.hawk.core.graph.*;
import org.hawk.core.model.*;
import org.neo4j.graphdb.index.IndexHits;

public class Neo4JIterable<T> implements IHawkIterable<T> {

	Iterable<?> parent;
	IGraphDatabase graph;

	public Neo4JIterable(IndexHits<?> query, IGraphDatabase graph) {
		parent = query;
		this.graph = graph;
	}

	public Neo4JIterable(Iterable<?> items, IGraphDatabase graph) {
		parent = items;
		this.graph = graph;
	}

	@Override
	public Iterator<T> iterator() {
		return new Neo4JIterator<T>(parent.iterator(), graph);
	}

	public int size() {

		if (parent instanceof IndexHits<?>)
			return ((IndexHits<?>) parent).size();
		else
			return count();

	}

	private int count() {

		int ret = 0;

		Iterator<?> it = parent.iterator();

		while (it.hasNext()) {
			it.next();
			ret++;
		}

		return ret;
	}

	@Override
	public T getSingle() {
		return iterator().next();
	}

}
