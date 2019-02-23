/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
package org.hawk.epsilon.emc.contextful;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.ModelElementNode;

/**
 * Finds all the instances of a type by starting from the types, and then
 * filtering their contents by file. Faster for rare types in large subtrees.
 */
public class TypeFirstAllOf implements AllOf {
	private Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles;
	private EOLQueryEngine engine;

	public TypeFirstAllOf(Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles, EOLQueryEngine engine) {
		this.allFiles = allFiles;
		this.engine = engine;
	}
	
	@Override
	public void addAllOf(IGraphNode typeNode, String typeorkind, Collection<Object> nodes) {
		final Set<IGraphNode> files = new HashSet<>();
		for (IGraphNode f : allFiles.apply(typeNode)) {
			files.add(f);
		}

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
			IGraphNode node = n.getStartNode();
			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				if (files.contains(e.getEndNode())) {
					nodes.add(new GraphNodeWrapper(node, engine));
				}
			}
		}
	}
}