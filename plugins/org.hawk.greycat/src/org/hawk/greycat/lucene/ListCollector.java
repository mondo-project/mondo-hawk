package org.hawk.greycat.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SimpleCollector;

class ListCollector extends SimpleCollector {
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