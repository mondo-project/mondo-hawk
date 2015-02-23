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

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.unsafe.batchinsert.BatchRelationship;

public class Neo4JIterator<T> implements Iterator<T> {

	Iterator<?> parent;

	IGraphDatabase graph;

	public Neo4JIterator(Iterator<?> iterator, IGraphDatabase graph) {
		parent = iterator;
		this.graph = graph;
	}

	@Override
	public boolean hasNext() {
		return parent.hasNext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T next() {

		Object next = parent.next();

		if (next instanceof Node)
			return (T) new Neo4JNode((Node) next, (Neo4JDatabase) graph);
		else if (next instanceof Relationship)
			return (T) new Neo4JEdge((Relationship) next, (Neo4JDatabase) graph);
		else if (next instanceof BatchRelationship)
			return (T) new Neo4JEdge((BatchRelationship) next,
					(Neo4JDatabase) graph);
		else if (next instanceof Long)
			return (T) new Neo4JNode((long) next, (Neo4JDatabase) graph);
		else {
			System.err.println("next called on unknown object (class: "
					+ next.getClass() + "), returning null");
			return null;
		}
	}

	@Override
	public void remove() {
		System.err
				.println("remove called on a hawknodeiterator, not supported");

	}

}
