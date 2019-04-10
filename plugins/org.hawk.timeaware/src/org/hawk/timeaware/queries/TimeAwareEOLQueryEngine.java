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
package org.hawk.timeaware.queries;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributorRegistry;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeIndex;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.contextful.AllOf;
import org.hawk.epsilon.emc.contextful.TypeFirstAllOf;
import org.hawk.epsilon.emc.pgetters.GraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.FileNodeWrapper;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.timeaware.queries.operations.reflective.TimeAwareNodeHistoryOperationContributor;
import org.hawk.timeaware.queries.operations.reflective.TypeHistoryOperationContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variant of {@link EOLQueryEngine} that exposes the time-aware nature of the
 * index through EOL. Both types and model elements have the same set of
 * time-aware properties and references: <code>.time</code>,
 * <code>.earliest</code>, <code>.latest</code>, <code>.versions</code>.
 *
 * TODO: integrate a "human time to seconds since epoch UTC" library.
 *
 * TODO: allow for revision-based times as well.
 */
public class TimeAwareEOLQueryEngine extends EOLQueryEngine {

	/**
	 * This is the default 'all of' provider. It will go to the type node and follow
	 * all 'is of type' and/or 'is of kind' edges in reverse, with no filtering.
	 */
	protected class ContextlessAllOf implements AllOf {
		public void addAllOf(IGraphNode typeNode, final String typeorkind, Collection<Object> nodes) {
			for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
				nodes.add(new GraphNodeWrapper(n.getStartNode(), TimeAwareEOLQueryEngine.this));
			}
			broadcastAllOfXAccess(nodes);
		}
	}

	/**
	 * This is the default 'all files' provider. Without a source node, it will
	 * return all available file nodes just like the regular EOL query engine. With
	 * a source node, it will return the file nodes on the same instant as that
	 * node.
	 */
	protected class ContextlessTimeAwareAllFiles implements Function<IGraphNode, Iterable<? extends IGraphNode>> {
		public Iterable<? extends ITimeAwareGraphNode> apply(IGraphNode sourceNode) {
			final ITimeAwareGraphDatabase taGraph = (ITimeAwareGraphDatabase) graph;
			if (sourceNode == null) {
				return taGraph.allNodes(FileNode.FILE_NODE_LABEL);
			} else {
				final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) sourceNode;
				return taGraph.allNodes(FileNode.FILE_NODE_LABEL, taNode.getTime());
			}
		}
	}

	/**
	 * This is a contextful 'all files' provider, which will use glob patterns to
	 * filter the files to be returned. If there is a context node, the files to be
	 * filtered will be those that existed at the same instant.
	 */
	protected class GlobPatternTimeAwareAllFiles implements Function<IGraphNode, Iterable<? extends IGraphNode>> {
		private final List<String> rplist;
		private final List<String> fplist;

		protected GlobPatternTimeAwareAllFiles(List<String> rplist, List<String> fplist) {
			this.rplist = rplist;
			this.fplist = fplist;
		}

		@Override
		public Iterable<? extends ITimeAwareGraphNode> apply(IGraphNode sourceNode) {
			final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) sourceNode;
			final ITimeAwareGraphDatabase tadb = (ITimeAwareGraphDatabase) graph;
			final GraphWrapper gw = new GraphWrapper(tadb);

			try (IGraphTransaction tx = tadb.beginTransaction()) {
				Set<FileNode> fileNodes;
				if (sourceNode == null) {
					fileNodes = gw.getFileNodes(rplist, fplist);
				} else {
					final ITimeAwareGraphNodeIndex fileIndex = tadb.getFileIndex().travelInTime(taNode.getTime());
					fileNodes = gw.getFileNodes(fileIndex, rplist, fplist);
				}

				final Set<ITimeAwareGraphNode> rawFileNodes = new HashSet<>();
				for (FileNode fn : fileNodes) {
					rawFileNodes.add((ITimeAwareGraphNode) fn.getNode());
				}
				tx.success();

				return rawFileNodes;
			} catch (Exception e) {
				LOGGER.error("Failed to find matching files", e);
				return Collections.emptySet();
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareEOLQueryEngine.class);

	/**
	 * Provides a set of file nodes, based on some other node. {@link #getFiles()}
	 * and other similar methods rely on this, so we may use composition rather than
	 * inheritance for this type of behaviour.
	 */
	private Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles;

	/**
	 * Provides on demand a list of instances of a type.
	 */
	private AllOf allOf;

	public TimeAwareEOLQueryEngine() {
		this.allFiles = new ContextlessTimeAwareAllFiles();
		this.allOf = new ContextlessAllOf();
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	protected GraphPropertyGetter createContextlessPropertyGetter() {
		return new TimeAwareGraphPropertyGetter(graph, this);
	}

	/**
	 * Extends "allInstances" with the concept of time. It is likely to be used
	 * through the {@link #allInstancesNow()} convenience function.
	 * 
	 * The regular "allInstances" can still be used in timeline queries, where the
	 * global timepoint is changed.
	 */
	public Collection<?> allInstancesAt(long timepoint) {
		final Set<Object> allContents = new HashSet<Object>();
		final ITimeAwareGraphDatabase taGraph = (ITimeAwareGraphDatabase) graph;
		for (IGraphNode node : taGraph.allNodes(ModelElementNode.OBJECT_VERTEX_LABEL, timepoint)) {
			GraphNodeWrapper wrapper = new GraphNodeWrapper(node, this);
			allContents.add(wrapper);
		}
		return allContents;
	}

	/**
	 * Returns all the instances in the model at this current moment, by visiting
	 * the type nodes at the current point in time.
	 */
	public Collection<?> allInstancesNow() {
		return allInstancesAt(System.currentTimeMillis());
	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException {
		final HawkState currentState = m.getCompositeStateListener().getCurrentState();
		if (currentState != HawkState.RUNNING) {
			throw new QueryExecutionException(
					String.format("Cannot run the query, as the indexer is not in the RUNNING state: it is %s instead.",
							currentState));
		}
		if (!(m.getGraph() instanceof ITimeAwareGraphDatabase)) {
			throw new QueryExecutionException(getClass().getName() + " can only be used with time-aware backends");
		}

		String defaultnamespaces = null;
		if (context != null) {
			defaultnamespaces = (String) context.get(PROPERTY_DEFAULTNAMESPACES);
		}

		final TimeAwareEOLQueryEngine q = new TimeAwareEOLQueryEngine();
		try {
			q.load(m);
			q.setDefaultNamespaces(defaultnamespaces);
			if (context == null || context.isEmpty()) {
				// nothing to do!
			} else {
				q.setContext(context);
			}
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}

		final IEolModule module = createModule();
		module.getContext().setOperationFactory(new TimeAwareEOLOperationFactory(q));

		final OperationContributorRegistry opcRegistry = module.getContext().getOperationContributorRegistry();
		opcRegistry.add(new TimeAwareNodeHistoryOperationContributor(q));
		opcRegistry.add(new TypeHistoryOperationContributor(q));
		parseQuery(query, context, q, module);
		return q.runQuery(module);
	}

	@Override
	public String getHumanReadableName() {
		return "Time Aware " + super.getHumanReadableName();
	}

	@Override
	public Collection<Object> getAllOf(IGraphNode typeNode, String typeorkind) {
		final Collection<Object> nodes = createAllOfCollection(typeNode);
		allOf.addAllOf(typeNode, typeorkind, nodes);
		return nodes;
	}

	@Override
	public Set<FileNodeWrapper> getFiles() {
		Set<FileNodeWrapper> allFNW = new HashSet<>();
		for (IGraphNode rawNode : allFiles.apply(null)) {
			allFNW.add(new FileNodeWrapper(new FileNode(rawNode), this));
		}
		return allFNW;
	}

	private void setAllFiles(Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles) {
		this.allFiles = allFiles;
	}

	private void setAllOf(AllOf allOf) {
		this.allOf = allOf;
	}

	/**
	 * Loads up the specified context into this object, changing its 'all of' and
	 * 'all files' providers.
	 */
	private void setContext(Map<String, Object> context) {
		// TODO use composition over existing property getter for context restriction -
		// needed for traversal scoping

		// Set up file/repository pattern lists
		final String sFilePatterns = (String) context.get(PROPERTY_FILECONTEXT);
		final String[] filePatterns = (sFilePatterns != null && sFilePatterns.trim().length() != 0)
				? sFilePatterns.split(",")
				: null;
		final List<String> fplist = (filePatterns != null) ? Arrays.asList(filePatterns) : null;

		final String sRepoPatterns = (String) context.get(PROPERTY_REPOSITORYCONTEXT);
		final String[] repoPatterns = (sRepoPatterns != null && sRepoPatterns.trim().length() != 0)
				? sRepoPatterns.split(",")
				: null;
		final List<String> rplist = (repoPatterns != null) ? Arrays.asList(repoPatterns) : null;

		/*
		 * Create suppliers for allFiles and allOf, so we may support file/repository
		 * contexts.
		 *
		 * TODO - currently limited to file first (no subtree, no derived allOf support
		 * yet).
		 */
		final GlobPatternTimeAwareAllFiles innerAllFiles = new GlobPatternTimeAwareAllFiles(rplist, fplist);
		setAllFiles(innerAllFiles);
		setAllOf(new TypeFirstAllOf(innerAllFiles, this));
	}
}
