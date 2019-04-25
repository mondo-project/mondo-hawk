/*******************************************************************************
 * Copyright (c) 2019 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.greycat.lucene.versions;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.greycat.AbstractGreycatDatabase;
import org.hawk.greycat.GreycatNode;
import org.hawk.greycat.lucene.AbstractLuceneIndexer;
import org.hawk.greycat.lucene.ListCollector;
import org.hawk.greycat.lucene.versions.GreycatLuceneVersionIndexer.GreycatLuceneVersionIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lucene-based index of relevant versions of nodes in the graph. It follows a very different
 * structure to the usual node indices, so it made more sense to keep it in its own class. Version
 * indices are much simpler than node indices: they are simply a set of time + node ID pairs. 
 */
public class GreycatLuceneVersionIndexer extends AbstractLuceneIndexer<GreycatLuceneVersionIndex> {

	public class NodeVersionCollector extends ListCollector {

		protected NodeVersionCollector(IndexSearcher searcher) {
			super(searcher);
		}

		public Iterator<ITimeAwareGraphNode> getNodeIterator() {
			final Iterator<Integer> itIdentifiers = docIds.iterator();
			return new Iterator<ITimeAwareGraphNode>() {
				@Override
				public boolean hasNext() {
					return itIdentifiers.hasNext();
				}

				@Override
				public ITimeAwareGraphNode next() {
					int docId = itIdentifiers.next();
					try {
						final Document document = searcher.doc(docId);
						final long id = document.getField(NODEID_FIELD).numericValue().longValue();
						final long time = document.getField(NODETIME_FIELD).numericValue().longValue();
						return new GreycatNode(database, database.getWorld(), time, id);
					} catch (IOException e) {
						LOGGER.error("Could not retrieve document with ID " + docId, e);
						throw new NoSuchElementException();
					}
				}
			};
		}

	}

	public class LuceneVersionIterable implements Iterable<ITimeAwareGraphNode> {

		private final Query query;
		private final ITimeAwareGraphNode node;
		private final String indexName;

		public LuceneVersionIterable(Query query, String indexName, ITimeAwareGraphNode n) {
			this.query = query;
			this.indexName = indexName;
			this.node = n;
		}

		/*
		 * TODO should have a method to compose these iterables together through the
		 * queries.
		 */

		@Override
		public Iterator<ITimeAwareGraphNode> iterator() {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
			try {
				final NodeVersionCollector nvc = new NodeVersionCollector(searcher);

				final Query fullQuery = new BooleanQuery.Builder()
					.add(new TermQuery(new Term(INDEX_FIELD, indexName)), Occur.FILTER)
					.add(LongPoint.newExactQuery(NODEID_FIELD, (long)node.getId()), Occur.MUST)
					.add(query, Occur.MUST)
					.build();

				searcher.search(fullQuery, nvc);
				return nvc.getNodeIterator();
			} catch (IOException e) {
				LOGGER.error("Failed to obtain results", e);
				return Collections.emptyIterator();
			}
		}

	}

	private static final String NODEID_FIELD = "h_nodeid";
	private static final String NODETIME_FIELD = "h_nodetime";
	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatLuceneVersionIndexer.class);

	protected class GreycatLuceneVersionIndex implements ITimeAwareGraphNodeVersionIndex {

		private final String name;

		public GreycatLuceneVersionIndex(String name) {
			this.name = name;
		}

		@Override
		public void addVersion(ITimeAwareGraphNode n) {
			Document doc = new Document();

			final String newUUID = UUID.randomUUID().toString();
			doc.add(new StringField(UUID_FIELD, newUUID, Store.NO));
			doc.add(new StringField(INDEX_FIELD, name, Store.NO));
			doc.add(new StringField(DOCTYPE_FIELD, "node", Store.NO));
			doc.add(new LongPoint(NODETIME_FIELD, n.getTime()));
			doc.add(new StoredField(NODETIME_FIELD, n.getTime()));
			doc.add(new LongPoint(NODEID_FIELD, (long) n.getId()));
			doc.add(new StoredField(NODEID_FIELD, (long) n.getId()));

			try {
				lucene.update(new Term(UUID_FIELD, newUUID), null, doc);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		@Override
		public void removeAllVersions(ITimeAwareGraphNode n) {
			Query queryToDelete = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.FILTER)
				.add(LongPoint.newExactQuery(NODEID_FIELD, (long)n.getId()), Occur.MUST)
				.build();

			try { 
				lucene.delete(queryToDelete);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		@Override
		public void removeVersion(ITimeAwareGraphNode n) {
			Query queryToDelete = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.FILTER)
				.add(LongPoint.newExactQuery(NODEID_FIELD, (long)n.getId()), Occur.MUST)
				.add(LongPoint.newExactQuery(NODETIME_FIELD, n.getTime()), Occur.MUST)
				.build();

			try {
				lucene.delete(queryToDelete);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		@Override
		public LuceneVersionIterable getAllVersions(ITimeAwareGraphNode n) {
			final Query queryToFind = LongPoint.newExactQuery(NODEID_FIELD, (long) n.getId());
			return new LuceneVersionIterable(queryToFind, name, n);
		}

		@Override
		public LuceneVersionIterable getVersionsSince(ITimeAwareGraphNode n) {
			final Query queryToFind = LongPoint.newRangeQuery(NODETIME_FIELD, n.getTime(), Long.MAX_VALUE);
			return new LuceneVersionIterable(queryToFind, name, n);
		}

		@Override
		public LuceneVersionIterable getVersionsAfter(ITimeAwareGraphNode n) {
			final Query queryToFind = LongPoint.newRangeQuery(NODETIME_FIELD, n.getTime() + 1, Long.MAX_VALUE);
			return new LuceneVersionIterable(queryToFind, name, n);
		}

		@Override
		public LuceneVersionIterable getVersionsUntil(ITimeAwareGraphNode n) {
			final Query queryToFind = LongPoint.newRangeQuery(NODETIME_FIELD, 0, n.getTime());
			return new LuceneVersionIterable(queryToFind, name, n);
		}

		@Override
		public LuceneVersionIterable getVersionsBefore(ITimeAwareGraphNode n) {
			final Query queryToFind = LongPoint.newRangeQuery(NODETIME_FIELD, 0, n.getTime() - 1);
			return new LuceneVersionIterable(queryToFind, name, n);
		}

		@Override
		public void flush() {
			lucene.flush();
		}

		@Override
		public void delete() {
			GreycatLuceneVersionIndexer.this.deleteIndex(name);
		}
	}

	public GreycatLuceneVersionIndexer(AbstractGreycatDatabase db, File dir) throws IOException {
		super(db, dir);
	}

	@Override
	protected GreycatLuceneVersionIndex createIndexInstance(String name) {
		return new GreycatLuceneVersionIndex(name);
	}

	public void remove(ITimeAwareGraphNode n) {
		final Query queryToDelete = new BooleanQuery.Builder()
			.add(LongPoint.newExactQuery(NODEID_FIELD, (long)n.getId()), Occur.MUST)
			.build();

		try {
			lucene.delete(queryToDelete);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

}