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
import java.util.function.Function;

import org.hawk.core.graph.IGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;

/**
 * Finds all the instances of a type by starting from the files, and then
 * checking their entire contents by type. Faster for querying small files in
 * large graphs.
 */
public class FileFirstAllOf implements AllOf {
	private final EOLQueryEngine engine;
	private final Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles;

	public FileFirstAllOf(Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles, EOLQueryEngine engine) {
		this.engine = engine;
		this.allFiles = allFiles;
	}

	@Override
	public void addAllOf(IGraphNode typeNode, String typeorkind, Collection<Object> nodes) {
		for (IGraphNode rawFileNode : allFiles.apply(typeNode)) {
			final FileNode f = new FileNode(rawFileNode);
			for (ModelElementNode me : f.getModelElements()) {
				if (me.isOfKind(typeNode)) {
					nodes.add(new GraphNodeWrapper(me.getNode(), engine));
				}
			}
		}
	}
}