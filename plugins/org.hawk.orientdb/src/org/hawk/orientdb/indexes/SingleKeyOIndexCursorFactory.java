/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
package org.hawk.orientdb.indexes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

final class SingleKeyOIndexCursorFactory implements OIndexCursorFactory {
	private final Object valueExpr;
	private final AbstractOrientIndex idx;
	private final String fieldName;
	private final boolean isRemote;
	private final Class<? extends Object> valueClass;

	SingleKeyOIndexCursorFactory(Object valueExpr, AbstractOrientIndex idx, String fieldName) {
		this.valueExpr = valueExpr;
		this.idx = idx;
		this.fieldName = fieldName;

		Class<? extends Object> tempValueClass = valueExpr.getClass();
		if ("*".equals(valueExpr)) {
			for (Class<?> klass : Arrays.asList(Integer.class, Double.class, String.class)) {
				if (idx.getIndex(klass) != null) {
					tempValueClass = klass;
				}
			}
		}
		this.valueClass = tempValueClass;

		/*
		 * See note in {@link AbstractOrientIndex#iterateEntriesBetween} for why
		 * we use a URL check instead of an instanceOf check later on.
		 */
		this.isRemote = idx.getDatabase().getGraph().getURL().startsWith("remote:");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator<OIdentifiable> query() {
		final OIndex<?> index = idx.getIndex(valueClass);
		if (index == null) {
			return Collections.emptyListIterator();
		}

		final Set<OCompositeKey> keys = Collections.singleton(new OCompositeKey(fieldName, valueExpr));
		if (!(valueExpr instanceof String)) {
			return iterateEntries(index, keys);
		}

		final String sValueExpr = valueExpr.toString();
		final int starPosition = sValueExpr.indexOf('*');
		if (starPosition < 0) {
			// No '*' found: go straight to the value
			return iterateEntries(index, keys);
		}
		else if (starPosition == 0) {
			// value expr starts with "*"
			if (sValueExpr.length() == 1) {
				// value expr is "*": iterate over everything
				if (isRemote) {
					return ((OIndex)index).cursor();
				} else {
					final Object minValue = AbstractOrientIndex.getMinValue(valueClass);
					final Object maxValue = AbstractOrientIndex.getMaxValue(valueClass);
					return AbstractOrientIndex.iterateEntriesBetween(fieldName, minValue, maxValue, true, true, index, idx.getDatabase().getGraph());
				}
			} else {
				// value expr starts with "*": filter all entries based on the fragments between the *
				final String[] fragments = sValueExpr.split("[*]");
				final OIndexCursor cursor = index.cursor();
				return new FragmentFilteredIndexCursor(fragments, cursor);
			}
		}
		else if (starPosition == sValueExpr.length() - 1) {
			final String prefix = sValueExpr.substring(0, starPosition);
			return prefixCursor(index, prefix);
		} else {
			// value expr has one or more "*" inside: use prefix to first * as a filter and
			// then wrap with a proper filter for the rest
			final String[] fragments = sValueExpr.split("[*]");
			final OIndexCursor cursor = prefixCursor(index, sValueExpr.substring(0, starPosition));
			return new FragmentFilteredIndexCursor(fragments, cursor);
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected Iterator<OIdentifiable> iterateEntries(final OIndex<?> index, final Set<OCompositeKey> keys) {
		// Not a string: go straight to the value
		if (isRemote) {
			// OIndexRemote does not support iterateEntries, and since 2.2 it's hidden behind a delegate
			// that doesn't expose getEntries(...), so we had to essentially bring some of its code here.
			final StringBuilder params = new StringBuilder(128);
		    if (!keys.isEmpty()) {
		      params.append("?");
		      for (int i = 1; i < keys.size(); i++) {
		        params.append(", ?");
		      }
		    }

		    final String text = String.format("select from index:%s where key in [%s]", index.getName(), params.toString());
		    final OCommandRequest cmd = new OCommandSQL(text);
		    Collection<ODocument> entries = (Collection<ODocument>) idx.getDatabase().getGraph().command(cmd).execute(keys.toArray());

			final Iterator<ODocument> itEntries = entries.iterator();
			return new Iterator<OIdentifiable>() {
				@Override
				public boolean hasNext() {
					return itEntries.hasNext();
				}

				@Override
				public OIdentifiable next() {
					return itEntries.next().field("rid");
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException("iterateEntries iterator");
				}
			};
		} else {
			return index.iterateEntries(keys, false);
		}
	}

	private OIndexCursor prefixCursor(OIndex<?> index, String prefix) {
		// prefix is S + C + "*", where S is a substring and C is a character:
		// do ranged query between S + C and S + (C+1) (the next Unicode code point)
		final char lastChar = prefix.charAt(prefix.length() - 1);
		final String rangeStart = prefix;
		final String rangeEnd = prefix.substring(0, rangeStart.length() - 1) + Character.toString((char)(lastChar+1));
		return AbstractOrientIndex.iterateEntriesBetween(fieldName, rangeStart, rangeEnd, true, true, index, idx.getDatabase().getGraph());
	}
}