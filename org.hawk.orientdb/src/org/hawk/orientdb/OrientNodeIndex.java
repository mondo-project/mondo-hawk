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
package org.hawk.orientdb;

import java.util.Arrays;
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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.index.OIndexManagerProxy;
import com.orientechnologies.orient.core.index.OIndexes;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientExtendedVertex;

public class OrientNodeIndex implements IGraphNodeIndex {

	private final class OrientNodeIndexAllKeysCursorIterable implements IGraphIterable<IGraphNode> {
		private final Object valueExpr;
		private final Set<String> valueIdxNames;

		private OrientNodeIndexAllKeysCursorIterable(Object valueExpr, Set<String> valueIdxNames) {
			this.valueExpr = valueExpr;
			this.valueIdxNames = valueIdxNames;
		}

		@Override
		public Iterator<IGraphNode> iterator() {
			final Iterator<String> itIdxName = valueIdxNames.iterator(); 
			return new Iterator<IGraphNode>() {
				Iterator<IGraphNode> currentIterator = null;

				@Override
				public boolean hasNext() {
					if (currentIterator == null || !currentIterator.hasNext()) {
						if (itIdxName.hasNext()) {
							final OIndex<?> idx = getIndex(itIdxName.next());
							currentIterator = new OrientNodeIndexCursorIterable(valueExpr, idx).iterator();
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
			for (String name : valueIdxNames) {
				OIndex<?> idx = getIndex(name);
				count += new OrientNodeIndexCursorIterable(valueExpr, idx).size();
			}
			return count;
		}

		@Override
		public IGraphNode getSingle() {
			for (String name : valueIdxNames) {
				OIndex<?> idx = getIndex(name);
				Iterator<IGraphNode> it = new OrientNodeIndexCursorIterable(valueExpr, idx).iterator();
				if (it.hasNext()) {
					return it.next();
				}
			}
			return null;
		}
	}

	private final class OrientNodeIndexCursorIterable implements IGraphIterable<IGraphNode> {
		private final Object valueExpr;
		private final OIndex<?> index;

		private OrientNodeIndexCursorIterable(Object valueExpr, OIndex<?> finalIdx) {
			this.valueExpr = QueryParser.escape(valueExpr.toString()).replace("\\*", "*");
			this.index = finalIdx;
		}

		@Override
		public Iterator<IGraphNode> iterator() {
			final OIndexCursor results = runQuery();

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

		private OIndexCursor runQuery() {
			if ("*".equals(valueExpr)) {
				final Object firstKey = QueryParser.escape(index.getFirstKey().toString());
				final Object lastKey = QueryParser.escape(index.getLastKey().toString());
				return index.iterateEntriesBetween(firstKey, true, lastKey, true, false);
			} else {
				return index.iterateEntries(Arrays.asList(valueExpr), false);
			}
		}

		@Override
		public int size() {
			final OIndexCursor results = runQuery();
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
			return new OrientNodeIndexAllKeysCursorIterable(valueExpr, valueIdxNames);
		} else {
			OIndex<?> idx = getIndex(key);
			return new OrientNodeIndexCursorIterable(valueExpr, idx);
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
	public IGraphIterable<IGraphNode> get(String key, Object valueExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> derived) {
		for (Entry<String, Object> entry : derived.entrySet()) {
			final OIndex<?> idx = getIndex(entry.getKey());
			final OrientNode orientNode = (OrientNode)n;
			final OrientExtendedVertex eVertex = (OrientExtendedVertex)orientNode.getVertex();
			idx.put(entry.getValue(), eVertex.getIdentity());
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
		final String fullName = name + SEPARATOR + suffix;
		final OIndexManager indexManager = getIndexManager();
		OIndex<?> idx = indexManager.getIndex(fullName);
		if (idx == null) {
			// Indexes have to be created outside transactions
			graph.enterBatchMode();
			final OIndexFactory factory = OIndexes.getFactory("FULLTEXT", "LUCENE");
			final OSimpleKeyIndexDefinition indexDefinition = new OSimpleKeyIndexDefinition(factory.getLastVersion(), OType.LINK, OType.STRING);
			indexManager.createIndex(fullName, "FULLTEXT", indexDefinition, null, null, null, "LUCENE");
			graph.exitBatchMode();
	
			// We need to fetch again the index: using the one that was just created will result in multithreading exceptions from OrientDB
			idx = indexManager.getIndex(fullName);
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
