/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb.indexes;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.index.OIndexRemote;

final class SingleKeyOIndexCursorFactory implements OIndexCursorFactory {
	private final Object valueExpr;
	private final AbstractOrientIndex idx;
	private final String fieldName;

	SingleKeyOIndexCursorFactory(Object valueExpr, AbstractOrientIndex idx, String fieldName) {
		this.valueExpr = valueExpr;
		this.idx = idx;
		this.fieldName = fieldName;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator<OIdentifiable> query() {
		final OIndex<?> index = idx.getIndex(valueExpr.getClass());
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
				if (index instanceof OIndexRemote) {
					return ((OIndexRemote)index).cursor();
				} else {
					final Object minValue = AbstractOrientIndex.getMinValue(valueExpr.getClass());
					final Object maxValue = AbstractOrientIndex.getMaxValue(valueExpr.getClass());
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Iterator<OIdentifiable> iterateEntries(final OIndex<?> index, final Set<OCompositeKey> keys) {
		// Not a string: go straight to the value
		if (index instanceof OIndexRemote) {
			// OIndexRemote does not support iterateEntries in 2.0.16
			return ((OIndexRemote)index).getEntries(keys).iterator();
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