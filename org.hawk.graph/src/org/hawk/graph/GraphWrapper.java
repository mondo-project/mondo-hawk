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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.internal.updater.GraphModelBatchInjector;

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
	 * Returns all the file nodes with repository URLs that match the
	 * <code>repositoryPattern</code> and at least one of the
	 * <code>patterns</code>.
	 *
	 * @param repositoryPattern
	 *            Pattern for the repository URL. <code>null</code> or
	 *            <code>"*"</code> will access all repositories.
	 * @param filePatterns
	 *            Patterns for the files. Having <code>"*"</code> as an element
	 *            or passing a null or empty {@link Iterable} will return all
	 *            files in the selected repository or repositories.
	 */
	public Set<FileNode> getFileNodes(String repositoryPattern, Iterable<String> filePatterns) {
		if (repositoryPattern == null || repositoryPattern.trim().length() == 0) {
			/*
			 * Use "*" as the default value. This will look through all
			 * repositories. Note that Lucene only allows exact
			 * values for the keys of the file index (which use full repository
			 * URLs), so we'll have to do our own looping.
			 */
			repositoryPattern = "*";
		}
		if (filePatterns == null || !filePatterns.iterator().hasNext()) {
			filePatterns = Arrays.asList("*");
		}

		final Set<FileNode> files = new LinkedHashSet<>();
		final IGraphNodeIndex fileIndex = graph.getFileIndex();
		for (String s : filePatterns) {
			final String fullPattern = repositoryPattern.trim()
					+ GraphModelBatchInjector.FILEINDEX_REPO_SEPARATOR
					+ s.trim();
			for (IGraphNode n : fileIndex.query("id", fullPattern)) {
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
