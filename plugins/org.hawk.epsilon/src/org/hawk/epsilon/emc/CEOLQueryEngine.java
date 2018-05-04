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
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CEOLQueryEngine extends EOLQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(CEOLQueryEngine.class);

	private interface AllOf {
		void addAllOf(IGraphNode typeNode, final String typeorkind, Collection<Object> nodes);
	}

	public class DerivedAllOf implements AllOf {
		private static final String DEDGE_PREFIX = "allof_";

		private final List<String> rplist;
		private final String subtreeRootPath;
		private final MemoizedSupplier<Set<ModelElementNode>> roots = new MemoizedSupplier<>(this::computeRoots);

		public DerivedAllOf(List<String> rplist, String subtreeRootPath) {
			this.rplist = rplist;
			this.subtreeRootPath = subtreeRootPath;
		}

		@Override
		public void addAllOf(IGraphNode typeNode, String typeorkind, Collection<Object> nodes) {
			// Add derived edge if it doesn't exist
			final TypeNode tn = new TypeNode(typeNode);
			final String dedgeName = DEDGE_PREFIX + tn.getTypeName();
			final Slot slot = tn.getSlot(dedgeName);
			if (slot == null) {
				final boolean isMany = true;
				final boolean isOrdered = false;
				final boolean isUnique = true;

				// TODO check with subtypes
				indexer.addDerivedAttribute(tn.getMetamodelURI(), tn.getTypeName(), dedgeName,
						tn.getTypeName(), isMany, isOrdered, isUnique, EOLQueryEngine.TYPE,
						"return self.closure(e|e.eContainers);");
			}

			for (ModelElementNode root : roots.get()) {
				for (IGraphEdge e : root.getNode().getIncomingWithType(ModelElementNode.DERIVED_EDGE_PREFIX + dedgeName)) {
					final IGraphNode derivedFeatureNode = e.getStartNode();
					final IGraphNode sourceElementNode = derivedFeatureNode.getIncoming().iterator().next().getStartNode();
					nodes.add(new GraphNodeWrapper(sourceElementNode, CEOLQueryEngine.this));
				}
			}
		}

		private Set<ModelElementNode> computeRoots() {
			final GraphWrapper gw = new GraphWrapper(graph);
			final Set<FileNode> allRootFileNodes = gw.getFileNodes(rplist, Collections.singletonList(subtreeRootPath));
			final Set<ModelElementNode> rootNodes = new HashSet<>();

			/*
			 * We cannot use getRootModelElements(), because that returns *global* roots
			 * (elements which are not contained within *any* other, even in another file.)
			 *
			 * We need local roots: elements which are not contained by any other element in
			 * the same file. Due to the complications with proxy references and container
			 * edges, these can only be figured out after indexing is done.
			 *
			 * For the sake of efficiency, we assume that a file has exactly one 'local'
			 * root: this means it's enough to go to the first element and go up in the
			 * containment tree until we either we find a global root, or the container of
			 * the element is in another file.
			 */
			for (FileNode fn : allRootFileNodes) {
				Iterator<ModelElementNode> itElems = fn.getModelElements().iterator();
				if (itElems.hasNext()) {
					ModelElementNode first = itElems.next();
					rootNodes.add(first.getLocalRoot());
				}
			}

			return rootNodes;
		}
	}

	/**
	 * Finds all the instances of a type by starting from the files, and then
	 * checking their entire contents by type. Faster for querying small files in
	 * large graphs.
	 */
	public class FileFirstAllOf implements AllOf {
		@Override
		public void addAllOf(IGraphNode typeNode, String typeorkind, Collection<Object> nodes) {
			for (IGraphNode rawFileNode : fileSupplier.get()) {
				final FileNode f = new FileNode(rawFileNode);
				for (ModelElementNode me : f.getModelElements()) {
					if (me.isOfKind(typeNode)) {
						nodes.add(new GraphNodeWrapper(me.getNode(), CEOLQueryEngine.this));
					}
				}
			}
		}
	}

	/**
	 * Finds all the instances of a type by starting from the types, and then
	 * filtering their contents by file. Faster for rare types in large subtrees.
	 */
	public class TypeFirstAllOf implements AllOf {
		@Override
		public void addAllOf(IGraphNode typeNode, String typeorkind, Collection<Object> nodes) {
			final Set<IGraphNode> files = fileSupplier.get();
			for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
				IGraphNode node = n.getStartNode();
				for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
					if (files.contains(e.getEndNode())) {
						nodes.add(new GraphNodeWrapper(node, CEOLQueryEngine.this));
					}
				}
			}
		}
	}

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
	private AllOf allOf;
	private boolean isTraversalScopingEnabled;

	public void setContext(Map<String, Object> context) {
		if (context == null) {
			context = Collections.emptyMap();
		}

		// Set up file/repository pattern lists
		final String sFilePatterns = (String) context.get(PROPERTY_FILECONTEXT);
		final String sRepoPatterns = (String) context.get(PROPERTY_REPOSITORYCONTEXT);
		final String[] filePatterns = (sFilePatterns != null && sFilePatterns.trim().length() != 0)	? sFilePatterns.split(",") : null;
		final String[] repoPatterns = (sRepoPatterns != null && sRepoPatterns.trim().length() != 0) ? sRepoPatterns.split(",") : null;
		List<String> fplist = (filePatterns != null) ? Arrays.asList(filePatterns) : null;
		List<String> rplist = (repoPatterns != null) ? Arrays.asList(repoPatterns) : null;

		// Set up basic options
		name = (String) context.getOrDefault(EOLQueryEngine.PROPERTY_NAME, "Model");
		setDefaultNamespaces((String) context.get(PROPERTY_DEFAULTNAMESPACES));
		isTraversalScopingEnabled = Boolean.parseBoolean((String)context.getOrDefault(PROPERTY_ENABLE_TRAVERSAL_SCOPING, "false"));
		if (isTraversalScopingEnabled) {
			LOGGER.debug("Full Traversal Scoping ENABLED");
		}

		// Decide on suppliers for various operations
		final boolean filterByFileFirst = Boolean.parseBoolean((String) context.getOrDefault(PROPERTY_FILEFIRST, "false"));
		final String sSubtreeRootFile = (String) context.get(PROPERTY_SUBTREECONTEXT);
		final boolean allOfDerived = Boolean.parseBoolean((String) context.getOrDefault(PROPERTY_SUBTREE_DERIVEDALLOF, "false"));

		if (sSubtreeRootFile == null) {
			fileSupplier = new MemoizedSupplier<>(new GlobPatternFileSupplier(fplist, rplist));
		} else {
			fileSupplier = new MemoizedSupplier<>(new SubtreeFileSupplier(sSubtreeRootFile, rplist));
		}

		if (sSubtreeRootFile != null && allOfDerived) {
			allOf = new DerivedAllOf(rplist, sSubtreeRootFile);
		} else if (filterByFileFirst) {
			allOf = new FileFirstAllOf();
		} else {
			allOf = new TypeFirstAllOf();
		}

		// Final preparations
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
		allOf.addAllOf(typeNode, typeorkind, nodes);
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
