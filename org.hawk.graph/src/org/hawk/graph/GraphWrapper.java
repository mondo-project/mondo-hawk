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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.internal.updater.GraphModelUpdater;

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
	 * @param rplist
	 *            Pattern for the repository URL. <code>null</code> or
	 *            <code>"*"</code> will access all repositories.
	 * @param filePatterns
	 *            Patterns for the files. Having <code>"*"</code> as an element
	 *            or passing a null or empty {@link Iterable} will return all
	 *            files in the selected repository or repositories.
	 */
	public Set<FileNode> getFileNodes(List<String> repoPatterns, Iterable<String> filePatterns) {
		if (repoPatterns == null || !repoPatterns.iterator().hasNext()) {
			repoPatterns = Arrays.asList("*");
		}
		if (filePatterns == null || !filePatterns.iterator().hasNext()) {
			filePatterns = Arrays.asList("*");
		}

		final Set<FileNode> files = new LinkedHashSet<>();
		final IGraphNodeIndex fileIndex = graph.getFileIndex();
		for (String repo : repoPatterns) {
			for (String file : filePatterns) {
				String fullPattern;
				if ("*".equals(repo) && "*".equals(file)) {
					fullPattern = "*";
				} else {
					fullPattern = repo.trim() + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + file.trim();
				}
				for (IGraphNode n : fileIndex.query("id", fullPattern)) {
					files.add(new FileNode(n));
				}
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
			throw new NoSuchElementException("No node exists with id " + id);
		}
		return new ModelElementNode(rawNode);
	}

	/**
	 * Retrieves a {@link TypeNode} by identifier.
	 *
	 * @throws NoSuchElementException
	 *             No node with that identifier exists.
	 */
	public TypeNode getTypeNodeById(String id) {
		final IGraphNode rawNode = graph.getNodeById(id);
		if (rawNode == null) {
			throw new NoSuchElementException("No type node exists with id " + id);
		}
		return new TypeNode(rawNode);
	}

	/**
	 * Retrieves a metamodel node by namespace URI.
	 *
	 * @throws NoSuchElementException
	 *             No metamodel node with that namespace URI exists.
	 */
	public MetamodelNode getMetamodelNodeByNsURI(String nsURI) {
		final Iterator<IGraphNode> metamodelNode = graph.getMetamodelIndex().query("id", nsURI).iterator();
		if (!metamodelNode.hasNext()) {
			throw new NoSuchElementException("No metamodel node exists with URI " + nsURI);
		}
		return new MetamodelNode(metamodelNode.next());
	}

	/**
	 * Returns an iterable of all metamodel nodes.
	 */
	public Iterable<MetamodelNode> getMetamodelNodes() {
		final IGraphIterable<IGraphNode> metamodelNodes = graph.getMetamodelIndex().query("*", "*");
		return new Iterable<MetamodelNode>() {

			@Override
			public Iterator<MetamodelNode> iterator() {
				final Iterator<IGraphNode> itMN = metamodelNodes.iterator();
				return new Iterator<MetamodelNode>(){

					@Override
					public boolean hasNext() {
						return itMN.hasNext();
					}

					@Override
					public MetamodelNode next() {
						return new MetamodelNode(itMN.next());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}

		};
	}
}
