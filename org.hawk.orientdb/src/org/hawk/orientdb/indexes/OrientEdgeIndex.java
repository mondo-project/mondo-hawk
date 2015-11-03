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

import java.util.Collection;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientIndexStore;
import org.hawk.orientdb.util.EmptyIGraphIterable;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;

public class OrientEdgeIndex extends AbstractOrientIndex implements IGraphEdgeIndex {

	public OrientEdgeIndex(String name, OrientDatabase graph) {
		super(name, graph, IndexType.EDGE);

		final OrientIndexStore idxStore = graph.getIndexStore();
		idxStore.addEdgeIndex(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IGraphIterable<IGraphEdge> query(String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		if ("*".equals(key)) {
			final Set<String> valueIdxNames = graph.getIndexStore().getEdgeFieldIndexNames(name);
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyOIndexCursorFactoryIterable(valueExpr, this, valueIdxNames);
			return new IndexCursorFactoriesIterable<>(iterFactories, graph, IGraphEdge.class);
		} else {
			final SingleKeyOIndexCursorFactory factory = new SingleKeyOIndexCursorFactory(valueExpr, this, key);
			return new IndexCursorFactoryNodeIterable<>(factory, graph, IGraphEdge.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public IGraphIterable<IGraphEdge> get(String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(valueExpr.getClass()));
		if (idx == null) {
			return new EmptyIGraphIterable<>();
		}

		final Collection<OIdentifiable> resultSet = (Collection<OIdentifiable>) idx.get(valueExpr);
		return new ResultSetIterable<>(resultSet, graph, IGraphEdge.class);
	}

}
