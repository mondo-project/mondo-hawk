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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndexCursor;

final class FragmentFilteredIndexCursor implements Iterator<OIdentifiable> {
	private final String[] fragments;
	private final OIndexCursor cursor;
	OIdentifiable next = null;

	FragmentFilteredIndexCursor(String[] fragments, OIndexCursor cursor) {
		this.fragments = fragments;
		this.cursor = cursor;
	}

	@Override
	public boolean hasNext() {
		if (next != null) return true;

		Entry<Object, OIdentifiable> entry = cursor.nextEntry();
		outer:
		while (entry != null) {
			final String s = entry.getKey().toString();
			int currentPosition = 0;
			for (String fragment : fragments) {
				final int matchPos = s.indexOf(fragment, currentPosition);
				if (matchPos < 0) {
					entry = cursor.nextEntry();
					continue outer;
				} else {
					currentPosition = matchPos + fragment.length();
				}
			}
			next = entry.getValue();
			return true;
		}

		return false;
	}

	@Override
	public OIdentifiable next() {
		if (!hasNext()) throw new NoSuchElementException();
		final OIdentifiable ret = next;
		next = null;
		return ret;
	}

	@Override
	public void remove() {
		cursor.remove();
	}
}