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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;

public class DerivedAllOf implements AllOf {
	private static final String DEDGE_PREFIX = "allof_";

	private final IModelIndexer indexer;
	private final EOLQueryEngine engine;

	private final List<String> rplist;
	private final String subtreeRootPath;
	private final MemoizedSupplier<Set<ModelElementNode>> roots = new MemoizedSupplier<>(this::computeRoots);

	public DerivedAllOf(IModelIndexer indexer, EOLQueryEngine engine, List<String> rplist, String subtreeRootPath) {
		this.rplist = rplist;
		this.subtreeRootPath = subtreeRootPath;
		this.indexer = indexer;
		this.engine = engine;
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
				nodes.add(new GraphNodeWrapper(sourceElementNode, engine));
			}
		}
	}

	private Set<ModelElementNode> computeRoots() {
		final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
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