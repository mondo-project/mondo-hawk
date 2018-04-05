/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
import org.hawk.core.query.IQueryEngine;

public interface HawkResource extends Resource {

	/**
	 * Returns <code>true</code> if the object has any children, without
	 * retrieving the actual value.
	 */
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

	/**
	 * Performs a raw query on the underlying graph and returns the result
	 * as-is. Does not load results.
	 *
	 * @param queryLanguage
	 *            Name of the query language, as reported by the appropriate
	 *            {@link IQueryEngine#getType()} method.
	 * @param query
	 *            String representing the query itself.
	 * @param context
	 *            Map from the query options to their values: see
	 *            {@link IQueryEngine} for some common keys.
	 */
	Object performRawQuery(String queryLanguage, String query, Map<String, Object> context) throws Exception;

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

	/**
	 * Marks an object as changed, indicating that it shouldn't be garbage collected.
	 */
	void markChanged(final EObject eob);
}
