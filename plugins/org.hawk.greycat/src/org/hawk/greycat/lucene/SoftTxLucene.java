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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements "soft" transactions within Lucene. This class is NOT thread-safe: only one
 * thread is assumed to be accessing this Lucene index at a time.
 */
public final class SoftTxLucene {
	private static final Logger LOGGER = LoggerFactory.getLogger(SoftTxLucene.class);

	private final Directory storage;
	private final Analyzer analyzer;
	private final IndexWriter writer;

	private interface IUndoable {
		void doWork() throws IOException;
		void undoWork() throws IOException;
	}

	private final List<IUndoable> rollbackLog = new LinkedList<>();
	private final ScheduledExecutorService executor;
	private final SearcherManager searchManager;

	public class SearcherCloseable implements Closeable {
		private IndexSearcher searcher;

		public SearcherCloseable() throws IOException {
			this.searcher = searchManager.acquire();
		}

		public IndexSearcher get() {
			return searcher;
		}

		@Override
		public void close() {
			if (searcher != null) {
				try {
					searchManager.release(searcher);
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
				searcher = null;
			}
		}
	}

	public SoftTxLucene(File dir) throws IOException {
		this.storage = new MMapDirectory(dir.toPath());
		this.analyzer = new CaseInsensitiveWhitespaceAnalyzer();
		this.writer = new IndexWriter(storage, new IndexWriterConfig(analyzer));
		this.searchManager = new SearcherManager(writer, true, false, null);
		
		this.executor = Executors.newScheduledThreadPool(1);
		executor.scheduleWithFixedDelay(() -> {
			synchronized (rollbackLog) {
				try {
					writer.commit();
					refreshReader();
				} catch (IOException e) {
					LOGGER.error("Periodic commit of Lucene at " + storage + " failed", e);
				}
			}
		}, 30, 30, TimeUnit.SECONDS); 
	}

	public SearcherCloseable getSearcher() throws IOException {
		return new SearcherCloseable();
	}

	private void refreshReader() throws IOException {
		searchManager.maybeRefresh();
	}

	public void flush() {
		try {
			writer.flush();
			searchManager.maybeRefreshBlocking();
		} catch (IOException e) {
			LOGGER.error("Failed to flush index", e);
		}
	}

	public void shutdown() {
		try {
			executor.shutdown();
			executor.awaitTermination(300, TimeUnit.SECONDS);

			writer.close();
			storage.close();
		} catch (IOException e) {
			LOGGER.error("Error during Lucene shutdown", e);
		} catch (InterruptedException e) {
			LOGGER.error("Gave up waiting for Lucene to commit", e);
		}
	}

	public void commit() {
		synchronized (rollbackLog) {
			rollbackLog.clear();
		}
	}

	public void rollback() throws IOException {
		synchronized (rollbackLog) {
			for (ListIterator<IUndoable> itUndoable = rollbackLog.listIterator(rollbackLog.size()); itUndoable.hasPrevious(); ) {
				itUndoable.previous().undoWork();
			}
			rollbackLog.clear();
			refreshReader();
		}
	}

	/**
	 * Updates a single document with this term.
	 */
	public void update(Term term, Document oldDocument, Document newDocument) throws IOException {
		doWork(new IUndoable() {
			private Document prevDocument = null;

			@Override
			public void doWork() throws IOException {
				prevDocument = DocumentUtils.copy(oldDocument);
				writer.updateDocument(term, newDocument);
				refreshReader();
			}

			@Override
			public void undoWork() throws IOException {
				if (prevDocument == null) {
					writer.deleteDocuments(term);
				} else {
					writer.updateDocument(term, prevDocument);
				}
			}
		});
	}

	/**
	 * Deletes a single document with this term.
	 */
	public void delete(Term term) throws IOException {
		doWork(new IUndoable() {
			private Document oldDocument = null;

			@Override
			public void doWork() throws IOException {
				oldDocument = DocumentUtils.copy(getDocument(term));
				writer.deleteDocuments(term);
				refreshReader();
			}

			@Override
			public void undoWork() throws IOException {
				if (oldDocument != null) {
					writer.addDocument(oldDocument);
				}
			}
		});
	}

	/**
	 * Deletes all documents matching this query.
	 */
	public void delete(Query query) throws IOException {
		doWork(new IUndoable() {
			private List<Document> oldDocuments;

			@Override
			public void doWork() throws IOException {
				try (SearcherCloseable sc = new SearcherCloseable()) {
					final ListCollector lc = new ListCollector(sc.get());
					sc.get().search(query, lc);
					oldDocuments = lc.getDocuments().stream().map(d -> DocumentUtils.copy(d)).collect(Collectors.toList());
					writer.deleteDocuments(query);
				}
			}

			@Override
			public void undoWork() throws IOException {
				for (Document old : oldDocuments) {
					writer.addDocument(old);
				}
			}
		});
	}

	private Document getDocument(Term term) throws IOException {
		try (SearcherCloseable sc = new SearcherCloseable()) {
			final IndexSearcher searcher = sc.get();
			final TopDocs topDocs = searcher.search(new TermQuery(term), 1);
			if (topDocs.totalHits > 0) {
				return searcher.doc(topDocs.scoreDocs[0].doc);
			}
		}
		return null;
	}

	private void doWork(IUndoable iUndoable) throws IOException {
		synchronized (rollbackLog) {
			iUndoable.doWork();
			rollbackLog.add(iUndoable);
		}
	}
}
