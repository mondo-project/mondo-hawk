/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Bergmann Gabor		- mondix API
 ******************************************************************************/
package org.hawk.mondix.query;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IUnaryView;

public abstract class HawkUnaryQueryInstance extends HawkQueryInstance
		implements IUnaryView {

	public HawkUnaryQueryInstance(IGraphDatabase graph, List<String> col,
			Map<String, Object> filter, IMondixRelation rel) throws Exception {

		super(graph, col, filter, rel);

		if (col.size() != 1) {
			dispose();
			throw new Exception("HawkUnaryQueryInstance created with "
					+ col.size() + " columns specified, disposed");
		}

	}

	@Override
	public Iterable<? extends Object> getValues() {

		final Iterable<? extends List<?>> tuples = getAllTuples();

		return new Iterable<Object>() {

			@Override
			public Iterator<Object> iterator() {
				return new Iterator<Object>() {
					Iterator<? extends List<?>> it = tuples.iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Object next() {
						List<?> values = it.next();
						if (values.size() != 1) {
							System.err
									.println("next() of getValues() returned a multi-valued list (should have returned a single value) retunring null");
							return null;
						} else
							return values.get(0);
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}

		};

	}
}
