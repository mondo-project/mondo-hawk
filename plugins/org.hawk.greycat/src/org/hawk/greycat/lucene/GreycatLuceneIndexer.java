package org.hawk.greycat.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
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

	protected final class GreycatLuceneNodeIndex implements IGraphNodeIndex {

		protected final class ListCollector extends SimpleCollector {
			private final List<IGraphNode> nodes;
			private final IndexSearcher searcher;

			protected ListCollector(List<IGraphNode> nodes, IndexSearcher searcher) {
				this.nodes = nodes;
				this.searcher = searcher;
			}

			@Override
			public boolean needsScores() {
				return false;
			}

			@Override
			public void collect(int doc) throws IOException {
				final Document document = searcher.doc(doc);
				final String compositeId = document.getField(CMPID_FIELD).stringValue();
				final GreycatNode gn = getNodeByDocumentId(compositeId);
				nodes.add(gn);
			}
		}
		
		private static final String ATTRIBUTE_PREFIX = "a_";
		private static final String CMPID_FIELD = "h_composite_id";

		private final String name;

		public GreycatLuceneNodeIndex(String name) {
			this.name = name;
		}

		@Override
		public void remove(String key, Object value, IGraphNode n) {
			// TODO Auto-generated method stub
			
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IGraphIterable<IGraphNode> query(String key, Object valueExpr) {
			try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
				final IndexSearcher searcher = new IndexSearcher(reader);
				final List<IGraphNode> nodes = new ArrayList<>();
				final Collector c = new ListCollector(nodes, searcher);

				final String sValueExpr = valueExpr.toString();
				final int starIdx = sValueExpr.indexOf('*');
				Query query;
				if (starIdx == -1) {
					query = new TermQuery(new Term(ATTRIBUTE_PREFIX + key, sValueExpr));
				} else if (starIdx == sValueExpr.length() - 1) {
					final String prefix = sValueExpr.substring(0, sValueExpr.length() - 1);
					query = new PrefixQuery(new Term(ATTRIBUTE_PREFIX + key, prefix));
				} else {
					final String regex = sValueExpr.replaceAll("[*]", ".*");
					query = new RegexpQuery(new Term(ATTRIBUTE_PREFIX + key, regex));
				}

				searcher.search(query, c);
				return new ListIGraphIterable(nodes);
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
			// TODO Auto-generated method stub
			return null;
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
			add(n, Collections.singletonMap(key, value));
		}

		@Override
		public void add(IGraphNode n, Map<String, Object> values) {
			final GreycatNode gn = (GreycatNode)n;

			try (IndexWriter writer = createWriter()) {
				// Find existing document first, if any
				final DirectoryReader reader = DirectoryReader.open(writer);
				final IndexSearcher searcher = new IndexSearcher(reader);

				final String docId = getDocumentId(gn);
				final TopDocs results = searcher.search(findNodeQuery(docId), 1);
				final boolean isNew = results.totalHits == 0;
				Document document;
				if (isNew) {
					document = new Document();
					document.add(new StringField(CMPID_FIELD, docId, Store.YES));
				} else {
					document = searcher.doc(results.scoreDocs[0].doc);
				}

				for (Entry<String, Object> entry : values.entrySet()) {
					final String fieldName = ATTRIBUTE_PREFIX + entry.getKey();
					document.removeField(fieldName);

					// TODO: handle numeric types here
					document.add(new StringField(fieldName, entry.getValue().toString(), Store.YES));
				}

				writer.updateDocument(nodeTerm(docId), document);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		protected TermQuery findNodeQuery(final String compositeID) {
			return new TermQuery(nodeTerm(compositeID));
		}

		protected Term nodeTerm(final String compositeID) {
			return new Term(CMPID_FIELD, compositeID);
		}

		protected String getDocumentId(final GreycatNode gn) {
			return String.format("%d_%d_%d", gn.getWorld(), gn.getTime(), gn.getId());
		}

		protected GreycatNode getNodeByDocumentId(String compositeId) {
			String[] parts = compositeId.split("_", 3);
			final long id = Long.parseLong(parts[2]);
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
		return new GreycatLuceneNodeIndex(name);
	}

	private IndexWriter createWriter() throws IOException {
		return new IndexWriter(indexDirectory, new IndexWriterConfig(analyzer));
	}
}
