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

import org.hawk.core.graph.EmptyIGraphIterable;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientIndexStore;
import org.hawk.orientdb.OrientNode;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;

/**
 * Logical index for nodes, which uses a set of SBTree indexes. We can't use the
 * indexes in the OrientDB Graph API, because they do not support querying.
 * The raw indexing API has support for composite keys, but the types of each
 * value in the tuple must be known in advance (so we can't use STRING+ANY). To
 * overcome this, we'll use up to three OrientDB indexes per logical index:
 * one for strings, one for doubles and one for integers. The actual indexes created
 * will depend on what we use (they'll be created on the fly on the first addition).
 *
 * Star queries are implemented using range queries, with additional filtering if
 * we have more than one star in the query string.
 *
 * Node index names and node index field names are kept in a singleton vertex
 * type, maintained by the {@link OrientIndexStore} class.
 */
public class OrientNodeIndex extends AbstractOrientIndex implements IGraphNodeIndex {

	public static class PostponedIndexAdd {
		private final OIndex<?> index;
		private final Object key;
		private final OIdentifiable value;

		private PostponedIndexAdd(OIndex<?> index, Object key, OIdentifiable value) {
			this.index = index;
			this.key = key;
			this.value = value;
		}

		public OIndex<?> getIndex() {
			return index;
		}

		public Object getKey() {
			return key;
		}

		public OIdentifiable getValue() {
			return value;
		}
	}
	private static final List<PostponedIndexAdd> postponed = new ArrayList<>();

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
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyOIndexCursorFactoryIterable(valueExpr, this, valueIdxNames);
			return new IndexCursorFactoriesIterable<>(iterFactories, graph, IGraphNode.class);
		} else {
			final SingleKeyOIndexCursorFactory factory = new SingleKeyOIndexCursorFactory(valueExpr, this, key);
			return new IndexCursorFactoryNodeIterable<>(factory, graph, IGraphNode.class);
		}
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final Number nFrom, final Number nTo, final boolean fromInclusive, final boolean toInclusive) {
		if (nFrom instanceof Float || nFrom instanceof Double) {
			final double dFrom = nFrom.doubleValue();
			final double dTo = nTo.doubleValue();

			final OIndex<?> idx = getIndex(Double.class);
			if (idx == null) {
				return new EmptyIGraphIterable<>();
			}
	
			return new IndexCursorFactoryNodeIterable<>(new OIndexCursorFactory() {
				@Override
				public Iterator<OIdentifiable> query() {
					return iterateEntriesBetween(key, dFrom, dTo, fromInclusive, toInclusive, idx, graph.getGraph());
				}
			}, graph, IGraphNode.class);
			
		} else {
			final int lFrom = nFrom.intValue();
			final int lTo = nTo.intValue();

			final OIndex<?> idx = getIndex(Integer.class);
			if (idx == null) {
				return new EmptyIGraphIterable<>();
			}

			return new IndexCursorFactoryNodeIterable<>(new OIndexCursorFactory() {
				@Override
				public Iterator<OIdentifiable> query() {
					// Workaround for SQL-based approach: > seems to work like
					// >= for composite keys (see RemoteIndexIT)
					return iterateEntriesBetween(key, fromInclusive ? lFrom : lFrom + 1, toInclusive ? lTo : lTo - 1,
							true, true, idx, graph.getGraph());
				}
			}, graph, IGraphNode.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public IGraphIterable<IGraphNode> get(final String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		final OIndex<?> idx = getIndex(valueExpr.getClass());
		if (idx == null) {
			return new EmptyIGraphIterable<>();
		}

		final Collection<OIdentifiable> resultSet = (Collection<OIdentifiable>) idx.get(new OCompositeKey(key, valueExpr));
		return new ResultSetIterable<>(resultSet, graph, IGraphNode.class);
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> derived) {
		if (derived == null) {
			return;
		}
		final OrientNode orientNode = (OrientNode) n;

		for (Entry<String, Object> entry : derived.entrySet()) {
			final String field = entry.getKey();
			final Object rawValue = entry.getValue();
			if (rawValue == null) {
				continue;
			}

			final Object valueExpr = normalizeValue(rawValue);
			final Class<?> valueClass = valueExpr.getClass();
			final OIndex<?> idx = getOrCreateFieldIndex(field, valueClass);

			final ORID identity = orientNode.getId();
			final OCompositeKey idxKey = new OCompositeKey(field, valueExpr);

			if (graph.getGraph().getURL().startsWith("remote:") && identity.isNew()) {
				// To avoid "Temporary RID cannot be managed at server side", we need to postpone the put
				postponed.add(new PostponedIndexAdd(idx, idxKey, identity));
				graph.addPostponedIndex(this);
			} else {
				idx.put(idxKey, identity);
			}
			orientNode.addIndexKey(idx.getName(), field, valueExpr);
		}
	}

	@Override
	public void add(IGraphNode n, String s, Object derived) {
		add(n, Collections.singletonMap(s, derived));
	}

	@Override
	public void remove(IGraphNode n) {
		final OrientNode oNode = (OrientNode)n;

		final OIdentifiable toRemove = oNode.getId().isPersistent() ? oNode.getId() : oNode.getDocument();
		List<Entry<String, Map<String, List<Object>>>> indices = oNode.removeIndexFields(escapedName);
		for (Entry<String, Map<String, List<Object>>> index : indices) {
			final String indexName = index.getKey();
			OIndex<?> oIndex = getIndexManager().getIndex(indexName);
			for (Entry<String, List<Object>> idxEntry : index.getValue().entrySet()) {
				final String field = idxEntry.getKey();
				for (Object key : idxEntry.getValue()) {
					oIndex.remove(new OCompositeKey(field, key), toRemove);
				}
			}
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

			final OIndex<?> idx = getIndex(value.getClass());
			if (idx != null) {
				idx.remove(new OCompositeKey(field, value), oNode.getDocument());
				oNode.removeIndexKey(getSBTreeIndexName(value.getClass()), field, value);
			}
		}
	}

	private void remove(String field, final OrientNode n) {
		remove(String.class, field, n);
		remove(Double.class, field, n);
		remove(Integer.class, field, n);
	}

	private void remove(final Class<?> keyClass, String field, final OrientNode n) {
		final Collection<Object> keys = n.removeIndexField(getSBTreeIndexName(keyClass), field);
		final OIndex<?> idx = getIndex(keyClass);
		final OIdentifiable toRemove = n.getId().isPersistent() ? n.getId() : n.getDocument();
		for (Object key : keys) {
			idx.remove(new OCompositeKey(field, key), toRemove);
		}
	}

	@Override
	public void flush() {
		graph.getGraph().commit();
	}

	@Override
	public void delete() {
		final boolean txWasOpen = graph.getGraph().getTransaction().isActive();
		if (txWasOpen) {
			graph.getConsole().println("Warning: prematurely committing a transaction so we can delete index " + name);
			graph.saveDirty();
			graph.getGraph().commit();
		}

		final OrientIndexStore store = graph.getIndexStore();
		final OIndexManager indexManager = getIndexManager();
		indexManager.dropIndex(getSBTreeIndexName(String.class));
		indexManager.dropIndex(getSBTreeIndexName(Double.class));
		indexManager.dropIndex(getSBTreeIndexName(Integer.class));
		store.removeNodeIndex(name);
		indexManager.flush();
		graph.getGraph().getMetadata().getIndexManager().getConfiguration().save();
	}

	/**
	 * Returns a list of index additions that should be done once the current transaction is completed. This
	 * is only needed for remote OrientDB indexes, which do not accept temporary values that only exist
	 * so far on the client.
	 */
	public List<PostponedIndexAdd> getPostponedIndexAdditions() {
		return postponed;
	}
}
