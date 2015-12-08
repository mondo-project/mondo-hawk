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
package org.hawk.emfresource.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalHawkResourceUpdater implements IGraphChangeListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalHawkResourceUpdater.class);

	private final LocalHawkResourceImpl resource;

	/** Root nodes added since the last call to either {@link #changeSuccess()} or {@link #changeFailure()}. We need the class for removing */
	private final Map<IGraphNode, EClass> addedRootNodes = new HashMap<>();

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
		for (Runnable r : resource.getSyncEndListeners()) {
			r.run();
		}
	}

	@Override
	public void changeStart() {
		// do nothing
	}

	@Override
	public void changeSuccess() {
		for (IGraphNode removedNode : removedNodes) {
			resource.removeNode(removedNode.getId().toString());
			resource.setModified(true);
		}
		updatedNodes.removeAll(removedNodes);

		final List<ModelElementNode> elems = new ArrayList<>();
		for (IGraphNode addedNode : addedRootNodes.keySet()) {
			final ModelElementNode addedME = new ModelElementNode(addedNode);
			elems.add(addedME);
			resource.setModified(true);
		}
		for (IGraphNode updatedNode : updatedNodes) {
			final ModelElementNode updatedME = new ModelElementNode(updatedNode);
			elems.add(updatedME);
			resource.setModified(true);
		}
		try (IGraphTransaction tx = resource.beginGraphTransaction()) {
			resource.createOrUpdateEObjects(elems);
			tx.success();
		} catch (Exception e) {
			LOGGER.error("Error while updating resource", e);
			addedRootNodes.clear();
			removedNodes.clear();
			updatedNodes.clear();
		}
	}

	@Override
	public void changeFailure() {
		addedRootNodes.clear();
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
		// new model elements are assumed to be roots unless proven otherwise
		if (isTransient) return;

		final IHawkClassifier type = element.getType();
		final Registry packageRegistry = resource.getResourceSet().getPackageRegistry();
		final EPackage ePackage = packageRegistry.getEPackage(type.getPackageNSURI());
		final EClass eClass = (EClass)ePackage.getEClassifier(type.getName());
		addedRootNodes.put(elementNode, eClass);
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

		final EClass sourceClass = addedRootNodes.get(source);
		if (sourceClass != null) {
			// in a container reference, the source is not a root
			final EReference eRef = (EReference)sourceClass.getEStructuralFeature(edgelabel);
			if (eRef != null && eRef.isContainer()) {
				addedRootNodes.remove(source);
			}
		}

		final EClass destClass = addedRootNodes.get(destination);
		if (destClass != null) {
			// in a containment reference, the target is not a root
			final EReference eRef = (EReference)destClass.getEStructuralFeature(edgelabel);
			if (eRef != null && eRef.isContainment()) {
				addedRootNodes.remove(destination);
			}
		}
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