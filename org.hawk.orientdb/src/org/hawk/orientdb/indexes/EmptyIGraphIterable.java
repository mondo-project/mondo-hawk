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

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;

final class EmptyIGraphIterable implements IGraphIterable<IGraphNode> {
	@Override
	public Iterator<IGraphNode> iterator() {
		return Collections.emptyListIterator();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public IGraphNode getSingle() {
		return null;
	}
}