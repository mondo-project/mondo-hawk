/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - extract queries into GraphWrapper
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.pgetters.CGraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.FileNodeWrapper;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CEOLQueryEngine extends EOLQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(CEOLQueryEngine.class);

	/**
	 * Finds all the files in scope by going to the root elements of all the files
	 * and seeing if they are contained within the root of the subtree.
	 */
	public class SubtreeFileSupplier implements Supplier<Set<IGraphNode>> {
		private final String subtreeRootPath;
		private final List<String> rplist;

		public SubtreeFileSupplier(String subtreeRootPath, List<String> rplist) {
			this.subtreeRootPath = subtreeRootPath;
			this.rplist = rplist;
		}

		@Override
		public Set<IGraphNode> get() {
			final GraphWrapper gw = new GraphWrapper(graph);
			try (IGraphTransaction tx = graph.beginTransaction()) {
				final Set<IGraphNode> fileNodes = new HashSet<>();

				final Set<FileNode> allFileNodes = gw.getFileNodes(rplist, null);
				for (FileNode fn : allFileNodes) {
					Iterator<ModelElementNode> itElems = fn.getModelElements().iterator();

					if (itElems.hasNext()) {
						ModelElementNode first = itElems.next();
						if (rplist == null) {
							if (first.isContainedWithin(null, subtreeRootPath)) {
								fileNodes.add(fn.getNode());
							}
						} else {
							for (String repo : rplist) {
								if (first.isContainedWithin(repo, subtreeRootPath)) {
									fileNodes.add(fn.getNode());
									break;
								}
							}
						}
					}
				}

				tx.success();
				return fileNodes;
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				return Collections.emptySet();
			}
		}
	}

	/**
	 * Finds all the relevant files through glob-like patterns on the file index.
	 */
	public class GlobPatternFileSupplier implements Supplier<Set<IGraphNode>> {
		private List<String> fplist, rplist;

		public GlobPatternFileSupplier(List<String> fplist, List<String> rplist) {
			this.fplist = fplist;
			this.rplist = rplist;
		}

		@Override
		public synchronized Set<IGraphNode> get() {
			final GraphWrapper gw = new GraphWrapper(graph);
			try (IGraphTransaction tx = graph.beginTransaction()) {
				final Set<FileNode> fileNodes = gw.getFileNodes(rplist, fplist);
				final Set<IGraphNode> rawFileNodes = new HashSet<>();
				for (FileNode fn : fileNodes) {
					rawFileNodes.add(fn.getNode());
				}
				tx.success();
				return rawFileNodes;
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				return Collections.emptySet();
			}
		}
	}

	/**
	 * Wraps around an existing supplier, which will be used once and then have its
	 * value stored for faster access later.
	 */
	private static class MemoizedSupplier<T> implements Supplier<T> {
		private Supplier<T> delegate;
		private boolean initialized;

		public MemoizedSupplier(Supplier<T> original) {
			this.delegate = original;
		}

		@Override
		public T get() {
			if (!initialized) {
				T value = delegate.get();
				delegate = () -> value;
				initialized = true;
			}
			return delegate.get();
		}
	}

	private Supplier<Set<IGraphNode>> fileSupplier;
	private boolean isTraversalScopingEnabled;
	private boolean filterByFileFirst;

	public void setContext(Map<String, Object> context) {
		if (context == null) {
			context = Collections.emptyMap();
		}

		final String sFilePatterns = (String) context.get(PROPERTY_FILECONTEXT);
		final String sRepoPatterns = (String) context.get(PROPERTY_REPOSITORYCONTEXT);
		final String sSubtreeRootFile = (String) context.get(PROPERTY_SUBTREECONTEXT);
		setDefaultNamespaces((String) context.get(PROPERTY_DEFAULTNAMESPACES));
		isTraversalScopingEnabled = Boolean.parseBoolean((String)context.getOrDefault(PROPERTY_ENABLE_TRAVERSAL_SCOPING, "false"));
		filterByFileFirst = Boolean.parseBoolean((String) context.getOrDefault(PROPERTY_FILEFIRST, "false"));
		name = (String) context.getOrDefault(EOLQueryEngine.PROPERTY_NAME, "Model");

		final String[] filePatterns = (sFilePatterns != null && sFilePatterns.trim().length() != 0)	? sFilePatterns.split(",") : null;
		final String[] repoPatterns = (sRepoPatterns != null && sRepoPatterns.trim().length() != 0) ? sRepoPatterns.split(",") : null;
		List<String> fplist = (filePatterns != null) ? Arrays.asList(filePatterns) : null;
		List<String> rplist = (repoPatterns != null) ? Arrays.asList(repoPatterns) : null;

		if (sSubtreeRootFile == null) {
			fileSupplier = new MemoizedSupplier<>(new GlobPatternFileSupplier(fplist, rplist));
		} else {
			fileSupplier = new MemoizedSupplier<>(new SubtreeFileSupplier(sSubtreeRootFile, rplist));
		}

		if (isTraversalScopingEnabled) {
			LOGGER.debug("Full Traversal Scoping ENABLED");
		}

		if (propertyGetter == null) {
			propertyGetter = new CGraphPropertyGetter(graph, this);
		}
	}

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * objects, limited by the files in scope.
	 */
	@Override
	public Collection<?> allContents() {
		final Set<GraphNodeWrapper> allContents = new HashSet<GraphNodeWrapper>();

		for (IGraphNode rawFileNode : fileSupplier.get()) {
			final FileNode f = new FileNode(rawFileNode);
			for (ModelElementNode me : f.getModelElements()) {
				GraphNodeWrapper wrapper = new GraphNodeWrapper(me.getNode(), this);
				allContents.add(wrapper);
			}
		}

		return allContents;
	}

	@Override
	public Collection<Object> getAllOf(IGraphNode typeNode, final String typeorkind) {
		Collection<Object> nodes = createAllOfCollection(typeNode);

		final Set<IGraphNode> files = fileSupplier.get();
		if (filterByFileFirst) {
			for (IGraphNode rawFileNode : files) {
				final FileNode f = new FileNode(rawFileNode);
				for (ModelElementNode me : f.getModelElements()) {
					if (me.isOfKind(typeNode)) {
						nodes.add(new GraphNodeWrapper(me.getNode(), this));
					}
				}
			}
		} else {
			for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
				IGraphNode node = n.getStartNode();

				for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
					if (files.contains(e.getEndNode())) {
						nodes.add(new GraphNodeWrapper(node, this));
					}
				}
			}
		}

		return nodes;
	}

	@Override
	public Set<FileNodeWrapper> getFiles() {
		Set<FileNodeWrapper> allFNW = new HashSet<>();
		for (IGraphNode rawNode : fileSupplier.get()) {
			allFNW.add(new FileNodeWrapper(new FileNode(rawNode), this));
		}
		return allFNW;
	}

	public Set<IGraphNode> getRawFileNodes() {
		return fileSupplier.get();
	}

	public boolean isTraversalScopingEnabled() {
		return isTraversalScopingEnabled;
	}
}
