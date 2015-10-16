/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalHawkResourceUpdater implements IGraphChangeListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalHawkResourceUpdater.class);

	private final LocalHawkResourceImpl resource;

	/** Nodes removed since the last call to either {@link #changeSuccess()} or {@link #changeFailure()}. */
	private final Set<IGraphNode> removedNodes = new HashSet<>();

	/** Nodes updated since the last call to either {@link #changeSuccess()} or {@link #changeFailure()}. */
	private final Set<IGraphNode> updatedNodes = new HashSet<>();

	public LocalHawkResourceUpdater(LocalHawkResourceImpl r) {
		this.resource = r;
	}

	@Override
	public String getName() {
		return "Local Hawk resource " + resource.getURI();
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		// ignore
	}

	@Override
	public void synchroniseStart() {
		// do nothing
	}

	@Override
	public void synchroniseEnd() {
		// do nothing
	}

	@Override
	public void changeStart() {
		// do nothing
	}

	@Override
	public void changeSuccess() {
		for (IGraphNode removedNode : removedNodes) {
			resource.removeNode(removedNode.getId().toString());
		}
		updatedNodes.removeAll(removedNodes);

		final List<ModelElementNode> elems = new ArrayList<>();
		for (IGraphNode updatedNode : updatedNodes) {
			final ModelElementNode updatedME = new ModelElementNode(updatedNode);
			elems.add(updatedME);
		}
		try (IGraphTransaction tx = resource.beginGraphTransaction()) {
			resource.createOrUpdateEObjects(elems);
			tx.success();
		} catch (Exception e) {
			LOGGER.error("Error while updating resource", e);
			removedNodes.clear();
			updatedNodes.clear();
		}
	}

	@Override
	public void changeFailure() {
		removedNodes.clear();
		updatedNodes.clear();
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		// do nothing
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		// do nothing
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		// do nothing
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		// do nothing
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element, IGraphNode elementNode, boolean isTransient) {
		// do nothing - we'll fetch it when we need it
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode, boolean isTransient) {
		// if we have it, mark it for deletion
		if (isTransient) return;

		final String id = elementNode.getId().toString();
		final EObject existing = resource.getNodeEObject(id);
		if (existing != null) {
			removedNodes.add(elementNode);
		}
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s, IHawkObject eObject, String attrName, Object oldValue,	Object newValue, IGraphNode elementNode, boolean isTransient) {
		if (isTransient) return;
		markUpdatedIfExisting(elementNode);
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s, IHawkObject eObject, String attrName, IGraphNode elementNode, boolean isTransient) {
		if (isTransient) return;
		markUpdatedIfExisting(elementNode);
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel, boolean isTransient) {
		if (isTransient) return;
		markUpdatedIfExisting(source);
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel, boolean isTransient) {
		if (isTransient) return;
		markUpdatedIfExisting(source);
	}

	private void markUpdatedIfExisting(IGraphNode elementNode) {
		final String id = elementNode.getId().toString();
		final EObject existing = resource.getNodeEObject(id);
		if (existing != null) {
			updatedNodes.add(elementNode);
		}
	}
}