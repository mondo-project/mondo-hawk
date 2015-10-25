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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientNode;

import com.orientechnologies.lucene.OLuceneMapEntryIterator;
import com.orientechnologies.lucene.collections.LuceneResultSet;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
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
import com.tinkerpop.blueprints.impls.orient.OrientExtendedVertex;

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
			OIndex<?> index = getIndex(key);
			if ("*".equals(valueExpr)) {
				if (index.getInternal() instanceof OLuceneFullTextIndex) {
					final OLuceneFullTextIndex luceneIdx = (OLuceneFullTextIndex) index.getInternal();
					IndexReader reader;
					try {
						reader = luceneIdx.searcher().getIndexReader();
						final OLuceneMapEntryIterator<?, ?> itMapEntry = new OLuceneMapEntryIterator(reader, index.getDefinition());
						while (itMapEntry.hasNext()) {
							final Entry<?, ?> entry = itMapEntry.next();
							System.out.println(entry.getKey() + ": " + entry.getValue());
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}
				return index.cursor();
			} else {
				final Object escaped = QueryParser.escape(valueExpr.toString()).replace("\\*", "*");
				return index.iterateEntries(Collections.singleton(escaped), false);
			}
		}
	}

	private static final String SEPARATOR = "___";

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
			final Iterable<OIndexCursorFactory> iterFactories = new StarKeyValueOIndexCursorFactoryIterable(valueExpr,
					valueIdxNames);
			return new IndexCursorFactoriesIterable(iterFactories, graph);
		} else {
			final SingleKeyValueQueryOIndexCursorFactory factory = new SingleKeyValueQueryOIndexCursorFactory(valueExpr,
					key);
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

	@Override
	public IGraphIterable<IGraphNode> get(final String key, final Object valueExpr) {
		// TODO Neither get nor iterateEntries work for the index: only
		// iterateEntriesBetween seems to work?
		final Object escaped = QueryParser.escape(valueExpr.toString());
		final LuceneResultSet resultSet = (LuceneResultSet) getIndex(key).get(escaped);
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
			final OIndex<?> idx = getIndex(entry.getKey());
			final OrientNode orientNode = (OrientNode) n;
			final OrientExtendedVertex eVertex = (OrientExtendedVertex) orientNode.getVertex();
			idx.put(entry.getValue(), eVertex.getRecord());
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
		// nothing to do?
	}

	@Override
	public void delete() {
		for (String name : getValueIndexNames()) {
			graph.removeIndex(name);
		}
	}

	private OIndex<?> getIndex(final String suffix) {
		final String luceneName = name + SEPARATOR + suffix;
		final OIndexManager indexManager = getIndexManager();
		OIndex<?> idx = indexManager.getIndex(luceneName);

		if (idx == null) {
			// Indexes have to be created outside transactions
			graph.enterBatchMode();
			final OIndexFactory factory = OIndexes.getFactory("FULLTEXT", "LUCENE");
			final OSimpleKeyIndexDefinition indexDefinition = new OSimpleKeyIndexDefinition(factory.getLastVersion(),
					OType.LINK, OType.STRING);
			final ODocument metadata = new ODocument().field("analyzer",
					"org.apache.lucene.analysis.core.WhitespaceAnalyzer");
			indexManager.createIndex(luceneName, "FULLTEXT", indexDefinition, null, null, metadata, "LUCENE");
			graph.exitBatchMode();

			// We need to fetch again the index: using the one that was just
			// created will result in multithreading exceptions from OrientDB
			idx = indexManager.getIndex(luceneName);
		}
		return idx;
	}

	private OIndexManager getIndexManager() {
		final ODatabaseDocumentTx rawGraph = graph.getGraph().getRawGraph();
		final OIndexManagerProxy indexManager = rawGraph.getMetadata().getIndexManager();
		return indexManager;
	}

	private Set<String> getValueIndexNames() {
		// TODO need something better than going through all indexes here
		final String prefix = name + SEPARATOR;
		final Set<String> names = new HashSet<>();
		for (OIndex<?> idx : getIndexManager().getIndexes()) {
			if (idx.getName().startsWith(prefix)) {
				names.add(idx.getName().substring(prefix.length()));
			}
		}
		return names;
	}

}
