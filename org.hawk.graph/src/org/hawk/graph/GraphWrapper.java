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

import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;

/**
 * Wraps an {@link IGraphDatabase} that has been updated by this plugin. This is
 * the starting point for the read-only abstraction of the graph populated by
 * this updater. Users of this class and any other classes of this package are
 * expected to manage their own transactions with
 * {@link IGraphDatabase#beginTransaction()}.
 *
 * TODO This is an incomplete WIP abstraction. More methods will be called as
 * the existing queries are moved into this API.
 */
public class GraphWrapper {

	private final IGraphDatabase graph;

	public GraphWrapper(IGraphDatabase graph) {
		this.graph = graph;
	}

	/**
	 * Convenience version of {@link #getRawFileNodes(Iterable)} that wraps all
	 * results as {@link FileNode}s.
	 */
	public Set<FileNode> getFileNodes(Iterable<String> patterns) {
		final IGraphNodeIndex fileIndex = graph.getFileIndex();

		final Set<FileNode> files = new LinkedHashSet<>();
		for (String s : patterns) {
			for (IGraphNode n : fileIndex.query("id", s)) {
				files.add(new FileNode(n));
			}
		}
		return files;
	}

	/**
	 * Returns the graph wrapped by this instance.
	 */
	public IGraphDatabase getGraph() {
		return graph;
	}

	/**
	 * Retrieves a {@link ModelElementNode} by identifier.
	 *
	 * @throws NoSuchElementException
	 *             No node with that identifier exists.
	 */
	public ModelElementNode getModelElementNodeById(Object id) {
		final IGraphNode rawNode = graph.getNodeById(id);
		if (rawNode == null) {
			throw new NoSuchElementException();
		}
		return new ModelElementNode(rawNode);
	}
}
