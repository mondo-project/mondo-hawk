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

import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

public interface HawkResource extends Resource {

	boolean hasChildren(EObject o);

	/**
	 * Fetches attributes for the specified objects, if they haven't been
	 * fetched yet.
	 * 
	 * @param idToEObject
	 *            Map of ID to EObject.
	 */
	void fetchAttributes(Map<String, EObject> idToEObject) throws Exception;

	/**
	 * Retrieves an EObject by container resource + URI fragment.
	 */
	EObject fetchNode(HawkResource containerResource, String uriFragment, boolean mustFetchAttributes) throws Exception;

	/**
	 * Retrieves a single EObject by its graph node identifier.
	 */
	EObject fetchNode(String id, boolean mustFetchAttributes) throws Exception;

	/**
	 * Returns the graph node ID for an EObject in the resource.
	 */
	String getEObjectNodeID(EObject obj);

	EList<EObject> fetchNodes(List<String> ids, boolean mustFetchAttributes) throws Exception;

	EList<EObject> fetchNodes(EClass eClass, boolean mustFetchAttributes) throws Exception;

	List<Object> fetchValuesByEClassifier(EClassifier dataType) throws Exception;

	Map<EObject, Object> fetchValuesByEStructuralFeature(EStructuralFeature feature) throws Exception;

	Map<EClass, List<EStructuralFeature>> fetchTypesWithEClassifier(EClassifier dataType) throws Exception;

	List<String> getRegisteredMetamodels() throws Exception;

	List<String> getRegisteredTypes(String metamodelURI) throws Exception;

	boolean addSyncEndListener(Runnable r);

	boolean removeSyncEndListener(Runnable r);

	/**
	 * Adds a new listener for changes in the underlying model, as reported by
	 * Hawk.
	 * 
	 * @throws UnsupportedOperationException
	 *             The functionality is not offered for this resource yet.
	 */
	boolean addChangeListener(HawkResourceChangeListener l);

	/**
	 * Adds a new listener for changes in the underlying model, as reported by
	 * Hawk.
	 * 
	 * @throws UnsupportedOperationException
	 *             The functionality is not offered for this resource yet.
	 */
	boolean removeChangeListener(HawkResourceChangeListener l);
}
