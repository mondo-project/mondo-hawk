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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientIndexStore;
import org.hawk.orientdb.OrientNode;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;

/**
 * Logical index for nodes, which uses a set of SBTree indexes. We can't use the
 * indexes in the OrientDB Graph API, because they do not support querying.
 * However, the raw indexing API has only one level of naming for indexes. To
 * overcome this, adding a key to a field F for the first time will create a new
 * index of each type named <code>name SEPARATOR_SBTREE F</code>.
 *
 * Star queries are implemented using range queries, with additional filtering if
 * we have more than one star in the query string.
 *
 * Node index names and node index field names are kept in a singleton vertex
 * type, maintained by the {@link OrientIndexStore} class.
 */
public class OrientNodeIndex extends AbstractOrientIndex implements IGraphNodeIndex {

	public OrientNodeIndex(String name, OrientDatabase graph) {
		super(name, graph, IndexType.NODE);

		final OrientIndexStore idxStore = graph.getIndexStore();
		idxStore.addNodeIndex(name);
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		if ("*".equals(key)) {
			final Set<String> valueIdxNames = graph.getIndexStore().getNodeFieldIndexNames(name);
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyValueOIndexCursorFactoryIterable(valueExpr, this, valueIdxNames);
			return new IndexCursorFactoriesIterable<>(iterFactories, graph, IGraphNode.class);
		} else {
			final SingleKeyValueQueryOIndexCursorFactory factory = new SingleKeyValueQueryOIndexCursorFactory(valueExpr, this, key);
			return new IndexCursorFactoryNodeIterable<>(factory, graph, IGraphNode.class);
		}
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final int from, final int to, final boolean fromInclusive, final boolean toInclusive) {
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(key));
		if (idx == null) {
			return new EmptyIGraphIterable<>();
		}
		return new IndexCursorFactoryNodeIterable<>(new OIndexCursorFactory() {
			@Override
			public Iterator<OIdentifiable> query() {
				return idx.iterateEntriesBetween(from, fromInclusive, to, toInclusive, false);
			}
		}, graph, IGraphNode.class);
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final double from, final double to, final boolean fromInclusive, final boolean toInclusive) {
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(key));
		if (idx == null) {
			return new EmptyIGraphIterable<>();
		}
		return new IndexCursorFactoryNodeIterable<>(new OIndexCursorFactory() {
			@Override
			public Iterator<OIdentifiable> query() {
				return idx.iterateEntriesBetween(from, fromInclusive, to, toInclusive, false);
			}
		}, graph, IGraphNode.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IGraphIterable<IGraphNode> get(final String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(key));
		if (idx == null) {
			return new EmptyIGraphIterable<>();
		}

		final Collection<OIdentifiable> resultSet = (Collection<OIdentifiable>) idx.get(valueExpr);
		return new ResultSetIterable<>(resultSet, graph, IGraphNode.class);
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> derived) {
		final OrientNode orientNode = (OrientNode) n;

		for (Entry<String, Object> entry : derived.entrySet()) {
			final String field = entry.getKey();
			final Object valueExpr = normalizeValue(entry.getValue());
			final Class<?> valueClass = valueExpr.getClass();
			final OIndex<?> idx = getOrCreateFieldIndex(field, valueClass);
			idx.put(valueExpr, orientNode.getDocument());
		}
	}

	@Override
	public void add(IGraphNode n, String s, Object derived) {
		add(n, Collections.singletonMap(s, derived));
	}

	@Override
	public void remove(IGraphNode n) {
		final OrientNode oNode = (OrientNode)n;
		final OrientIndexStore store = graph.getIndexStore();
		for (String fieldName : store.getNodeFieldIndexNames(name)) {
			remove(fieldName, oNode);
		}
	}

	@Override
	public void remove(String field, Object value, IGraphNode n) {
		final OrientNode oNode = (OrientNode)n;

		if (field == null && value == null) {
			remove(n);
		} else if (field == null) {
			final OrientIndexStore store = graph.getIndexStore();
			for (String fieldName : store.getNodeFieldIndexNames(name)) {
				remove(fieldName, value, n);
			}
		} else if (value == null) {
			remove(field, oNode);
		} else {
			value = normalizeValue(value);

			final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(field));
			if (idx != null) {
				idx.remove(value, oNode.getDocument());
			}
		}
	}

	private void remove(String field, final OrientNode n) {
		final List<Object> keysToRemove = new ArrayList<>();
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(field));
		if (idx == null) return;

		final OIndexCursor cursor = idx.cursor();
		for (Entry<Object, OIdentifiable> entry = cursor.nextEntry(); entry != null; entry = cursor.nextEntry()) {
			if (n.getId().equals(entry.getValue().getIdentity())) {
				keysToRemove.add(entry.getKey());
			}
		}

		for (Object key : keysToRemove) {
			idx.remove(key);
		}
	}

	@Override
	public void flush() {
		final ODatabaseDocumentTx orientGraph = graph.getGraph();
		if (orientGraph != null) {
			orientGraph.commit();
		}
	}

	@Override
	public void delete() {
		OrientIndexStore store = graph.getIndexStore();
		for (String fieldName : store.getNodeFieldIndexNames(name)) {
			final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(fieldName));
			if (idx != null) {
				idx.delete();
			}
		}
		store.removeNodeIndex(name);
	}

}
