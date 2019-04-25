/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;
import org.hawk.greycat.AbstractGreycatDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Base class for the Lucene-based indexers in the Greycat backend. Index names are stored
 * in the Lucene index itself, as documents of a specific type. Transactional operations are
 * delegated to the {@link SoftTxLucene} class.
 */
public abstract class AbstractLuceneIndexer<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLuceneIndexer.class);

	/** Universally unique document field. Useful for deletion / rollback of documents. */
	protected static final String UUID_FIELD = "h_id";
	protected static final String INDEX_FIELD   = "h_index";
	protected static final String DOCTYPE_FIELD = "h_doctype";
	protected static final String INDEX_DOCTYPE = "indexdecl";

	protected final AbstractGreycatDatabase database;
	protected final Cache<String, T> nodeIndexCache = CacheBuilder.newBuilder().maximumSize(100).build();
	protected final SoftTxLucene lucene;

	public AbstractLuceneIndexer(AbstractGreycatDatabase db, File dir) throws IOException {
		this.database = db;
		this.lucene = new SoftTxLucene(dir);
	}

	public T getNodeIndex(String name) throws Exception {
		return nodeIndexCache.get(name, () -> {
			final IndexSearcher searcher = new IndexSearcher(lucene.getReader());
	
			final Query query = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(INDEX_FIELD, name)), Occur.MUST)
				.add(new TermQuery(new Term(DOCTYPE_FIELD, INDEX_DOCTYPE)), Occur.MUST).build();
	
			final TotalHitCountCollector thc = new TotalHitCountCollector();
			searcher.search(query, thc);
			if (thc.getTotalHits() == 0) {
				Document doc = new Document();
				doc.add(new StringField(INDEX_FIELD, name, Store.YES));
				doc.add(new StringField(DOCTYPE_FIELD, INDEX_DOCTYPE, Store.YES));
				lucene.update(new Term(INDEX_FIELD, name), null, doc);
			}

			return createIndexInstance(name);
		});
	}

	/**
	 * Creates an instance of the Lucene index with the specified name. The index
	 * name will already have been recorded in Lucene.
	 */
	protected abstract T createIndexInstance(String name);

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

	protected void deleteIndex(String name) {
		// This operation is NOT time-aware: it will drop the entire index in one go.
		try {
			lucene.delete(new TermQuery(new Term(INDEX_FIELD, name)));
			nodeIndexCache.invalidate(name);
		} catch (IOException e) {
			LOGGER.error("Could not delete index " + name, e);
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

}