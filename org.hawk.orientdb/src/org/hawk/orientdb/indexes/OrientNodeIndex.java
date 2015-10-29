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
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.index.OIndexManagerProxy;
import com.orientechnologies.orient.core.index.OIndexes;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientExtendedVertex;

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
public class OrientNodeIndex implements IGraphNodeIndex {

	private final class StarKeyValueOIndexCursorFactoryIterable implements Iterable<OIndexCursorFactory> {
		private final Object valueExpr;
		private final Set<String> valueIdxNames;

		private StarKeyValueOIndexCursorFactoryIterable(Object valueExpr, Set<String> valueIdxNames) {
			this.valueExpr = valueExpr;
			this.valueIdxNames = valueIdxNames;
		}

		@Override
		public Iterator<OIndexCursorFactory> iterator() {
			final Iterator<String> itIdxNames = valueIdxNames.iterator();
			return new Iterator<OIndexCursorFactory>() {
				@Override
				public boolean hasNext() {
					return itIdxNames.hasNext();
				}

				@Override
				public OIndexCursorFactory next() {
					return new SingleKeyValueQueryOIndexCursorFactory(valueExpr, itIdxNames.next());
				}

				@Override
				public void remove() {
					itIdxNames.remove();
				}
			};
		}
	}

	private final class SingleKeyValueQueryOIndexCursorFactory implements OIndexCursorFactory {
		private final Object valueExpr;
		private final String key;

		private SingleKeyValueQueryOIndexCursorFactory(Object valueExpr, String key) {
			this.valueExpr = valueExpr;
			this.key = key;
		}

		@Override
		public Iterator<OIdentifiable> query() {
			final OIndex<?> index = getIndexManager().getIndex(getSBTreeIndexName(key));
			if (index == null) {
				return Collections.emptyListIterator();
			}

			if (!(valueExpr instanceof String)) {
				// Not a string: go straight to the value
				return index.iterateEntries(Collections.singleton(valueExpr), false);
			}

			final String sValueExpr = valueExpr.toString();
			final int starPosition = sValueExpr.indexOf('*');
			if (starPosition < 0) {
				// No '*' found: go straight to the value
				return index.iterateEntries(Collections.singleton(valueExpr), false);
			}
			else if (starPosition == 0) {
				// value expr starts with "*"
				if (sValueExpr.length() == 1) {
					// value expr is "*": iterate over everything
					return index.cursor();
				} else {
					// value expr starts with "*": filter all entries based on the fragments between the *
					final String[] fragments = sValueExpr.split("[*]");
					final OIndexCursor cursor = index.cursor();
					return new FragmentFilteredIndexCursor(fragments, cursor);
				}
			}
			else if (starPosition == sValueExpr.length() - 1) {
				final String prefix = sValueExpr.substring(0, starPosition);
				return prefixCursor(index, prefix);
			} else {
				// value expr has one or more "*" inside: use prefix to first * as a filter and
				// then wrap with a proper filter for the rest
				final String[] fragments = sValueExpr.split("[*]");
				final OIndexCursor cursor = prefixCursor(index, sValueExpr.substring(0, starPosition));
				return new FragmentFilteredIndexCursor(fragments, cursor);
			}
		}

		private OIndexCursor prefixCursor(OIndex<?> index, String prefix) {
			// prefix is S + C + "*", where S is a substring and C is a character:
			// do ranged query between S + C and S + (C+1) (the next Unicode code point)
			final char lastChar = prefix.charAt(prefix.length() - 1);
			final String rangeStart = prefix;
			final String rangeEnd = prefix.substring(0, rangeStart.length() - 1) + Character.toString((char)(lastChar+1));
			return index.iterateEntriesBetween(rangeStart, true, rangeEnd, true, false);
		}
	}

	private static final String SEPARATOR_SBTREE = "_@sbtree@_";

	private String name;
	private OrientDatabase graph;

	public OrientNodeIndex(String name, OrientDatabase graph) {
		this.name = name;
		this.graph = graph;

		final OrientIndexStore idxStore = graph.getIndexStore();
		idxStore.addNodeIndex(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		if ("*".equals(key)) {
			final Set<String> valueIdxNames = graph.getIndexStore().getNodeFieldIndexNames(name);
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyValueOIndexCursorFactoryIterable(valueExpr, valueIdxNames);
			return new IndexCursorFactoriesIterable(iterFactories, graph);
		} else {
			final SingleKeyValueQueryOIndexCursorFactory factory = new SingleKeyValueQueryOIndexCursorFactory(valueExpr, key);
			return new IndexCursorFactoryIterable(factory, graph);
		}
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final int from, final int to, final boolean fromInclusive, final boolean toInclusive) {
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(key));
		if (idx == null) {
			return new EmptyIGraphIterable();
		}
		return new IndexCursorFactoryIterable(new OIndexCursorFactory() {
			@Override
			public Iterator<OIdentifiable> query() {
				return idx.iterateEntriesBetween(from, fromInclusive, to, toInclusive, false);
			}
		}, graph);
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final double from, final double to, final boolean fromInclusive, final boolean toInclusive) {
		final OIndex<?> idx = getIndexManager().getIndex(getSBTreeIndexName(key));
		if (idx == null) {
			return new EmptyIGraphIterable();
		}
		return new IndexCursorFactoryIterable(new OIndexCursorFactory() {
			@Override
			public Iterator<OIdentifiable> query() {
				return idx.iterateEntriesBetween(from, fromInclusive, to, toInclusive, false);
			}
		}, graph);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IGraphIterable<IGraphNode> get(final String key, Object valueExpr) {
		valueExpr = normalizeValue(valueExpr);
		final OIndex<?> idx = getOrCreateFieldIndex(key, valueExpr.getClass());
		final Collection<OIdentifiable> resultSet = (Collection<OIdentifiable>) idx.get(valueExpr);
		return new IGraphIterable<IGraphNode>() {
			@Override
			public Iterator<IGraphNode> iterator() {
				if (resultSet == null || resultSet.isEmpty()) {
					return Collections.emptyListIterator();
				} else {
					return Collections.singleton(getSingle()).iterator();
				}
			}

			@Override
			public int size() {
				return resultSet.size();
			}

			@Override
			public IGraphNode getSingle() {
				final Iterator<OIdentifiable> iterator = resultSet.iterator();
				if (iterator.hasNext()) {
					return new OrientNode(graph.getVertex(iterator.next().getIdentity()), graph);
				}
				return null;
			}

		};
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> derived) {
		final OrientNode orientNode = (OrientNode) n;
		final OrientExtendedVertex eVertex = (OrientExtendedVertex) orientNode.getVertex();

		for (Entry<String, Object> entry : derived.entrySet()) {
			final String field = entry.getKey();
			final Object valueExpr = normalizeValue(entry.getValue());
			final Class<?> valueClass = valueExpr.getClass();
			final OIndex<?> idx = getOrCreateFieldIndex(field, valueClass);
			idx.put(valueExpr, eVertex.getRecord());
		}
	}

	/**
	 * Normalizes a value expression so it'll always be either an Integer, a
	 * Double or a String.
	 */
	private static Object normalizeValue(Object valueExpr) {
		if (valueExpr instanceof Byte || valueExpr instanceof Short || valueExpr instanceof Long) {
			valueExpr = ((Number)valueExpr).intValue();
		} else if (valueExpr instanceof Float) {
			valueExpr = ((Float)valueExpr).doubleValue();
		} else if (valueExpr instanceof String || valueExpr instanceof Integer || valueExpr instanceof Double) {
			return valueExpr;
		}
		return valueExpr.toString();
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
				idx.remove(value, oNode.getVertex());
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
		final OrientBaseGraph orientGraph = graph.getGraph();
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

	private OIndex<?> getOrCreateFieldIndex(final String field, final Class<?> valueClass) {
		final String idxName = getSBTreeIndexName(field);
		final OIndexManager indexManager = getIndexManager();
		OIndex<?> idx = indexManager.getIndex(idxName);

		if (idx == null) {
			createIndex(field, valueClass);

			// We need to fetch again the index: using the one that was just
			// created will result in multithreading exceptions from OrientDB
			idx = indexManager.getIndex(idxName);
		}
		return idx;
	}

	/**
	 * Creates the SBTree index paired to this field within this logical index.
	 */
	private void createIndex(final String field, final Class<?> keyClass) {
		final OIndexManager indexManager = getIndexManager();

		// Indexes have to be created outside transactions
		final boolean wasTransactional = graph.currentMode() == OrientDatabase.TX_MODE;
		if (wasTransactional) {
			graph.enterBatchMode();
		}

		// Index key type
		OType keyType = OType.STRING;
		if (keyClass == Byte.class || keyClass == Short.class || keyClass == Integer.class || keyClass == Long.class) {
			keyType = OType.INTEGER;
		} else if (keyClass == Float.class || keyClass == Double.class) {
			keyType = OType.DOUBLE;
		}

		// Create SBTree NOTUNIQUE index
		final String idxName = getSBTreeIndexName(field);
		final OIndexFactory idxFactory = OIndexes.getFactory("NOTUNIQUE", "SBTREE");
		final OSimpleKeyIndexDefinition indexDef = new OSimpleKeyIndexDefinition(idxFactory.getLastVersion(), keyType);
		indexManager.createIndex(idxName, "NOTUNIQUE", indexDef, null, null, null, "SBTREE");

		graph.getIndexStore().addNodeFieldIndex(name, field);

		if (wasTransactional) {
			graph.exitBatchMode();
		}
	}

	private String getSBTreeIndexName(final String field) {
		return name + SEPARATOR_SBTREE + field.replace(':', '!');
	}

	private OIndexManager getIndexManager() {
		final ODatabaseDocumentTx rawGraph = graph.getGraph().getRawGraph();
		final OIndexManagerProxy indexManager = rawGraph.getMetadata().getIndexManager();
		return indexManager;
	}

}
