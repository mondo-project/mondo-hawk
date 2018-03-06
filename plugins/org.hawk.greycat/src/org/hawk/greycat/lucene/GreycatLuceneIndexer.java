package org.hawk.greycat.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
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
 */
public class GreycatLuceneIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatLuceneNodeIndex.class);

	private static final String ATTRIBUTE_PREFIX = "a_";
	private static final String INDEX_FIELD   = "h_index";
	private static final String DOCTYPE_FIELD = "h_doctype";
	private static final String FIELDS_FIELD = "h_fields";
	private static final String INDEX_DOCTYPE = "indexdecl";
	private static final String NODEID_FIELD   = "h_nodeid";

	protected static final class ListIGraphIterable implements IGraphIterable<IGraphNode> {
		private final List<IGraphNode> nodes;

		protected ListIGraphIterable(List<IGraphNode> nodes) {
			this.nodes = nodes;
		}

		@Override
		public Iterator<IGraphNode> iterator() {
			return nodes.iterator();
		}

		@Override
		public int size() {
			return nodes.size();
		}

		@Override
		public IGraphNode getSingle() {
			return nodes.iterator().next();
		}
	}

	protected class ListCollector extends SimpleCollector {
		private final List<Integer> docIds = new ArrayList<>();
		private final IndexSearcher searcher;
		private int docBase;

		protected ListCollector(IndexSearcher searcher) {
			this.searcher = searcher;
		}

		@Override
		protected void doSetNextReader(LeafReaderContext context) throws IOException {
			this.docBase = context.docBase;
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public void collect(int doc) {
			docIds.add(docBase + doc);
		}

		public List<Document> getDocuments() throws IOException {
			List<Document> result = new ArrayList<>();
			for (int docId : docIds) {
				final Document document = searcher.doc(docId);
				result.add(document);
			}
			return result;
		}
	}

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
		public void remove(String key, Object value, IGraphNode n) {
			try {
				final IndexSearcher searcher = new IndexSearcher(reader);

				final GreycatNode gn = (GreycatNode) n;
				final TopDocs results = searcher.search(findNodeQuery(gn), 1);
				if (results.totalHits > 0) {
					final Document document = searcher.doc(results.scoreDocs[0].doc);

					if (key == null) {
						removeValue(writer, document, gn, value);
					} else {
						removeKeyValue(writer, document, gn, key, value);
					}
				}

				refreshReader();
			} catch (IOException e) {
				LOGGER.error("Could not remove node from index", e);
			}
		}

		protected void removeKeyValue(IndexWriter writer, final Document document, final GreycatNode gn, String key, Object value) throws IOException {
			final List<IndexableField> remainingFields = new ArrayList<>();
			boolean matched = false;
			for (IndexableField field : document.getFields(ATTRIBUTE_PREFIX + key)) {
				if (value == null) {
					matched = true;
				} else if (value instanceof Float || value instanceof Double) {
					// WARN: not happy about == for equality with floating-point values... 
					matched = matched || ((Number)value).doubleValue() == field.numericValue().doubleValue();
				} else if (value instanceof Number) {
					matched = matched || ((Number)value).longValue() == field.numericValue().longValue();
				} else if (value.equals(field.stringValue())) {
					matched = true;
				} else {
					remainingFields.add(field);
				}
			}

			if (matched) {
				document.removeFields(ATTRIBUTE_PREFIX + key);
				for (IndexableField field : remainingFields) {
					document.add(field);
				}
				writer.updateDocument(findNodeTerm(gn), document);
			}
		}

		protected void removeValue(IndexWriter writer, final Document document, final GreycatNode gn, Object value) throws IOException {
			final Document copy = new Document();

			boolean matched = false;
			for (IndexableField field : document.getFields()) {
				if (field.name().startsWith(ATTRIBUTE_PREFIX)) {
					final String existingValue = field.stringValue();
					if (value == null || existingValue.equals(value)) {
						matched = true;
					} else {
						copy.add(field);
					}
				} else {
					copy.add(field);
				}
			}

			if (matched) {
				writer.updateDocument(findNodeTerm(gn), copy);
			}
		}

		@Override
		public void remove(IGraphNode n) {
			try {
				writer.deleteDocuments(findNodeTerm((GreycatNode) n));
			} catch (IOException e) {
				LOGGER.error("Could not remove node from index", e);
			}
			refreshReader();
		}

		@Override
		public IGraphIterable<IGraphNode> query(String key, Number from, Number to, boolean fromInclusive, boolean toInclusive) {
			try {
				final IndexSearcher searcher = new IndexSearcher(reader);

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
				final IndexSearcher searcher = new IndexSearcher(reader);
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
				IndexSearcher searcher = new IndexSearcher(reader);

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
			try {
				writer.flush();
			} catch (IOException e) {
				LOGGER.error("Failed to flush index", e);
			}
		}

		@Override
		public void delete() {
			try {
				writer.deleteDocuments(new TermQuery(new Term(INDEX_FIELD, name)));
				refreshReader();
			} catch (IOException e) {
				LOGGER.error(String.format("Could not delete index %s", name), e);
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
				final IndexSearcher searcher = new IndexSearcher(reader);

				final Document document = new Document();
				document.add(new StringField(NODEID_FIELD, getNodeId(gn), Store.YES));
				document.add(new StringField(CMPID_FIELD, getCompositeId(gn), Store.NO));
				document.add(new StringField(DOCTYPE_FIELD, NODE_DOCTYPE, Store.NO));
				document.add(new StringField(INDEX_FIELD, name, Store.NO));

				final TopDocs results = searcher.search(findNodeQuery(gn), 1);
				if (results.totalHits > 0) {
					// Create our own copy, for updating
					Document oldDocument = searcher.doc(results.scoreDocs[0].doc);
					values = copyOldValues(oldDocument, values);
				}

				for (Entry<String, Object> entry : values.entrySet()) {
					addField(document, entry.getKey(), entry.getValue());
				}

				writer.updateDocument(findNodeTerm(gn), document);
				refreshReader();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		protected void addField(Document document, final String key, final Object value) {
			final String fieldName = ATTRIBUTE_PREFIX + key;

			/*
			 * Point classes are very useful for fast range queries, but they do not store
			 * the value in the document. We need to add a StoredField so we can use the
			 * full version of remove (key, value and node).
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

			document.add(new StringField(FIELDS_FIELD, key, Store.NO));
		}

		protected Map<String, Object> copyOldValues(Document oldDocument, Map<String, Object> destValues) {
			destValues = new HashMap<>(destValues);

			for (IndexableField oldField : oldDocument.getFields()) {
				final String rawOldFieldName = oldField.name();
				if (rawOldFieldName.startsWith(ATTRIBUTE_PREFIX)) {
					final String oldFieldName = rawOldFieldName.substring(ATTRIBUTE_PREFIX.length());
					if (!destValues.containsKey(oldFieldName)) {
						if (oldField.stringValue() != null) {
							destValues.put(oldFieldName, oldField.stringValue());
						} else if (oldField.numericValue() != null) {
							destValues.put(oldFieldName, oldField.numericValue());
						} else {
							throw new IllegalStateException("Attribute stored with unknown value type");
						}
					}
				}
			}
			return destValues;
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

	private final Directory storage;
	private final Analyzer analyzer;
	private final GreycatDatabase database;
	private IndexWriter writer;
	private DirectoryReader reader;

	public GreycatLuceneIndexer(GreycatDatabase db, File dir) throws IOException {
		this.database = db;
		this.storage = new MMapDirectory(dir.toPath());
		this.analyzer = new CaseInsensitiveWhitespaceAnalyzer();
		this.writer = new IndexWriter(storage, new IndexWriterConfig(analyzer));
		this.reader = DirectoryReader.open(writer);
	}

	public IGraphNodeIndex getIndex(String name) {
		// Make sure the index is listed
		try {
			final IndexSearcher searcher = new IndexSearcher(reader);

			TopDocs scoreDocs = searcher.search(new TermQuery(new Term(INDEX_FIELD, name)), 1);
			if (scoreDocs.totalHits == 0) {
				Document doc = new Document();
				doc.add(new StringField(INDEX_FIELD, name, Store.YES));
				doc.add(new StringField(DOCTYPE_FIELD, INDEX_DOCTYPE, Store.YES));
				writer.addDocument(doc);

				refreshReader();
			}
		}  catch (IOException e) {
			LOGGER.error("Could not register index", e);
		}

		return new GreycatLuceneNodeIndex(name);
	}

	private void refreshReader() {
		try {
			DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
			if (newReader != null && reader != newReader) {
				reader.close();
				reader = newReader;
			}
		} catch (IOException e) {
			LOGGER.error("Could not refresh the reader", e);
		}
	}

	public Set<String> getIndexNames() {
		try (IndexReader reader = DirectoryReader.open(writer)) {
			final IndexSearcher searcher = new IndexSearcher(reader);
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
		try (IndexReader reader = DirectoryReader.open(writer)) {
			final IndexSearcher searcher = new IndexSearcher(reader);

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
			LOGGER.debug("Removing node {} from ALL indices", nodeId);
			writer.deleteDocuments(new TermQuery(new Term(NODEID_FIELD, nodeId)));
		} catch (IOException e) {
			LOGGER.error(String.format(
				"Could not remove node %s in world %d at time %d",
				gn.getId(), gn.getWorld(), gn.getTime()), e);
		}

		refreshReader();
	}

	/**
	 * Returns an identifying string for a specific node at a specific point in time.
	 */
	protected String getNodeId(GreycatNode gn) {
		return String.format("%d@%d@%d", gn.getWorld(), gn.getTime(), gn.getId());
	}

	/**
	 * Commits all changes to the index. Should only be called after
	 * an explicit transaction in Hawk, during shutdown, or when
	 * switching transactional modes.
	 * 
	 * @throws IOException
	 *             Failed to commit the changes.
	 */
	public void commit() throws IOException {
		writer.commit();
		refreshReader();
	}

	/**
	 * Rolls back all changes to the index.
	 * 
	 * @throws IOException
	 *             Failed to roll back the changes.
	 */
	public synchronized void rollback() throws IOException {
		reader.close();
		writer.rollback();

		this.writer = new IndexWriter(storage, new IndexWriterConfig(analyzer));
		this.reader = DirectoryReader.open(writer);
	}

	public void shutdown() {
		try {
			reader.close();
			writer.close();
			storage.close();
		} catch (IOException e) {
			LOGGER.error("Error during Lucene shutdown", e);
		}
	}
}
