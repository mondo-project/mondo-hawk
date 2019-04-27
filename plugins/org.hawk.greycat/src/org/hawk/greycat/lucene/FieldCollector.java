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
import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SimpleCollector;

class FieldCollector<T> extends SimpleCollector {
	protected final List<Integer> docIds = new ArrayList<>();
	protected final IndexSearcher searcher;
	private int docBase;

	private final String fieldName;
	private Function<IndexableField, T> extractor;

	protected FieldCollector(IndexSearcher searcher, String fieldName, Function<IndexableField, T> extractor) {
		this.searcher = searcher;
		this.fieldName = fieldName;
		this.extractor = extractor;
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

	public List<T> getValues() throws IOException {
		List<T> result = new ArrayList<>();
		for (int docId : docIds) {
			final Document document = searcher.doc(docId);
			result.add(extractor.apply(document.getField(fieldName)));
		}
		return result;
	}
}