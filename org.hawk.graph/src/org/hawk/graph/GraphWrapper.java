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
package org.hawk.graph;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;

/**
 * Wraps an {@link IGraphDatabase} that has been updated by this plugin.
 * Provides some common queries so callers will not need to know the
 * internal details of how the graph database has been set up.
 *
 * TODO This is an incomplete WIP abstraction on top of the graph that is
 * created by the model updaters in this plugin.
 */
public class GraphWrapper {

	private final IGraphDatabase graph;

	public GraphWrapper(IGraphDatabase graph) {
		this.graph = graph;
	}

	/**
	 * Returns a set of file nodes matching the specified patterns. The patterns are
	 * of the form '*.extension' or 'file.ext'.
	 */
	public Set<IGraphNode> getFileNodes(String... patterns) throws Exception {
		final Set<IGraphNode> interestingFileNodes = new HashSet<>();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			IGraphNodeIndex fileIndex = graph.getFileIndex();

			for (String s : patterns) {
				for (IGraphNode n : fileIndex.query("id", s)) {
					interestingFileNodes.add(n);
				}
			}

			tx.success();
			return interestingFileNodes;
		}
	}

}
