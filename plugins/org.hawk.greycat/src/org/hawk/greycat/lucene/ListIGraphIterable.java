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

import java.util.Iterator;
import java.util.List;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;

final class ListIGraphIterable implements IGraphIterable<IGraphNode> {
	private final List<IGraphNode> nodes;

	protected ListIGraphIterable(List<IGraphNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		return nodes.iterator();
	}

	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public IGraphNode getSingle() {
		return nodes.iterator().next();
	}
}