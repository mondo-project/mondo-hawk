/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

/**
 * Utility functions for working with Lucene documents.
 */
public final class DocumentUtils {

	private DocumentUtils() {}

	public static final String ATTRIBUTE_PREFIX = "a_";
	public static final String FIELDS_FIELD = "h_fields";

	public static void replaceRawField(Document document, final String fieldName, final Object value) {
		document.removeFields(fieldName);
		addRawField(document, fieldName, value);
	}

	public static void addAttributes(final Document updated, Map<String, Object> values) {
		for (Entry<String, Object> entry : values.entrySet()) {
			final String attributeFieldName = ATTRIBUTE_PREFIX + entry.getKey();
			addRawField(updated, attributeFieldName, entry.getValue());
		}
	}
	
	public static void addRawField(Document document, final String fieldName, final Object value) {
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
	public static Document copy(Document doc) {
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
	public static void copyField(IndexableField field, final Document copy) {
		if (!FIELDS_FIELD.equals(field.name())) {
			if (field.numericValue() instanceof Number) {
				addRawField(copy, field.name(), field.numericValue());
			} else {
				addRawField(copy, field.name(), field.stringValue());
			}
		}
	}

}
