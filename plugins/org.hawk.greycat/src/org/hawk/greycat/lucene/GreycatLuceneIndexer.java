package org.hawk.greycat.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.hawk.core.graph.EmptyIGraphIterable;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.greycat.GreycatDatabase;
import org.hawk.greycat.GreycatNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration between Greycat and Apache Lucene, to allow it to have the type
 * of advanced indexing that we need for Hawk.
 *
 * The standard approach for Lucene is to commit only every so often, in a background
 * thread: commits are extremely expensive!
 *
 * We want to follow the same approach, while being able to react to real time queries
 * easily. To do this, we keep "soft" tx with a rollback log: should a soft rollback be
 * requested, the various operations since the previous soft commit will be undone in
 * reverse order in-memory.
 *
 * We have a background thread that will do a real commit if the rollback log is
 * empty. There is also an explicit commit when this indexer shuts down.
 */
public class GreycatLuceneIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatLuceneNodeIndex.class);

	private static final String ATTRIBUTE_PREFIX = "a_";
	private static final String INDEX_FIELD   = "h_index";
	private static final String DOCTYPE_FIELD = "h_doctype";
	private static final String FIELDS_FIELD = "h_fields";
	private static final String INDEX_DOCTYPE = "indexdecl";
	private static final String NODEID_FIELD   = "h_nodeid";

	/**
	 * Implements a node index as a collection of documents, with a single document
	 * representing the existence of the index itself.
	 */
	protected final class GreycatLuceneNodeIndex implements IGraphNodeIndex {
		private final String name;

		private static final String CMPID_FIELD = "h_cmpid";
		private static final String NODE_DOCTYPE = "node";
		
		protected class NodeListCollector extends ListCollector {
			// TODO: fetch documents on the go (saves memory)

			protected NodeListCollector(IndexSearcher searcher) {
				super(searcher);
			}

			public List<IGraphNode> getNodes() throws IOException {
				List<IGraphNode> result = new ArrayList<>();
				for (Document document : getDocuments()) {
					final String nodeId = document.getField(NODEID_FIELD).stringValue();
					String[] parts = nodeId.split("@", 3);
					final long id = Long.parseLong(parts[2]);
					final GreycatNode gn = database.getNodeById(id);

					result.add(gn);
				}
				return result;
			}
		}

		public GreycatLuceneNodeIndex(String name) {
			this.name = name;
		}

		@Override
		public void remove(IGraphNode n, String key, Object value) {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				final GreycatNode gn = (GreycatNode) n;
				final TermQuery query = findNodeQuery(gn);
				final TopDocs results = searcher.search(query, 1);
				if (results.totalHits > 0) {
					final Document document = searcher.doc(results.scoreDocs[0].doc);

					if (key == null) {
						removeValue(document, gn, value);
					} else {
						removeKeyValue(document, gn, key, value);
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
				lucene.update(findNodeTerm(gn), oldDocument, updated);
			}
		}

		protected void removeValue(final Document oldDocument, final GreycatNode gn, Object value) throws IOException {
			final Document copy = new Document();

			boolean matched = false;
			for (IndexableField field : oldDocument.getFields()) {
				if (field.name().startsWith(ATTRIBUTE_PREFIX)) {
					final String existingValue = field.stringValue();
					if (value == null || existingValue.equals(value)) {
						matched = true;
					} else {
						copyField(field, copy);
					}
				} else {
					copyField(field, copy);
				}
			}

			if (matched) {
				lucene.update(findNodeTerm(gn), oldDocument, copy);
			}
		}

		@Override
		public void remove(IGraphNode n) {
			try {
				lucene.delete(findNodeTerm((GreycatNode) n));
			} catch (IOException e) {
				LOGGER.error(String.format("Could not remove node with id %d from index %s", n.getId(), name), e);
			}
		}

		@Override
		public IGraphIterable<IGraphNode> query(String key, Number from, Number to, boolean fromInclusive, boolean toInclusive) {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				Query query;
				if (from instanceof Float || to instanceof Double) {
					final double dFrom = from.doubleValue(), dTo = to.doubleValue();
					query = DoublePoint.newRangeQuery(ATTRIBUTE_PREFIX + key, fromInclusive ? dFrom : DoublePoint.nextUp(dFrom), toInclusive ? dTo : DoublePoint.nextDown(dTo));
				} else {
					final long lFrom = from.longValue(), lTo = to.longValue();
					query = LongPoint.newRangeQuery(ATTRIBUTE_PREFIX + key, fromInclusive ? lFrom : Math.addExact(lFrom, 1), toInclusive ? lTo : Math.addExact(lTo, -1));
				}

				// Also filter by index
				query = getIndexQueryBuilder().add(query, Occur.MUST).build();

				final NodeListCollector c = new NodeListCollector(searcher);
				searcher.search(query, c);
				return new ListIGraphIterable(c.getNodes());

			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
				return new EmptyIGraphIterable<>();
			}
		}

		@Override
		public IGraphIterable<IGraphNode> query(String key, Object valueExpr) {
			try {
				final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
				final String sValueExpr = valueExpr.toString();

				Query query = null;
				if ("*".equals(key)) {
					if (!"*".equals(valueExpr)) {
						throw new UnsupportedOperationException("*:non-null not implemented yet for query");
					} else {
						// We can just delegate on the query == null case below
					}
				} else if ("*".equals(valueExpr)) {
					query = new TermQuery(new Term(FIELDS_FIELD, key));
				} else if (valueExpr instanceof Float || valueExpr instanceof Double) {
					query = DoublePoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number)valueExpr).doubleValue());
				} else if (valueExpr instanceof Number) {
					query = LongPoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number)valueExpr).longValue());
				} else {
					final int starIdx = sValueExpr.indexOf('*');
					if (starIdx == -1) {
						query = new TermQuery(new Term(ATTRIBUTE_PREFIX + key, sValueExpr));
					} else if (starIdx > 0 && starIdx == sValueExpr.length() - 1) {
						final String prefix = sValueExpr.substring(0, sValueExpr.length() - 1);
						query = new PrefixQuery(new Term(ATTRIBUTE_PREFIX + key, prefix));
					} else {
						query = new WildcardQuery(new Term(ATTRIBUTE_PREFIX + key, sValueExpr));
					}
				}

				if (query == null) {
					query = getIndexQueryBuilder().build();
				} else {
					query = getIndexQueryBuilder().add(query, Occur.MUST).build();
				}

				final NodeListCollector c = new NodeListCollector(searcher);
				searcher.search(query, c);
				return new ListIGraphIterable(c.getNodes());
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
				return new EmptyIGraphIterable<>();
			}
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public IGraphIterable<IGraphNode> get(String key, Object valueExpr) {
			try {
				IndexSearcher searcher = new IndexSearcher(lucene.getReader());

				Query valueQuery;
				if (valueExpr instanceof Float || valueExpr instanceof Double) {
					valueQuery = DoublePoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number)valueExpr).doubleValue());
				} else if (valueExpr instanceof Number) {
					valueQuery = LongPoint.newExactQuery(ATTRIBUTE_PREFIX + key, ((Number)valueExpr).longValue());
				} else {
					final Term term = new Term(ATTRIBUTE_PREFIX + key, valueExpr.toString());
					valueQuery = new TermQuery(term);
				}

				final Query query = getIndexQueryBuilder().add(valueQuery, Occur.MUST).build();
				final NodeListCollector collector = new NodeListCollector(searcher);
				searcher.search(query, collector);
				return new ListIGraphIterable(collector.getNodes());
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
				return new EmptyIGraphIterable<>();
			}
		}

		@Override
		public void flush() {
			lucene.flush();
		}

		@Override
		public void delete() {
			try {
				lucene.delete(new TermQuery(new Term(INDEX_FIELD, name)));
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

				final Document updated = new Document();
				updated.add(new StringField(NODEID_FIELD, getNodeId(gn), Store.YES));
				updated.add(new StringField(CMPID_FIELD, getCompositeId(gn), Store.YES));
				updated.add(new StringField(DOCTYPE_FIELD, NODE_DOCTYPE, Store.YES));
				updated.add(new StringField(INDEX_FIELD, name, Store.YES));

				Document oldDocument = null;
				final TopDocs results = searcher.search(findNodeQuery(gn), 1);
				if (results.totalHits > 0) {
					oldDocument = searcher.doc(results.scoreDocs[0].doc);

					for (IndexableField oldField : oldDocument.getFields()) {
						final String rawOldFieldName = oldField.name();
						if (rawOldFieldName.startsWith(ATTRIBUTE_PREFIX)) {
							final String oldFieldName = rawOldFieldName.substring(ATTRIBUTE_PREFIX.length());
							if (!values.containsKey(oldFieldName)) {
								copyField(oldField, updated);
							}
						}
					}
				}

				for (Entry<String, Object> entry : values.entrySet()) {
					addRawField(updated, ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue());
				}

				lucene.update(findNodeTerm(gn), oldDocument, updated);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		protected TermQuery findNodeQuery(final GreycatNode n) {
			return new TermQuery(findNodeTerm(n));
		}

		protected Term findNodeTerm(final GreycatNode n) {
			return new Term(CMPID_FIELD, getCompositeId(n));
		}

		protected BooleanQuery.Builder getIndexQueryBuilder() {
			return new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.FILTER)
				.add(new TermQuery(new Term(DOCTYPE_FIELD, NODE_DOCTYPE)), Occur.FILTER);
		}

		/**
		 * Returns an identifying string for a specific node at a specific point in time, in a specific index.
		 */
		protected String getCompositeId(GreycatNode gn) {
			return String.format("%s@%d@%d@%d", name, gn.getWorld(), gn.getTime(), gn.getId());
		}
	}


	private final GreycatDatabase database;
	private final SoftTxLucene lucene;

	public GreycatLuceneIndexer(GreycatDatabase db, File dir) throws IOException {
		this.database = db;
		this.lucene = new SoftTxLucene(dir);
	}

	public IGraphNodeIndex getIndex(String name) {
		// Make sure the index is listed
		try {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());

			TopDocs scoreDocs = searcher.search(new TermQuery(new Term(INDEX_FIELD, name)), 1);
			if (scoreDocs.totalHits == 0) {
				Document doc = new Document();
				doc.add(new StringField(INDEX_FIELD, name, Store.YES));
				doc.add(new StringField(DOCTYPE_FIELD, INDEX_DOCTYPE, Store.YES));
				lucene.update(new Term(INDEX_FIELD, name), null, doc);
			}
		}  catch (IOException e) {
			LOGGER.error("Could not register index", e);
		}

		return new GreycatLuceneNodeIndex(name);
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
			
			TopDocs results = searcher.search(query, 1);
			return results.totalHits > 0;
		} catch (IOException e) {
			LOGGER.error(String.format("Could not check if %s exists", name), e);
			return false;
		}
	}

	/**
	 * Removes this node from all indices.
	 */
	public void remove(GreycatNode gn) {
		try {
			final String nodeId = getNodeId(gn);
			lucene.delete(new TermQuery(new Term(NODEID_FIELD, nodeId)));
		} catch (IOException e) {
			LOGGER.error(String.format(
				"Could not remove node %s in world %d at time %d",
				gn.getId(), gn.getWorld(), gn.getTime()), e);
		}
	}

	/**
	 * Returns an identifying string for a specific node at a specific point in time.
	 */
	protected String getNodeId(GreycatNode gn) {
		return String.format("%d@%d@%d", gn.getWorld(), gn.getTime(), gn.getId());
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
	
}
