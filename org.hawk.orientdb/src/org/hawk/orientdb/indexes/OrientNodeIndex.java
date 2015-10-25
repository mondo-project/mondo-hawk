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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientNode;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.index.OIndexManagerProxy;
import com.orientechnologies.orient.core.index.OIndexes;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientExtendedVertex;

/**
 * Logical index for nodes, which combines exact indexes (based on an SBTree)
 * with Lucene indexes. Both are needed, since Lucene indexes do not support
 * iterating over all values. Vertex indexes are not useful either, as they
 * would not easily support iterating over all values indexed with a certain
 * field.
 *
 * Additionally, OrientDB has only one level of naming for indexes. To overcome
 * this, adding a key to a field F for the first time will create a new index of
 * each type named <code>name SEPARATOR_EXACT F</code> and
 * <code>name SEPARATOR_LUCENE F</code>, respectively.
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
			final boolean luceneIndex = requiresLuceneIndex(valueExpr);
			OIndex<?> index = getFieldIndex(key, luceneIndex);
			if ("*".equals(valueExpr)) {
				return index.cursor();
			} else if (luceneIndex) {
				final Object escaped = QueryParser.escape(valueExpr.toString()).replace("\\*", "*");
				return index.iterateEntries(Collections.singleton(escaped), false);
			} else {
				return index.iterateEntries(Collections.singleton(valueExpr), false);
			}
		}
	}

	static boolean requiresLuceneIndex(Object valueExpr) {
		return !"*".equals(valueExpr) && valueExpr.toString().contains("*");
	}

	private static final String SEPARATOR_EXACT = "_@exact@_";
	private static final String SEPARATOR_LUCENE = "_@lucene@_";

	private String name;
	private OrientDatabase graph;

	public OrientNodeIndex(String name, OrientDatabase graph) {
		this.name = name;
		this.graph = graph;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IGraphIterable<IGraphNode> query(final String key, final Object valueExpr) {
		if ("*".equals(key)) {
			final Set<String> valueIdxNames = getValueIndexNames();
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyValueOIndexCursorFactoryIterable(valueExpr, valueIdxNames);
			return new IndexCursorFactoriesIterable(iterFactories, graph);
		} else {
			final SingleKeyValueQueryOIndexCursorFactory factory = new SingleKeyValueQueryOIndexCursorFactory(valueExpr, key);
			return new IndexCursorFactoryIterable(factory, graph);
		}
	}

	@Override
	public IGraphIterable<IGraphNode> query(String key, int from, int to, boolean fromInclusive, boolean toInclusive) {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphIterable<IGraphNode> query(String key, double from, double to, boolean fromInclusive,
			boolean toInclusive) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IGraphIterable<IGraphNode> get(final String key, final Object valueExpr) {
		final Collection<OIdentifiable> resultSet = (Collection<OIdentifiable>) getFieldIndex(key, false).get(valueExpr);
		return new IGraphIterable<IGraphNode>() {
			@Override
			public Iterator<IGraphNode> iterator() {
				if (resultSet.isEmpty()) {
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
		for (Entry<String, Object> entry : derived.entrySet()) {
			final OIndex<?> luceneIndex = getFieldIndex(entry.getKey(), true);
			final OIndex<?> exactIndex = getFieldIndex(entry.getKey(), false);
			final OrientNode orientNode = (OrientNode) n;
			final OrientExtendedVertex eVertex = (OrientExtendedVertex) orientNode.getVertex();
			luceneIndex.put(entry.getValue(), eVertex.getRecord());
			exactIndex.put(entry.getValue(), eVertex.getRecord());
		}
	}

	@Override
	public void add(IGraphNode n, String s, Object derived) {
		add(n, Collections.singletonMap(s, derived));
	}

	@Override
	public void remove(IGraphNode n) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(String key, String value, IGraphNode n) {
		// TODO Auto-generated method stub

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
		for (String name : getValueIndexNames()) {
			graph.removeIndex(getLuceneIndexName(name));
			graph.removeIndex(getExactIndexName(name));
		}
	}

	private OIndex<?> getFieldIndex(final String field, final boolean lucene) {
		return getOrCreateIndex(field, lucene ? getLuceneIndexName(field) : getExactIndexName(field));
	}

	private OIndex<?> getOrCreateIndex(final String field, final String idxName) {
		final OIndexManager indexManager = getIndexManager();
		OIndex<?> idx = indexManager.getIndex(idxName);

		if (idx == null) {
			createIndexes(field);

			// We need to fetch again the index: using the one that was just
			// created will result in multithreading exceptions from OrientDB
			idx = indexManager.getIndex(idxName);
		}
		return idx;
	}

	/**
	 * Creates the exact and Lucene indexes paired to this field within this logical index. 
	 */
	private void createIndexes(final String field) {
		final OIndexManager indexManager = getIndexManager();

		// Indexes have to be created outside transactions
		graph.enterBatchMode();

		// Lucene index, for prefix*suffix queries
		final String luceneName = getLuceneIndexName(field);
		final OIndexFactory luceneFactory = OIndexes.getFactory("FULLTEXT", "LUCENE");
		final OSimpleKeyIndexDefinition indexDefinition = new OSimpleKeyIndexDefinition(luceneFactory.getLastVersion(), OType.STRING);
		final ODocument metadata = new ODocument().field("analyzer",
				"org.apache.lucene.analysis.core.WhitespaceAnalyzer");
		indexManager.createIndex(luceneName, "FULLTEXT", indexDefinition, null, null, metadata, "LUCENE");

		// Exact index, for exact queries and iteration
		final String exactName = getExactIndexName(field);
		final OIndexFactory exactFactory = OIndexes.getFactory("NOTUNIQUE", "SBTREE");
		final OSimpleKeyIndexDefinition exactIndexDef = new OSimpleKeyIndexDefinition(exactFactory.getLastVersion(), OType.STRING);
		indexManager.createIndex(exactName, "NOTUNIQUE", exactIndexDef, null, null, null, "SBTREE");

		graph.exitBatchMode();
	}

	private String getExactIndexName(final String field) {
		return name + SEPARATOR_EXACT + field;
	}

	private String getLuceneIndexName(final String field) {
		return name + SEPARATOR_LUCENE + field;
	}

	private OIndexManager getIndexManager() {
		final ODatabaseDocumentTx rawGraph = graph.getGraph().getRawGraph();
		final OIndexManagerProxy indexManager = rawGraph.getMetadata().getIndexManager();
		return indexManager;
	}

	private Set<String> getValueIndexNames() {
		// TODO need something better than going through all indexes here
		final String prefix = name + SEPARATOR_LUCENE;
		final Set<String> names = new HashSet<>();
		for (OIndex<?> idx : getIndexManager().getIndexes()) {
			if (idx.getName().startsWith(prefix)) {
				names.add(idx.getName().substring(prefix.length()));
			}
		}
		return names;
	}

}
