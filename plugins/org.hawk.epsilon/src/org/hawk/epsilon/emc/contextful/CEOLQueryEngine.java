/*******************************************************************************
 * Copyright (c) 2011-2019 The University of York, Aston University.
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
package org.hawk.epsilon.emc.contextful;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.pgetters.CGraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.FileNodeWrapper;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CEOLQueryEngine extends EOLQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(CEOLQueryEngine.class);

	protected class IGraphIterablesCollection implements Collection<GraphNodeWrapper> {
		private final List<IGraphIterable<ModelElementNode>> iterables;
		private Set<GraphNodeWrapper> elements;

		protected IGraphIterablesCollection(List<IGraphIterable<ModelElementNode>> iterables) {
			this.iterables = iterables;
		}

		@Override
		public int size() {
			int total = 0;
			for (IGraphIterable<ModelElementNode> graphIterable : iterables) {
				total += graphIterable.size();
			}
			return total;
		}

		@Override
		public boolean isEmpty() {
			for (IGraphIterable<ModelElementNode> graphIterable : iterables) {
				if (graphIterable.size() > 0) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean contains(Object o) {
			return getElements().contains(o);
		}

		private Set<GraphNodeWrapper> getElements() {
			if (elements == null) {
				elements = new HashSet<>();
				for (IGraphIterable<ModelElementNode> iterable : iterables) {
					for (ModelElementNode e : iterable) {
						elements.add(new GraphNodeWrapper(e.getNode(), CEOLQueryEngine.this));
					}
				}
			}
			return elements;
		}

		@Override
		public Iterator<GraphNodeWrapper> iterator() {
			return getElements().iterator();
		}

		@Override
		public Object[] toArray() {
			return getElements().toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return getElements().toArray(a);
		}

		@Override
		public boolean add(GraphNodeWrapper e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return getElements().containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends GraphNodeWrapper> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	private Function<IGraphNode, Iterable<? extends IGraphNode>> allFiles;
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
			final Supplier<Set<IGraphNode>> supplier = new MemoizedSupplier<>(new GlobPatternFileSupplier(graph, fplist, rplist));
			allFiles = (dummy) -> supplier.get();
		} else {
			final Supplier<Set<IGraphNode>> supplier = new MemoizedSupplier<>(new SubtreeFileSupplier(graph, sSubtreeRootFile, rplist));
			allFiles = (dummy) -> supplier.get();
		}

		if (sSubtreeRootFile != null && allOfDerived) {
			allOf = new DerivedAllOf(indexer, this, rplist, sSubtreeRootFile);
		} else if (filterByFileFirst) {
			allOf = new FileFirstAllOf(allFiles, this);
		} else {
			allOf = new TypeFirstAllOf(allFiles, this);
		}

		/*
		 * This class won't work without this property getter, so we are enforcing its
		 * use.
		 */
		propertyGetter = new CGraphPropertyGetter(graph, this);
	}

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * objects, limited by the files in scope.
	 */
	@Override
	public Collection<?> allContents() {
		final Iterable<? extends IGraphNode> files = allFiles.apply(null);
		if (!files.iterator().hasNext()) {
			return Collections.emptyList();
		}

		final Iterator<? extends IGraphNode> itFiles = files.iterator();
		final FileNode firstFile = new FileNode(itFiles.next());
		final Iterable<ModelElementNode> firstFileElements = firstFile.getModelElements();

		if (firstFileElements instanceof IGraphIterable) {
			// Backend supports a more efficient #size operation: expose it through the console
			final List<IGraphIterable<ModelElementNode>> iterables = new ArrayList<>();
			iterables.add((IGraphIterable<ModelElementNode>) firstFileElements);
			while (itFiles.hasNext()) {
				final FileNode fn = new FileNode(itFiles.next());
				iterables.add((IGraphIterable<ModelElementNode>) fn.getModelElements());
			}
			return new IGraphIterablesCollection(iterables);
		}

		// Backend does not support the more efficient #size operation		
		final Set<GraphNodeWrapper> allContents = new HashSet<>();
		for (ModelElementNode elem : firstFileElements) {
			allContents.add(new GraphNodeWrapper(elem.getNode(), this));
		}
		while (itFiles.hasNext()) {
			final FileNode fn = new FileNode(itFiles.next());
			for (ModelElementNode elem : fn.getModelElements()) {
				allContents.add(new GraphNodeWrapper(elem.getNode(), this));
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
		for (IGraphNode rawNode : allFiles.apply(null)) {
			allFNW.add(new FileNodeWrapper(new FileNode(rawNode), this));
		}
		return allFNW;
	}

	public Set<IGraphNode> getRawFileNodes() {
		final HashSet<IGraphNode> nodes = new HashSet<>();
		for (IGraphNode n : allFiles.apply(null)) {
			nodes.add(n);
		}
		return nodes;
	}

	public boolean isTraversalScopingEnabled() {
		return isTraversalScopingEnabled;
	}
	
	@Override
	public String getHumanReadableName() {
		return "CEOL Query Engine";
	}
}
