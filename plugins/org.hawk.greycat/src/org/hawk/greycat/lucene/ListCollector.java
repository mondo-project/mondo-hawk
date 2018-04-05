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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SimpleCollector;

class ListCollector extends SimpleCollector {
	protected final List<Integer> docIds = new ArrayList<>();
	protected final IndexSearcher searcher;
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