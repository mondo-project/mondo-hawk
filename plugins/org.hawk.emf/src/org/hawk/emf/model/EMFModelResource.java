/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.emf.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.emf.EMFObject;

public class EMFModelResource implements IHawkModelResource {

	/**
	 * Goes through an EMF resource, mapping each non-proxy object within the
	 * resource to an EMFObject.
	 */
	private final class EMFObjectIterable implements Iterable<IHawkObject> {
		@Override
		public Iterator<IHawkObject> iterator() {
			final TreeIterator<EObject> it = EcoreUtil.getAllContents(res, false);

			return new Iterator<IHawkObject>() {
				EObject next = null;

				@Override
				public boolean hasNext() {
					while (next == null && it.hasNext()) {
						final EObject rawNext = it.next();
						if (!rawNext.eIsProxy()) {
							if (rawNext.eResource() == res) {
								next = rawNext;
							} else {
								it.prune();
							}
						}
					}
					return next != null;
				}

				@Override
				public IHawkObject next() {
					if (hasNext()) {
						EObject ret = next;
						next = null;
						return new EMFObject(ret);
					}
					throw new NoSuchElementException();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private Resource res;
	private IModelResourceFactory parser;
	private Set<IHawkObject> allContents = null;

	@Override
	public void unload() {
		res.unload();
		res.getResourceSet().getResources().remove(res);

		res = null;
		allContents = null;
	}

	public EMFModelResource(Resource r, IModelResourceFactory p) {
		parser = p;
		res = r;
	}

	@Override
	public Iterable<IHawkObject> getAllContents() {
		return new EMFObjectIterable();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		if (allContents == null) {
			allContents = new HashSet<>();
			for (IHawkObject eob : getAllContents()) {
				allContents.add(eob);
			}
		}

		return allContents;
	}

	@Override
	public String getType() {
		return parser.getType();
	}

	public Resource getResource() {
		return res;
	}

	@Override
	public boolean providesSingletonElements() {
		return false;
	}
}