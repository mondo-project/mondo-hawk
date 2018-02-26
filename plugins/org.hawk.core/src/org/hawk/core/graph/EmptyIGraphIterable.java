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
package org.hawk.core.graph;

import java.util.Collections;
import java.util.Iterator;

public final class EmptyIGraphIterable<T> implements IGraphIterable<T> {
	@Override
	public Iterator<T> iterator() {
		return Collections.emptyListIterator();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public T getSingle() {
		return iterator().next();
	}
}