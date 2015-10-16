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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores which attributes or references are to be lazily resolved, and resolves
 * them when they are needed. This is a simplified implementation of the
 * LazyResolver class in mondo-integration, which does not need to lazily
 * resolve attributes.
 */
public class LazyResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(LazyResolver.class);

	private final LocalHawkResourceImpl resource;

	public LazyResolver(LocalHawkResourceImpl resource) {
		this.resource = resource;
	}

	/** Objects for which we don't know their attributes yet (and their IDs). */
	private Map<EObject, String> pendingAttrs = new IdentityHashMap<>();

	/** Pending EReferences to be fetched. */
	private Map<EObject, Map<EReference, EList<Object>>> pendingRefs = new IdentityHashMap<>();

	/**
	 * Resolves the referenced feature, if it has been marked as lazy. After
	 * fetching it from the network, it will update the object accordingly so
	 * the actual call to {@link EObject#eGet(EStructuralFeature)} will retrieve
	 * the appropriate value.
	 */
	public void resolve(EObject object, EStructuralFeature feature) {
		try {
			if (feature instanceof EReference) {
				Map<EReference, EList<Object>> pending = pendingRefs.get(object);
				if (pending != null) {
					EList<Object> pendingObjects = pending.remove(feature);
					if (pendingObjects != null) {
						resolvePendingReference(object, (EReference) feature, pending, pendingObjects);
					}
				}
			} else if (feature instanceof EAttribute) {
				throw new UnsupportedOperationException();
			}
		} catch (Exception e) {
			LOGGER.error("Error while resolving lazy reference", e);
		}
	}

	/**
	 * Returns <code>true</code> if the fetch for feature is pending,
	 * <code>false</code> otherwise.
	 */
	public boolean isPending(EObject object, EStructuralFeature feature) {
		if (feature instanceof EReference) {
			Map<EReference, EList<Object>> pending = pendingRefs.get(object);
			if (pending != null) {
				return pending.containsKey(feature);
			}
		} else if (feature instanceof EAttribute) {
			return pendingAttrs.containsKey(object);
		}
		return false;
	}

	/**
	 * Adds a reference to the store, to be fetched later on demand.
	 * 
	 * @param eob
	 *            EObject whose reference will be fetched later on.
	 * @param feature
	 *            Reference to fetch.
	 * @param value
	 *            Mixed list of {@link String}s (from ID-based references) or
	 *            {@link EObject}s (from position-based references).
	 */
	public void markLazyReferences(EObject eob, EReference feature, EList<Object> value) {
		Map<EReference, EList<Object>> allPending = pendingRefs.get(eob);
		if (allPending == null) {
			allPending = new IdentityHashMap<>();
			pendingRefs.put(eob, allPending);
		}
		allPending.put(feature, value);
	}

	/**
	 * Removes a reference to the store.
	 * 
	 * @param eob
	 *            EObject whose reference will no longer be fetched later on.
	 * @param feature
	 *            Reference to be removed.
	 */
	public void unmarkLazyReferences(EObject eob, EReference ref) {
		Map<EReference, EList<Object>> allPending = pendingRefs.get(eob);
		if (allPending != null) {
			allPending.remove(ref);
		}
	}

	private void resolvePendingReference(EObject object, EReference feature, Map<EReference, EList<Object>> pending,
			EList<Object> ids) throws Exception {
		// This is a pending ref: resolve its proper value
		final EList<Object> eObjs = resolveReference(object, feature, ids);
		if (feature.isMany()) {
			object.eSet(feature, eObjs);
		} else if (!eObjs.isEmpty()) {
			object.eSet(feature, eObjs.get(0));
		}
	}

	private EList<Object> resolveReference(EObject source, EReference feature, EList<Object> targets) throws Exception {
		final List<String> ids = new ArrayList<>();
		addAllStrings(targets, ids);
		final EList<EObject> resolved = resource.fetchNodes(ids);

		// Replace all old String elements with their corresponding EObjects
		final EList<Object> result = new BasicEList<>();
		int iResolved = 0;
		for (int iElem = 0; iElem < targets.size(); iElem++) {
			final Object elem = targets.get(iElem);
			if (elem instanceof String) {
				final EObject eob = resolved.get(iResolved++);
				if (eob == null) {
					LOGGER.warn("Failed to resolve lazy reference to node {}: deleted without notification?", elem);
				} else {
					result.add(eob);
				}
			} else {
				result.add(elem);
			}
		}

		return result;
	}

	private void addAllStrings(EList<Object> source, final List<String> target) {
		for (Object elem : source) {
			if (elem instanceof String) {
				target.add((String) elem);
			}
		}
	}

	/**
	 * Returns a list of {@link String} identifiers and {@link EObject}s if the
	 * referencde <code>r</code> is pending for the object <code>o</code>, or
	 * <code>null</code> otherwise.
	 */
	public EList<Object> getPending(EObject o, EReference r) {
		Map<EReference, EList<Object>> allPending = pendingRefs.get(o);
		if (allPending != null) {
			return allPending.get(r);
		}
		return null;
	}

}