/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
package org.hawk.graph;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.graph.updater.GraphModelUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphWrapper.class);

	private final IGraphDatabase graph;

	public GraphWrapper(IGraphDatabase graph) {
		this.graph = graph;
	}

	/**
	 * Convenience version of
	 * {@link #getFileNodes(IGraphNodeIndex, Iterable, Iterable)} which passes the
	 * database's default file index configuration.
	 */
	public Set<FileNode> getFileNodes(Iterable<String> repoPatterns, Iterable<String> filePatterns) {
		final IGraphNodeIndex fileIndex = graph.getFileIndex();
		return getFileNodes(fileIndex, repoPatterns, filePatterns);
	}

	/**
	 * Returns all the file nodes with repository URLs within the specified file
	 * index (usually from {@link IGraphDatabase#getFileIndex()}) that match the
	 * <code>repositoryPattern</code> and at least one of the <code>patterns</code>.
	 *
	 * @param fileIndex    File index that can retrieve all available files through
	 *                     glob patterns.
	 * @param rplist       Pattern for the repository URL. <code>null</code> or
	 *                     <code>"*"</code> will access all repositories.
	 * @param filePatterns Patterns for the files. Having <code>"*"</code> as an
	 *                     element or passing a null or empty {@link Iterable} will
	 *                     return all files in the selected repository or
	 *                     repositories. If a file pattern has URI-invalid
	 *                     characters (e.g. spaces), it will be URI-encoded first.
	 */
	public Set<FileNode> getFileNodes(final IGraphNodeIndex fileIndex, Iterable<String> repoPatterns,
			Iterable<String> filePatterns) {
		if (repoPatterns == null || !repoPatterns.iterator().hasNext()) {
			repoPatterns = Arrays.asList("*");
		}
		if (filePatterns == null || !filePatterns.iterator().hasNext()) {
			filePatterns = Arrays.asList("*");
		}

		final Set<FileNode> files = new LinkedHashSet<>();
		for (String repo : repoPatterns) {
			for (String file : filePatterns) {
				String fullPattern;
				if ("*".equals(repo) && "*".equals(file)) {
					fullPattern = "*";
				} else {
					try {
						// Is this a valid URI?
						new URI(file);
					} catch (URISyntaxException ex) {
						try {
							// No, encode it (but keep slashes and use %20 instead of + for spaces)
							file = URLEncoder.encode(file, StandardCharsets.UTF_8.toString())
									.replaceAll("%2F", "/").replaceAll("[+]", "%20");
						} catch (UnsupportedEncodingException e) {
							LOGGER.error(e.getMessage(), e);
						}
					}

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
	 * Retrieves a {@link ModelElementNode} referenced from another node. Will
	 * make sure to reuse the same context, i.e. same world/timepoint if we are
	 * working within a time-aware graph.
	 */
	public ModelElementNode getModelElementNodeById(ModelElementNode ctx, Object id) {
		final IGraphNode ctxNode = ctx.getNode();
		final IGraphNode rawNode = graph.getNodeById(id);
		if (rawNode == null) {
			throw new NoSuchElementException("No node exists with id " + id);
		}

		if (ctxNode instanceof ITimeAwareGraphNode) {
			ITimeAwareGraphNode ctxTANode = (ITimeAwareGraphNode) ctxNode;
			ITimeAwareGraphNode rawTANode = (ITimeAwareGraphNode) rawNode;
			return new ModelElementNode(rawTANode.travelInTime(ctxTANode.getTime()));
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
		final Iterator<? extends IGraphNode> metamodelNode = graph.getMetamodelIndex().query("id", nsURI).iterator();
		if (!metamodelNode.hasNext()) {
			throw new NoSuchElementException("No metamodel node exists with URI " + nsURI);
		}
		return new MetamodelNode(metamodelNode.next());
	}

	/**
	 * Returns an iterable of all metamodel nodes.
	 */
	public Iterable<MetamodelNode> getMetamodelNodes() {
		final IGraphIterable<? extends IGraphNode> metamodelNodes = graph.getMetamodelIndex().query("*", "*");
		return new Iterable<MetamodelNode>() {

			@Override
			public Iterator<MetamodelNode> iterator() {
				final Iterator<? extends IGraphNode> itMN = metamodelNodes.iterator();
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
