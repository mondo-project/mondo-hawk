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
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;
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

		private static final String CMPID_FIELD   = "h_cmpid";
		private static final String NODE_DOCTYPE = "node";
		
		protected class NodeListCollector extends ListCollector {
			protected NodeListCollector(IndexSearcher searcher) {
				super(searcher);
			}

			public List<IGraphNode> getNodes() throws IOException {
				List<IGraphNode> result = new ArrayList<>();
				for (Document document : getDocuments()) {
					final String compositeId = document.getField(CMPID_FIELD).stringValue();
					final GreycatNode gn = getNodeByDocumentId(compositeId);
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
			try (IndexWriter writer = createWriter()) {
				final DirectoryReader reader = DirectoryReader.open(writer);
				final IndexSearcher searcher = new IndexSearcher(reader);

				final String docId = getDocumentId((GreycatNode) n);
				final TopDocs results = searcher.search(findNodeQuery(docId), 1);
				if (results.totalHits > 0) {
					final Document document = searcher.doc(results.scoreDocs[0].doc);

					if (key == null) {
						removeValue(writer, document, docId, value);
					} else {
						removeKeyValue(writer, document, docId, key, value);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Could not remove node from index", e);
			}
		}

		protected void removeKeyValue(IndexWriter writer, final Document document, final String docId, String key,
				Object value) throws IOException {
			// TODO: handle other value types
			final List<IndexableField> remainingFields = new ArrayList<>();
			boolean matched = false;
			for (IndexableField field : document.getFields(ATTRIBUTE_PREFIX + key)) {
				final String existingValue = field.stringValue();
				if (value == null || existingValue.equals(value)) {
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
				writer.updateDocument(nodeTerm(docId), document);
			}
		}

		protected void removeValue(IndexWriter writer, final Document document, final String docId, Object value)
				throws IOException {
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
				writer.updateDocument(nodeTerm(docId), copy);
			}
		}

		@Override
		public void remove(IGraphNode n) {
			try (IndexWriter writer = createWriter()) {
				final String docId = getDocumentId((GreycatNode) n);
				final Term term = nodeTerm(docId);
				writer.deleteDocuments(term);
			} catch (IOException e) {
				LOGGER.error("Could not remove node from index", e);
			}
		}

		@Override
		public IGraphIterable<IGraphNode> query(String key, Number from, Number to, boolean fromInclusive, boolean toInclusive) {
			try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
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
				query = new BooleanQuery.Builder()
					.add(getIndexQuery(), Occur.FILTER)
					.add(query, Occur.MUST)
					.build();

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
			try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
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
						final String regex = sValueExpr.replaceAll("[*]", ".*");
						query = new RegexpQuery(new Term(ATTRIBUTE_PREFIX + key, regex));
					}
				}

				if (query == null) {
					query = getIndexQuery();
				} else {
					query = new BooleanQuery.Builder()
						.add(getIndexQuery(), Occur.FILTER)
						.add(query, Occur.MUST)
						.build();
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
			try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
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

				final Query query = new BooleanQuery.Builder()
					.add(getIndexQuery(), Occur.FILTER)
					.add(valueQuery, Occur.MUST)
					.build();

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
			// nothing to do
		}

		@Override
		public void delete() {
			// TODO Auto-generated method stub
			
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

			try (IndexWriter writer = createWriter()) {
				final DirectoryReader reader = DirectoryReader.open(writer);
				final IndexSearcher searcher = new IndexSearcher(reader);

				final String docId = getDocumentId(gn);
				final Document document = new Document();
				document.add(new StringField(CMPID_FIELD, docId, Store.YES));
				document.add(new StringField(DOCTYPE_FIELD, NODE_DOCTYPE, Store.NO));
				document.add(new StringField(INDEX_FIELD, name, Store.NO));

				final TopDocs results = searcher.search(findNodeQuery(docId), 1);
				if (results.totalHits > 0) {
					// Create our own copy, for updating
					Document oldDocument = searcher.doc(results.scoreDocs[0].doc);
					values = copyOldValues(oldDocument, values);
				}

				for (Entry<String, Object> entry : values.entrySet()) {
					addField(document, entry.getKey(), entry.getValue());
				}

				writer.updateDocument(nodeTerm(docId), document);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		protected void addField(Document document, final String key, final Object value) {
			final String fieldName = ATTRIBUTE_PREFIX + key;

			if (value instanceof Float || value instanceof Double) {
				document.add(new DoublePoint(fieldName, ((Number)value).doubleValue()));
			} else if (value instanceof Number) {
				document.add(new LongPoint(fieldName, ((Number)value).longValue()));
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

		protected TermQuery findNodeQuery(final String compositeID) {
			return new TermQuery(nodeTerm(compositeID));
		}

		protected Term nodeTerm(final String compositeID) {
			return new Term(CMPID_FIELD, compositeID);
		}

		protected Query getIndexQuery() {
			return new PrefixQuery(new Term(CMPID_FIELD, name + "@"));
		}

		protected String getDocumentId(final GreycatNode gn) {
			return String.format("%s@%d@%d@%d", name, gn.getWorld(), gn.getTime(), gn.getId());
		}

		protected GreycatNode getNodeByDocumentId(String compositeId) {
			String[] parts = compositeId.split("@", 4);
			final long id = Long.parseLong(parts[3]);
			final GreycatNode gn = database.getNodeById(id);
			return gn;
		}
	}

	private final NIOFSDirectory indexDirectory;
	private final Analyzer analyzer;
	private final GreycatDatabase database;

	public GreycatLuceneIndexer(GreycatDatabase db, File indexDirectory) throws IOException {
		this.database = db;
		this.indexDirectory = new NIOFSDirectory(indexDirectory.toPath());
		this.analyzer = new CaseInsensitiveWhitespaceAnalyzer();
	}

	public IGraphNodeIndex getIndex(String name) {
		// Make sure the index is listed
		try (IndexWriter writer = createWriter()) {
			final DirectoryReader reader = DirectoryReader.open(writer);
			final IndexSearcher searcher = new IndexSearcher(reader);

			TopDocs scoreDocs = searcher.search(new TermQuery(new Term(INDEX_FIELD, name)), 1);
			if (scoreDocs.totalHits == 0) {
				Document doc = new Document();
				doc.add(new StringField(INDEX_FIELD, name, Store.YES));
				doc.add(new StringField(DOCTYPE_FIELD, INDEX_DOCTYPE, Store.YES));
				writer.addDocument(doc);
			}
		}  catch (IOException e) {
			LOGGER.error("Could not register index", e);
		}

		return new GreycatLuceneNodeIndex(name);
	}

	private IndexWriter createWriter() throws IOException {
		return new IndexWriter(indexDirectory, new IndexWriterConfig(analyzer));
	}

	public Set<String> getIndexNames() {
		try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
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
}
