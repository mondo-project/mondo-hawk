/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
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

import java.util.Iterator;
import java.util.Set;

final class StarKeyOIndexCursorFactoryIterable implements Iterable<OIndexCursorFactory> {
	private final Object valueExpr;
	private final Set<String> valueIdxNames;
	private final AbstractOrientIndex idx;

	StarKeyOIndexCursorFactoryIterable(Object valueExpr, AbstractOrientIndex idx, Set<String> valueIdxNames) {
		this.valueExpr = valueExpr;
		this.valueIdxNames = valueIdxNames;
		this.idx = idx;
	}

	@Override
	public Iterator<OIndexCursorFactory> iterator() {
		final Iterator<String> itIdxNames = valueIdxNames.iterator();
		return new Iterator<OIndexCursorFactory>() {
			@Override
			public boolean hasNext() {
				return itIdxNames.hasNext();
			}

			@Override
			public OIndexCursorFactory next() {
				return new SingleKeyOIndexCursorFactory(valueExpr, idx, itIdxNames.next());
			}

			@Override
			public void remove() {
				itIdxNames.remove();
			}
		};
	}
}