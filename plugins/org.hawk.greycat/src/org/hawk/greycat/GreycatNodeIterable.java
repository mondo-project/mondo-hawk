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
package org.hawk.greycat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Node;

public class GreycatNodeIterable implements IGraphIterable<IGraphNode> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNodeIterable.class);

	private final Callable<Node[]> nodesGenerator;
	private final GreycatDatabase db;

	public GreycatNodeIterable(GreycatDatabase db, Callable<Node[]> nodesGenerator) {
		this.db = db;
		this.nodesGenerator = nodesGenerator;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		try {
			final Node[] nodes = nodesGenerator.call();
			final List<IGraphNode> gNodes = new ArrayList<>();
			for (Node n : nodes) {
				GreycatNode gNode = new GreycatNode(db, n.world(), n.time(), n.id());
				if (!gNode.isSoftDeleted()) {
					gNodes.add(gNode);
				}
				n.free();
			}

			return gNodes.iterator();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptyIterator();
		}
	}

	@Override
	public int size() {
		try {
			return nodesGenerator.call().length;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return 0;
		}
	}

	@Override
	public IGraphNode getSingle() {
		return iterator().next();
	}

}
