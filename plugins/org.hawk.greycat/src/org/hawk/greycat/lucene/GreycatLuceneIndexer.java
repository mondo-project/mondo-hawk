/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.greycat.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.WildcardQuery;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeIndex;
import org.hawk.greycat.AbstractGreycatDatabase;
import org.hawk.greycat.GreycatNode;
import org.hawk.greycat.lucene.IntervalCollector.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Integration between Greycat and Apache Lucene, to allow it to have the type
 * of advanced indexing that we need for Hawk.</p>
 *
 * <p>The standard approach for Lucene is to commit only every so often, in a background
 * thread: commits are extremely expensive!</p>
 *
 * <p>We want to follow the same approach, while being able to react to real time queries
 * easily. To do this, we keep "soft" tx with a rollback log: should a soft rollback be
 * requested, the various operations since the previous soft commit will be undone in
 * reverse order in-memory.</p>
 *
 * <p>We have a background thread that will do a real commit if the rollback log is
 * empty. There is also an explicit commit when this indexer shuts down.</p>
 *
 * <p>TODO: add support for multiple worlds to this index. This may require keeping track
 * of how worlds branch off from each other.</p>
 */
public class GreycatLuceneIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatLuceneNodeIndex.class);

	private static final String ATTRIBUTE_PREFIX = "a_";
	private static final String UUID_FIELD = "h_id";
	private static final String INDEX_FIELD   = "h_index";
	private static final String DOCTYPE_FIELD = "h_doctype";
	private static final String FIELDS_FIELD = "h_fields";
	private static final String INDEX_DOCTYPE = "indexdecl";

	/** Node ID, given by Greycat. */
	private static final String NODEID_FIELD   = "h_nodeid";

	/**
	 * Timepoint from which this index entry is valid. This is set to the timepoint
	 * of the node being indexed.
	 */
	private static final String VALIDFROM_FIELD = "h_from";

	/**
	 * Timepoint up to which (itself included) this index entry is valid. This is
	 * initially set to {@link Long#MAX_VALUE}, but it may be reduced if the index
	 * entry is overridden or removed later.
	 */
	private static final String VALIDTO_FIELD = "h_to";

	protected class MatchExistsCollector extends SimpleCollector {
		private boolean matchFound = false;

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public void collect(int doc) throws IOException {
			matchFound = true;
			throw new CollectionTerminatedException();
		}

		public boolean isMatchFound() {
			return matchFound;
		}
	}

	protected final class NodeListCollector extends ListCollector {
		private final Long timepoint;

		protected NodeListCollector(IndexSearcher searcher, Long timepoint) {
			super(searcher);
			this.timepoint = timepoint;
		}
		
		public Iterator<GreycatNode> getNodeIterator() {
			final Iterator<Integer> itIdentifiers = docIds.iterator();
			return new Iterator<GreycatNode>() {
				@Override
				public boolean hasNext() {
					return itIdentifiers.hasNext();
				}

				@Override
				public GreycatNode next() {
					int docId = itIdentifiers.next();
					try {
						final Document document = searcher.doc(docId);
						final GreycatNode node = getNodeByDocument(document);
						return timepoint == null ? node : node.travelInTime(timepoint);
					} catch (IOException e) {
						LOGGER.error("Could not retrieve document with ID " + docId, e);
						throw new NoSuchElementException();
					}
				}
			};
		}
	}

	protected final class LuceneGraphIterable implements IGraphIterable<GreycatNode> {
		private final Query query;
		private final Long timepoint;

		protected LuceneGraphIterable(Query query, Long timepoint) {
			this.query = query;
			this.timepoint = timepoint;
		}

		@Override
		public Iterator<GreycatNode> iterator() {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
				final NodeListCollector lc = new NodeListCollector(searcher, timepoint);
				searcher.search(query, lc);
				return lc.getNodeIterator();
			} catch (IOException e) {
				LOGGER.error("Failed to obtain result", e);
				return Collections.emptyIterator();
			}
		}

		@Override
		public int size() {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
				final TotalHitCountCollector collector = new TotalHitCountCollector();
				searcher.search(query, collector);
				return collector.getTotalHits();
			} catch (IOException e) {
				LOGGER.error("Failed to obtain size", e);
				return 0;
			}
		}

		@Override
		public GreycatNode getSingle() {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
				TopDocs results = searcher.search(query, 1);
				if (results.totalHits > 0) {
					final Document document = searcher.doc(results.scoreDocs[0].doc);
					final GreycatNode node = getNodeByDocument(document);
					if (timepoint == null) {
						return node;
					} else {
						return node.travelInTime(timepoint);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed to obtain single result", e);
			}

			throw new NoSuchElementException();
		}
	}
	
	/**
	 * Implements a node index as a collection of documents, with a single document
	 * representing the existence of the index itself. The default timepoint can be
	 * optionally specified upon creation: if not set, we will refer to the
	 * database's current time.
	 */
	public final class GreycatLuceneNodeIndex implements ITimeAwareGraphNodeIndex {
		private final String name;

		/**
		 * Timepoint which may override the current graph time, if not <code>null</code>.
		 * If <code>null</code> (the default), the current graph timepoint will be used.
		 */
		private final Long timepoint;

		private static final String NODE_DOCTYPE = "node";
		
		public GreycatLuceneNodeIndex(String name) {
			this(name, null);
		}

		public GreycatLuceneNodeIndex(String name, Long timepoint) {
			this.name = name;
			this.timepoint = timepoint;
		}

		@Override
		public void remove(IGraphNode n, String key, Object value) {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
				final GreycatNode gn = (GreycatNode) n;

				/*
				 * All documents from this point in time in the index need to be revised. One
				 * may be still valid, others just need to have the future value removed.
				 *
				 * If both key and value are present, we can add those to the query to reduce
				 * the number of documents to be changed.
				 */
				final Builder queryBuilder = getIndexQueryBuilder()
					.add(LongPoint.newExactQuery(NODEID_FIELD, gn.getId()), Occur.MUST)
					.add(LongPoint.newRangeQuery(VALIDTO_FIELD, gn.getTime(), Long.MAX_VALUE), Occur.MUST);
				if (key != null && value != null) {
					queryBuilder.add(getValueQuery(key, value), Occur.MUST);
				}
				final Query query = queryBuilder.build();
				final ListCollector collector = new ListCollector(searcher);
				searcher.search(query, collector);

				if (key == null) {
					for (Document doc : collector.getDocuments()) {
						removeValue(doc, gn, value);
					}
				} else {
					for (Document doc : collector.getDocuments()) {
						removeKeyValue(doc, gn, key, value);
					}
				}

			} catch (IOException e) {
				LOGGER.error("Could not remove node from index", e);
			}
		}

		protected void removeKeyValue(final Document oldDocument, final GreycatNode gn, String key, Object value) throws IOException {
			final Document updated = new Document();

			// Copy all other fields as we go
			boolean anyMatched = false;
			for (IndexableField field : oldDocument.getFields()) {
				boolean matched = false;

				if (field.name().equals(ATTRIBUTE_PREFIX + key)) {
					if (value == null) {
						matched = true;
					} else if (value instanceof Float) {
						final float fValue = field.numericValue() == null ? Float.valueOf(field.stringValue()) : field.numericValue().floatValue();
						matched = fValue == ((float) value);
					} else if (value instanceof Double) {
						final double fValue = field.numericValue() == null ? Double.valueOf(field.stringValue()) : field.numericValue().doubleValue();
						matched = fValue == ((double) value);
					} else if (value instanceof Number) {
						final Long fValue = field.numericValue() == null ? Long.valueOf(field.stringValue()) : field.numericValue().longValue();
						matched = ((Number) value).longValue() == fValue;
					} else if (value.equals(field.stringValue())) {
						matched = true;
					}
				}

				if (!matched) {
					copyField(field, updated);
				}
				
				anyMatched = anyMatched || matched;
			}

			if (anyMatched) {
				replaceDocumentAtTimepoint(gn, oldDocument, updated);
			}
		}

		private void replaceDocumentAtTimepoint(final GreycatNode gn, final Document oldDocument, final Document newDocument) throws IOException {
			assert oldDocument != null : "Old document should not be null";
			assert newDocument != null : "New document should not be null";
			assert newDocument.getField(VALIDFROM_FIELD) != null : "New document should have a starting point";
			assert newDocument.getField(VALIDTO_FIELD) != null : "New document should have an ending point";
			assert oldDocument.getField(UUID_FIELD).stringValue().equals(newDocument.getField(UUID_FIELD).stringValue()) : "Both documents should have same UUID";

			final long lOldFrom = oldDocument.getField(VALIDFROM_FIELD).numericValue().longValue();
			final long lOldTo = oldDocument.getField(VALIDTO_FIELD).numericValue().longValue();

			// Is the old document currently in effect? If so, we need to shorten its lifespan.
			if (gn.getTime() >= lOldFrom && gn.getTime() <= lOldTo) {
				// the old document is currently in effect, we have to shorten its lifespan
				final long lOldNewTo = gn.getTime() - 1;
				if (lOldNewTo < lOldFrom) {
					// old document would not have a lifespan - just replace
					lucene.update(new Term(UUID_FIELD, oldDocument.get(UUID_FIELD)), oldDocument, newDocument);
				} else {
					// shorten lifespan of old document, generate new UUID
					final Document shortenedDoc = copy(oldDocument);
					replaceRawField(shortenedDoc, VALIDTO_FIELD, lOldNewTo);
					lucene.update(new Term(UUID_FIELD, oldDocument.get(UUID_FIELD)), oldDocument, shortenedDoc);

					// generate new UUID for the other document and set starting timepoint
					final String newUUID = UUID.randomUUID().toString();
					replaceRawField(newDocument, UUID_FIELD, newUUID);
					replaceRawField(newDocument, VALIDFROM_FIELD, gn.getTime());
					lucene.update(new Term(UUID_FIELD, newUUID), null, newDocument);
				}
			} else {
				// the old document is not in effect - just replace the values there
				lucene.update(new Term(UUID_FIELD), oldDocument, newDocument);
			}
		}

		protected void removeValue(final Document oldDocument, final GreycatNode gn, Object value) throws IOException {
			final Document updated = new Document();

			boolean matched = false;
			for (IndexableField field : oldDocument.getFields()) {
				if (field.name().startsWith(ATTRIBUTE_PREFIX)) {
					final String existingValue = field.stringValue();
					if (value == null || existingValue.equals(value)) {
						matched = true;
					} else {
						copyField(field, updated);
					}
				} else {
					copyField(field, updated);
				}
			}

			if (matched) {
				replaceDocumentAtTimepoint(gn, oldDocument, updated);
			}
		}

		@Override
		public void remove(IGraphNode n) {
			try {
				final GreycatNode gn = (GreycatNode) n;

				// All documents for this node starting in the future must be deleted
				final Query queryToDelete = getIndexQueryBuilder()
					.add(findNodeQuery(gn), Occur.MUST)
					.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, gn.getTime() + 1, Long.MAX_VALUE), Occur.MUST)
					.build();
				lucene.delete(queryToDelete);

				// Currently valid documents must be invalidated from this timepoint
				final Query queryToInvalidate = getIndexQueryBuilder()
					.add(findValidNodeDocuments(gn), Occur.MUST)
					.build();
				invalidateAtTimepoint(gn, queryToInvalidate);
			} catch (IOException e) {
				LOGGER.error(String.format("Could not remove node with id %d from index %s", n.getId(), name), e);
			}
		}

		@Override
		public IGraphIterable<GreycatNode> query(String key, Number from, Number to, boolean fromInclusive, boolean toInclusive) {
				Query query;
				if (from instanceof Float || to instanceof Double) {
					final double dFrom = from.doubleValue(), dTo = to.doubleValue();
					query = DoublePoint.newRangeQuery(ATTRIBUTE_PREFIX + key, fromInclusive ? dFrom : DoublePoint.nextUp(dFrom), toInclusive ? dTo : DoublePoint.nextDown(dTo));
				} else {
					final long lFrom = from.longValue(), lTo = to.longValue();
					query = LongPoint.newRangeQuery(ATTRIBUTE_PREFIX + key, fromInclusive ? lFrom : Math.addExact(lFrom, 1), toInclusive ? lTo : Math.addExact(lTo, -1));
				}

				// Also filter by index and timepoint (using database for now)
				query = getIndexQueryBuilder()
					.add(query, Occur.MUST)
					.add(findValidDocumentsAtTimepoint(getTimepoint()), Occur.MUST)
					.build();

				return new LuceneGraphIterable(query, timepoint);
		}

		@Override
		public IGraphIterable<GreycatNode> query(String key, Object valueExpr) {
			final String sValueExpr = valueExpr.toString();

			Query valueQuery = null;
			if ("*".equals(key)) {
				if (!"*".equals(valueExpr)) {
					throw new UnsupportedOperationException("*:non-null not implemented yet for query");
				} else {
					// We can just delegate on the query == null case below
				}
			} else if ("*".equals(valueExpr)) {
				valueQuery = new TermQuery(new Term(FIELDS_FIELD, key));
			} else if (valueExpr instanceof Float || valueExpr instanceof Double) {
				valueQuery = DoublePoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number) valueExpr).doubleValue());
			} else if (valueExpr instanceof Number) {
				valueQuery = LongPoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number) valueExpr).longValue());
			} else {
				final int starIdx = sValueExpr.indexOf('*');
				if (starIdx == -1) {
					valueQuery = new TermQuery(new Term(ATTRIBUTE_PREFIX + key, sValueExpr));
				} else if (starIdx > 0 && starIdx == sValueExpr.length() - 1) {
					final String prefix = sValueExpr.substring(0, sValueExpr.length() - 1);
					valueQuery = new PrefixQuery(new Term(ATTRIBUTE_PREFIX + key, prefix));
				} else {
					valueQuery = new WildcardQuery(new Term(ATTRIBUTE_PREFIX + key, sValueExpr));
				}
			}

			final Builder builder = getIndexQueryBuilder()
				.add(findValidDocumentsAtTimepoint(getTimepoint()), Occur.MUST);
			if (valueQuery != null) {
				builder.add(valueQuery, Occur.MUST);
			}
			final Query query = builder.build();

			return new LuceneGraphIterable(query, timepoint);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public IGraphIterable<GreycatNode> get(String key, Object valueExpr) {
			final Query valueQuery = getValueQuery(key, valueExpr);
			final Query query = getIndexQueryBuilder()
				.add(valueQuery, Occur.MUST)
				.add(findValidDocumentsAtTimepoint(getTimepoint()), Occur.MUST)
				.build();
			return new LuceneGraphIterable(query, timepoint);
		}

		@Override
		public List<Long> getVersions(ITimeAwareGraphNode gn, String key, Object valueExpr) {
			final Query valueQuery = getValueQuery(key, valueExpr);
			final Query query = getIndexQueryBuilder()
				.add(valueQuery, Occur.MUST)
				.add(LongPoint.newExactQuery(NODEID_FIELD, (long) gn.getId()), Occur.MUST)
				.build();

			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				/*
				 * The index is structured in intervals, NOT in specific timepoints - this means
				 * that a specific interval may contain multiple timepoints. We can ask the node
				 * for those timepoints - this also ensures composability in whenAnnotated('...')
				 * operations.
				 */
				final IntervalCollector<Long> fc = new IntervalCollector<>(
					searcher, VALIDFROM_FIELD, VALIDTO_FIELD, f -> f.numericValue().longValue()
				);
				searcher.search(query, fc);

				final List<Interval<Long>> intervals = fc.getValues();
				Collections.sort(intervals, (a, b) -> -Long.compare(a.getFrom(), b.getFrom()));

				final List<Long> timepoints = new ArrayList<>();
				for (Interval<Long> interval : intervals) {
					List<Long> intervalTimepoints = gn.getInstantsBetween(interval.getFrom(), interval.getTo());
					timepoints.addAll(intervalTimepoints);
				}

				return timepoints;
			} catch (IOException e) {
				LOGGER.error("Failed to obtain result", e);
				return Collections.emptyList();
			}
		}

		@Override
		public Long getFirstVersionSince(ITimeAwareGraphNode gn, String key, Object valueExpr) {
			final Query valueQuery = getValueQuery(key, valueExpr);
			final Query query = getIndexQueryBuilder()
				.add(valueQuery, Occur.MUST)
				.add(LongPoint.newExactQuery(NODEID_FIELD, (long) gn.getId()), Occur.MUST)
				.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, gn.getTime(), Long.MAX_VALUE), Occur.MUST)
				.build();

			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				final Sort sort = new Sort(new SortedNumericSortField(VALIDFROM_FIELD, SortField.Type.LONG));
				final ScoreDoc[] hits = searcher.search(query, 1, sort).scoreDocs;
				if (hits.length == 0) {
					return null;
				}

				final Document doc = searcher.doc(hits[0].doc);
				final long from = doc.getField(VALIDFROM_FIELD).numericValue().longValue();
				final long to = doc.getField(VALIDFROM_FIELD).numericValue().longValue();
				final List<Long> versions = gn.getInstantsBetween(from, to);
				if (versions.isEmpty()) {
					return null;
				}

				return versions.get(versions.size() - 1);
			} catch (IOException e) {
				LOGGER.error("Failed to obtain result", e);
				return null;
			}
		}

		private Query getValueQuery(String key, Object valueExpr) {
			Query valueQuery;
			if (valueExpr instanceof Float || valueExpr instanceof Double) {
				valueQuery = DoublePoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number) valueExpr).doubleValue());
			} else if (valueExpr instanceof Number) {
				valueQuery = LongPoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number) valueExpr).longValue());
			} else {
				final Term term = new Term(ATTRIBUTE_PREFIX + key, valueExpr.toString());
				valueQuery = new TermQuery(term);
			}
			return valueQuery;
		}

		@Override
		public void flush() {
			lucene.flush();
		}

		@Override
		public void delete() {
			// This operation is NOT time-aware: it will drop the entire index in one go.
			try {
				lucene.delete(new TermQuery(new Term(INDEX_FIELD, name)));
				nodeIndexCache.invalidate(name);
			} catch (IOException e) {
				LOGGER.error("Could not delete index " + name, e);
			}
		}

		@Override
		public void add(IGraphNode n, String key, Object value) {
			if (value != null) {
				add(n, Collections.singletonMap(key, value));
			}
		}

		@Override
		public void add(IGraphNode n, Map<String, Object> values) {
			if (values == null) {
				return;
			}
			final GreycatNode gn = (GreycatNode)n;

			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				// We want to find the currently valid document for this node and update it
				final Query latestVersionQuery = getIndexQueryBuilder()
					.add(findNodeQuery(gn), Occur.MUST)
					.add(findValidDocumentsAtTimepoint(gn.getTime()), Occur.MUST)
					.build();

				final TopDocs results = searcher.search(latestVersionQuery, 1);
				long validTo;
				if (results.totalHits > 0) {
					validTo = extendCurrentDocument(gn, values, searcher, results);
				} else {
					validTo = addNewDocument(gn, values, searcher);
				}

				// If this document does not last forever, we need to update future documents too.
				// No need to manipulate lifespans in this case.
				if (validTo < Long.MAX_VALUE) {
					extendFutureDocuments(gn, values, searcher, validTo);
				}
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		private void extendFutureDocuments(final GreycatNode gn, Map<String, Object> values,
				final IndexSearcher searcher, long validTo) throws IOException {
			final Query allFutureQuery = getIndexQueryBuilder()
				.add(findNodeQuery(gn), Occur.MUST)
				.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, validTo + 1, Long.MAX_VALUE), Occur.MUST)
				.build();

			final ListCollector lc = new ListCollector(searcher);
			searcher.search(allFutureQuery, lc);
			for (final Document doc : lc.getDocuments()) {
				 final Document updatedFuture = copy(doc);
				 addAttributes(updatedFuture, values);

				 final String uuid = updatedFuture.getField(UUID_FIELD).stringValue();
				 lucene.update(new Term(UUID_FIELD, uuid), doc, updatedFuture);
			}
		}

		private long addNewDocument(final GreycatNode gn, Map<String, Object> values, final IndexSearcher searcher) throws IOException {
			final String uuid = UUID.randomUUID().toString();
			final Document newDocument = new Document();
			addRawField(newDocument, NODEID_FIELD, gn.getId());
			addRawField(newDocument, DOCTYPE_FIELD, NODE_DOCTYPE);
			addRawField(newDocument, INDEX_FIELD, name);
			addRawField(newDocument, UUID_FIELD, uuid);

			// 'valid from' is easy - from now onwards
			addRawField(newDocument, VALIDFROM_FIELD, gn.getTime());

			// 'valid to' depends on future entries - need to compute!
			final long validTo = computeValidToForNewDocument(gn, searcher);
			addRawField(newDocument, VALIDTO_FIELD, validTo);

			addAttributes(newDocument, values);
			lucene.update(new Term(UUID_FIELD, uuid), null, newDocument);

			return validTo;
		}

		private long extendCurrentDocument(final GreycatNode gn, Map<String, Object> values,
				final IndexSearcher searcher, final TopDocs results) throws IOException {
			final Document oldDocument = searcher.doc(results.scoreDocs[0].doc);

			final Document updatedDocument = new Document();
			addRawField(updatedDocument, NODEID_FIELD, gn.getId());
			addRawField(updatedDocument, DOCTYPE_FIELD, NODE_DOCTYPE);
			addRawField(updatedDocument, INDEX_FIELD, name);
			addRawField(updatedDocument, VALIDFROM_FIELD, gn.getTime());
			
			for (IndexableField oldField : oldDocument.getFields()) {
				final String rawOldFieldName = oldField.name();
				if (rawOldFieldName.startsWith(ATTRIBUTE_PREFIX)) {
					final String oldFieldName = rawOldFieldName.substring(ATTRIBUTE_PREFIX.length());
					if (!values.containsKey(oldFieldName)) {
						copyField(oldField, updatedDocument);
					}
				} else if (rawOldFieldName.equals(UUID_FIELD) || rawOldFieldName.equals(VALIDTO_FIELD)) {
					copyField(oldField, updatedDocument);
				}
			}
			addAttributes(updatedDocument, values);
			replaceDocumentAtTimepoint(gn, oldDocument, updatedDocument);
			final long validTo = updatedDocument.getField(VALIDTO_FIELD).numericValue().longValue();

			return validTo;
		}

		private long computeValidToForNewDocument(final GreycatNode gn, final IndexSearcher searcher) throws IOException {
			final Query afterStartQuery = getIndexQueryBuilder()
				.add(findNodeQuery(gn), Occur.MUST)
				.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, gn.getTime() + 1, Long.MAX_VALUE), Occur.MUST)
				.build();

			final ListCollector lc = new ListCollector(searcher);
			searcher.search(afterStartQuery, lc);
			Long minFrom = null;
			for (Document dAfterStart : lc.getDocuments()) {
				final long from = dAfterStart.getField(VALIDFROM_FIELD).numericValue().longValue();
				if (minFrom == null) {
					minFrom = from;
				} else {
					minFrom = Math.min(from, minFrom);
				}
			}

			return minFrom == null ? Long.MAX_VALUE : minFrom - 1;
		}

		@Override
		public GreycatLuceneNodeIndex travelInTime(long timepoint) {
			return new GreycatLuceneNodeIndex(name, timepoint);
		}

		private long getTimepoint() {
			if (timepoint == null) {
				return database.getTime();
			} else {
				return timepoint;
			}
		}

		protected Query findNodeQuery(final GreycatNode n) {
			return LongPoint.newExactQuery(NODEID_FIELD, n.getId());
		}

		protected BooleanQuery.Builder getIndexQueryBuilder() {
			return new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.FILTER)
				.add(new TermQuery(new Term(DOCTYPE_FIELD, NODE_DOCTYPE)), Occur.FILTER);
		}

	}


	private final AbstractGreycatDatabase database;
	private final Cache<String, GreycatLuceneNodeIndex> nodeIndexCache =
		CacheBuilder.newBuilder().maximumSize(100).build();
	private final SoftTxLucene lucene;

	public GreycatLuceneIndexer(AbstractGreycatDatabase db, File dir) throws IOException {
		this.database = db;
		this.lucene = new SoftTxLucene(dir);
	}

	public GreycatLuceneNodeIndex getIndex(String name) throws Exception {
		return nodeIndexCache.get(name, () -> {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

			final Query query = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.MUST)
				.add(new TermQuery(new Term(DOCTYPE_FIELD, INDEX_DOCTYPE)), Occur.MUST)
				.build();

			final TotalHitCountCollector thc = new TotalHitCountCollector();
			searcher.search(query, thc);
			if (thc.getTotalHits() == 0) {
				Document doc = new Document();
				doc.add(new StringField(INDEX_FIELD, name, Store.YES));
				doc.add(new StringField(DOCTYPE_FIELD, INDEX_DOCTYPE, Store.YES));
				lucene.update(new Term(INDEX_FIELD, name), null, doc);
			}

			return new GreycatLuceneNodeIndex(name);
		});
	}

	public Set<String> getIndexNames() {
		try {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
			final ListCollector lc = new ListCollector(searcher);
			searcher.search(new TermQuery(new Term(DOCTYPE_FIELD, INDEX_DOCTYPE)), lc);

			final Set<String> names = new HashSet<>();
			for (Document doc : lc.getDocuments()) {
				names.add(doc.getField(INDEX_FIELD).stringValue());
			}
			return names;
		} catch (IOException e) {
			LOGGER.error("Could not list index name", e.getMessage());
			return Collections.emptySet();
		} 
	}

	public boolean indexExists(String name) {
		try {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

			Query query = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(DOCTYPE_FIELD, INDEX_DOCTYPE)), Occur.FILTER)
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.MUST)
				.build();

			final TotalHitCountCollector collector = new TotalHitCountCollector();
			searcher.search(query, collector);
			return collector.getTotalHits() > 0;
		} catch (IOException e) {
			LOGGER.error(String.format("Could not check if %s exists", name), e);
			return false;
		}
	}

	/**
	 * Removes this node from all indices, from the timepoint of the node onwards.
	 */
	public void remove(GreycatNode gn) {
		try {
			// To be removed - all documents on the node valid after this timepoint
			final Query queryToDelete = new BooleanQuery.Builder()
				.add(LongPoint.newExactQuery(NODEID_FIELD, gn.getId()), Occur.MUST)
				.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, gn.getTime() + 1, Long.MAX_VALUE), Occur.MUST)
				.build();
			lucene.delete(queryToDelete);

			// To be updated - all documents valid up to now
			final Query queryToRevise = findValidNodeDocuments(gn);
			invalidateAtTimepoint(gn, queryToRevise);
		} catch (IOException e) {
			LOGGER.error(String.format(
				"Could not remove node %s in world %d from time %d onwards",
				gn.getId(), gn.getWorld(), gn.getTime()
			), e);
		}
	}

	/**
	 * Commits all changes to the index. This is a soft-commit: real Lucene
	 * commits are only done periodically in the background, when the rollback
	 * log is empty.
	 * 
	 * @throws IOException
	 *             Failed to commit the changes.
	 */
	public void commit() throws IOException {
		lucene.commit();
	}

	/**
	 * Rolls back all changes to the index. This is a soft-rollback: changes that
	 * have not been committed yet are undone in memory.
	 *
	 * @throws IOException
	 *             Failed to roll back the changes.
	 */
	public void rollback() throws IOException {
		lucene.rollback();
	}

	/**
	 * Commits all pending changes and shuts down Lucene.
	 */
	public void shutdown() {
		lucene.shutdown();
	}

	protected static void replaceRawField(Document document, final String fieldName, final Object value) {
		document.removeFields(fieldName);
		addRawField(document, fieldName, value);
	}

	protected static void addAttributes(final Document updated, Map<String, Object> values) {
		for (Entry<String, Object> entry : values.entrySet()) {
			final String attributeFieldName = ATTRIBUTE_PREFIX + entry.getKey();
			addRawField(updated, attributeFieldName, entry.getValue());
		}
	}
	
	protected static void addRawField(Document document, final String fieldName, final Object value) {
		/*
		 * Point classes are very useful for fast range queries, but they do not store
		 * the value in the document. We need to add a StoredField so we can use the
		 * full version of remove (key, value and node).
		 *
		 * TODO: do we get these back after a soft rollback? We need tests for this.
		 */
		if (value instanceof Float || value instanceof Double) {
			final double doubleValue = ((Number)value).doubleValue();
			document.add(new DoublePoint(fieldName, doubleValue));
			document.add(new StoredField(fieldName, doubleValue));
		} else if (value instanceof Number) {
			final long longValue = ((Number)value).longValue();
			document.add(new NumericDocValuesField(fieldName, longValue));
			document.add(new LongPoint(fieldName, longValue));
			document.add(new StoredField(fieldName, longValue));
		} else {
			document.add(new StringField(fieldName, value.toString(), Store.YES));
		}

		if (fieldName.startsWith(ATTRIBUTE_PREFIX)) {
			document.add(new StringField(FIELDS_FIELD, fieldName.substring(ATTRIBUTE_PREFIX.length()), Store.YES));
		}
	}

	/**
	 * Copies and recreates an entire document, including IntPoint and DoublePoint fields.
	 */
	protected static Document copy(Document doc) {
		if (doc == null) {
			return null;
		}

		final Document newDoc = new Document();
		for (IndexableField f : doc.getFields()) {
			copyField(f, newDoc);
		}

		return newDoc;
	}

	/**
	 * Copies an existing field into a document, as long as it is not the `meta`
	 * {@link #FIELDS_FIELD} that is used to indicate that an attribute has been
	 * set.
	 */
	protected static void copyField(IndexableField field, final Document copy) {
		if (!FIELDS_FIELD.equals(field.name())) {
			if (field.numericValue() instanceof Number) {
				addRawField(copy, field.name(), field.numericValue());
			} else {
				addRawField(copy, field.name(), field.stringValue());
			}
		}
	}

	protected GreycatNode getNodeByDocument(Document document) {
		final long id = document.getField(NODEID_FIELD).numericValue().longValue();
		final GreycatNode gn = database.getNodeById(id);
		return gn;
	}

	protected Query findValidNodeDocuments(GreycatNode gn) {
		return new BooleanQuery.Builder()
			.add(LongPoint.newExactQuery(NODEID_FIELD, gn.getId()), Occur.MUST)
			.add(findValidDocumentsAtTimepoint(gn.getTime()), Occur.MUST)
			.build();
	}

	protected Query findValidDocumentsAtTimepoint(final long time) {
		return new BooleanQuery.Builder()
			.add(LongPoint.newRangeQuery(VALIDFROM_FIELD, Long.MIN_VALUE, time), Occur.MUST)
			.add(LongPoint.newRangeQuery(VALIDTO_FIELD, time, Long.MAX_VALUE), Occur.MUST)
			.build();
	}

	protected void invalidateAtTimepoint(GreycatNode gn, final Query queryToRevise) throws IOException {
		final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
		final ListCollector lc = new ListCollector(searcher);
		searcher.search(queryToRevise, lc);
		for (Document doc : lc.getDocuments()) {
			invalidateAtTimepoint(gn, doc);
		}
	}

	protected void invalidateAtTimepoint(GreycatNode gn, Document doc) throws IOException {
		final long lFrom = doc.getField(VALIDFROM_FIELD).numericValue().longValue();

		if (lFrom == gn.getTime()) {
			// Document was only valid at this very timepoint: simply delete
			lucene.delete(new Term(UUID_FIELD, doc.get(UUID_FIELD)));
		} else {
			// Document was valid before this timepoint: shorten lifespan
			Document revisedDoc = copy(doc);
			replaceRawField(revisedDoc, VALIDTO_FIELD, gn.getTime() - 1);
			lucene.update(new Term(UUID_FIELD, doc.get(UUID_FIELD)), doc, revisedDoc);
		}
	}
}
