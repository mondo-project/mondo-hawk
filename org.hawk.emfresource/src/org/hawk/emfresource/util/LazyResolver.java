/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.emfresource.HawkResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>
 * Stores which attributes or references are to be lazily resolved. Lazy
 * attributes are resolved and {@link EObject#eSet(EStructuralFeature, Object)}
 * is invoked for them on the first fetch.
 * </p>
 *
 * <p>
 * Lazy references are resolved on the first fetch, but <code>eSet</code> is
 * <emph>not</emph> called in order to keep weak references between objects.
 * Users of this class should make sure that all the EObject methods involving
 * references of some sort (e.g. eGet, eIsSet, eContainer, eContainmentFeature,
 * eResource) are properly intercepted to fall back on this class.
 * </p>
 */
public class LazyResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(LazyResolver.class);

	private final HawkResource resource;

	private final class DirtyObjectMarkingEList<T> extends BasicEList<T> {
		private static final long serialVersionUID = 1L;
		private final EObject eob;

		private DirtyObjectMarkingEList(Collection<T> collection, EObject source) {
			super(collection);
			this.eob = source;
		}

		@Override
		protected void didChange() {
			resource.markChanged(eob);
		}
	}

	/**
	 * Intermediary that resolves upon demand the list, and can answer some
	 * queries without actually retrieving the value.
	 */
	private class LazyEListWrapper {
		private boolean isPending = true;
		private EList<Object> backingEList;

		private LazyEListWrapper(EList<Object> pending) {
			this.backingEList = pending;
		}

		public EList<Object> get(EObject object, EReference feature, Map<EReference, LazyEListWrapper> pending, boolean greedyReferences, boolean mustFetchAttributes) {
			if (isPending) {
				try {
					resolvePendingReference(object, feature, pending, backingEList, greedyReferences, mustFetchAttributes);
				} catch (Exception e) {
					LOGGER.error("Error while resolving reference: " + e.getMessage(), e);
					return new BasicEList<>();
				}
			}
			return backingEList;
		}

		public boolean add(Object value) {
			return backingEList.add(value);
		}

		public boolean remove(Object value) {
			return backingEList.remove(value);
		}

		public EList<Object> getWrapped() {
			return backingEList;
		}

		private void resolvePendingReference(EObject object, EReference feature, Map<EReference, LazyEListWrapper> pending, EList<Object> ids, boolean greedyReferences, boolean mustFetchAttributes) throws Exception {
			@SuppressWarnings("unused") EList<EObject> fetched = null;
			if (greedyReferences) {
				// The loading mode says we should prefetch all referenced nodes
				final List<String> childrenIds = new ArrayList<>();
				for (LazyEListWrapper elems : pending.values()) {
					addAllStrings(elems.getWrapped(), childrenIds);
				}
				addAllStrings(ids, childrenIds);
				fetched = resource.fetchNodes(childrenIds, mustFetchAttributes);
			}
			resolveReference(object, feature, ids, mustFetchAttributes);
		}

		private EList<Object> resolveReference(final EObject source, EReference feature, EList<Object> targets, boolean mustFetchAttributes) throws Exception {
			final List<String> ids = new ArrayList<>();
			addAllStrings(targets, ids);
			final EList<EObject> resolved = resource.fetchNodes(ids, mustFetchAttributes);

			// Replace all old String elements with their corresponding EObjects
			final List<Object> result = new ArrayList<>();
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

			backingEList = new DirtyObjectMarkingEList<>(result, source);
			isPending = false;

			if (feature.isContainment()) {
				/*
				 * If this was a containment reference, the target can't be at the
				 * root level of its resource anymore.
				 */
				for (Object target : result) {
					final EObject eobTarget = (EObject)target;
					if (eobTarget.eContainer() != null) {
						final Resource eResource = eobTarget.eResource();
						if (eResource != null) {
							eResource.getContents().remove(eobTarget);
						}
					}
					eObjectToContainer.put(eobTarget, new LazyEContainment(source, feature));
				}
			} else if (feature.isContainer()) {
				/*
				 * If this was a container reference, the eob can't be at the
				 * root level of its resource anymore.
				 */
				if (source.eContainer() != null) {
					source.eResource().getContents().remove(source);
				}
				for (Object target : result) {
					final EObject eobTarget = (EObject)target;
					eObjectToContainer.put(source, new LazyEContainment(eobTarget, feature.getEOpposite()));
				}
			}

			return backingEList;
		}

		private void addAllStrings(EList<Object> source, final List<String> target) {
			for (Object elem : source) {
				if (elem instanceof String) {
					target.add((String) elem);
				}
			}
		}
	}

	private static class LazyEContainment {
		private final EObject eContainer;
		private final EReference eContainmentReference;

		private LazyEContainment(EObject eContainer, EReference eContainmentReference) {
			this.eContainer = eContainer;
			this.eContainmentReference = eContainmentReference;
		}

		public EObject getContainer() {
			return eContainer;
		}

		public EReference getContainmentReference() {
			return eContainmentReference;
		}
	}

	public LazyResolver(HawkResource resource) {
		this.resource = resource;
	}

	/** Objects for which we don't know their attributes yet (and their IDs). */
	private Cache<EObject, String> eObjectWithPendingAttrs = CacheBuilder.newBuilder().weakKeys().build();

	/** Lazy EReferences kept in memory at the moment. */
	private Cache<EObject, Map<EReference, LazyEListWrapper>> eObjectToLazyRefs = CacheBuilder.newBuilder().weakKeys().build();

	/** Containers and containment features for lazily fetched objects. */
	private Cache<EObject, LazyEContainment> eObjectToContainer = CacheBuilder.newBuilder().weakKeys().build();

	/**
	 * Resolves all pending features in the object.
	 */
	public void resolve(EObject object, boolean mustFetchAttributes) {
		try {
			resolveAttributes(object);
			Map<EReference, LazyEListWrapper> refs = eObjectToLazyRefs.getIfPresent(object);
			if (refs != null) {
				for (Entry<EReference, LazyEListWrapper> entry : refs.entrySet()) {
					entry.getValue().get(object, entry.getKey(), refs, false, mustFetchAttributes);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while resolving lazy reference", e);
		}
	}

	/**
	 * Resolves the referenced feature, if it has been marked as lazy. If
	 * it returns a non-<code>null</code> value, this value should be used.
	 * Otherwise, we should fall back on the default eGet.
	 */
	public Object resolve(EObject object, EStructuralFeature feature, boolean greedyReferences, boolean mustFetchAttributes) {
		try {
			if (feature instanceof EReference) {
				Map<EReference, LazyEListWrapper> lazyRefs = eObjectToLazyRefs.getIfPresent(object);
				if (lazyRefs != null) {
					LazyEListWrapper pendingObjects = lazyRefs.get(feature);
					if (pendingObjects != null) {
						return pendingObjects.get(object, (EReference) feature, lazyRefs, greedyReferences, mustFetchAttributes);
					}
				}
			} else if (feature instanceof EAttribute) {
				resolveAttributes(object);
			}
		} catch (Exception e) {
			LOGGER.error("Error while resolving lazy reference", e);
			e.printStackTrace();
		}
		return null;
	}

	private void resolveAttributes(EObject object) throws Exception {
		final String pendingId = eObjectWithPendingAttrs.getIfPresent(object);
		eObjectWithPendingAttrs.invalidate(object);
		if (pendingId != null) {
			final Map<String, EObject> objects = new HashMap<>();
			objects.put(pendingId, object);
			resource.fetchAttributes(objects);
		}
	}

	/**
	 * Returns <code>true</code> if the lazy resolver knows about the feature,
	 * <code>false</code> otherwise.
	 */
	public boolean isLazy(EObject object, EStructuralFeature feature) {
		if (feature instanceof EReference) {
			Map<EReference, LazyEListWrapper> pending = eObjectToLazyRefs.getIfPresent(object);
			if (pending != null) {
				LazyEListWrapper lazyEList = pending.get(feature);
				return lazyEList != null;
			}
		} else if (feature instanceof EAttribute) {
			return eObjectWithPendingAttrs.getIfPresent(object) != null;
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
	public void putLazyReference(EObject eob, EReference feature, EList<Object> value) {
		Map<EReference, LazyEListWrapper> allPending = eObjectToLazyRefs.getIfPresent(eob);
		if (allPending == null) {
			allPending = new IdentityHashMap<>();
			eObjectToLazyRefs.put(eob, allPending);
		}
		allPending.put(feature, new LazyEListWrapper(value));
	}

	/**
	 * Removes a reference to the store.
	 * 
	 * @param eob
	 *            EObject whose reference will no longer be fetched later on.
	 * @param feature
	 *            Reference to be removed.
	 */
	public void removeLazyReference(EObject eob, EReference ref) {
		Map<EReference, LazyEListWrapper> allPending = eObjectToLazyRefs.getIfPresent(eob);
		if (allPending != null) {
			allPending.remove(ref);
		}
	}

	public boolean addToLazyReference(EObject sourceObj, EReference feature, Object value) {
		Map<EReference, LazyEListWrapper> allPending = eObjectToLazyRefs.getIfPresent(sourceObj);
		LazyEListWrapper pending = allPending.get(feature);
		LOGGER.debug("Added {} to lazy references of feature {} in #{}: {}", value, feature.getName(), sourceObj, pending);
		return pending.add(value);
	}

	public boolean removeFromLazyReference(EObject sourceObj, EReference feature, Object value) {
		Map<EReference, LazyEListWrapper> allPending = eObjectToLazyRefs.getIfPresent(sourceObj);
		LazyEListWrapper pending = allPending.get(feature);
		LOGGER.debug("Removed {} from lazy references of feature {} in #{}: {}", value, feature.getName(), sourceObj,
				pending);
		return pending.remove(value);
	}

	/**
	 * Marks a certain {@link EObject} so its attributes will be fetched on
	 * demand.
	 */
	public void putLazyAttributes(String id, EObject eObject) {
		eObjectWithPendingAttrs.put(eObject, id);
	}

	/**
	 * Removes a certain {@link EObject} so its attributes will be fetched on
	 * demand.
	 */
	public void removeLazyAttributes(String id, EObject eObject) {
		eObjectWithPendingAttrs.invalidate(eObject);
	}

	/**
	 * Returns a list of {@link String} identifiers and {@link EObject}s if the
	 * referenced <code>r</code> is pending for the object <code>o</code>, or
	 * <code>null</code> otherwise.
	 */
	public EList<Object> getPending(EObject o, EReference r) {
		Map<EReference, LazyEListWrapper> allPending = eObjectToLazyRefs.getIfPresent(o);
		if (allPending != null) {
			LazyEListWrapper lazyEListWrapper = allPending.get(r);
			if (lazyEListWrapper != null && lazyEListWrapper.isPending) {
				return lazyEListWrapper.getWrapped();
			}
		}
		return null;
	}

	public EObject getContainer(EObject o) {
		LazyEContainment containment = eObjectToContainer.getIfPresent(o);
		return containment != null ? containment.getContainer() : null;
	}

	public EReference getContainingFeature(EObject o) {
		LazyEContainment containment = eObjectToContainer.getIfPresent(o);
		return containment != null ? containment.getContainmentReference() : null;
	}

	public Resource getResource(EObject o) {
		LazyEContainment containment = eObjectToContainer.getIfPresent(o);
		return containment != null ? containment.getContainer().eResource() : null;
	}
}