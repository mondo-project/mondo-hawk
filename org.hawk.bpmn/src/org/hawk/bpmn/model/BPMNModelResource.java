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
package org.hawk.bpmn.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.hawk.bpmn.BPMNObject;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;

public class BPMNModelResource implements IHawkModelResource {

	Resource res;
	private IModelResourceFactory parser;
	Set<IHawkObject> allContents = null;

	@Override
	public void unload() {
		res.unload();
		res.getResourceSet().getResources().remove(res);

		res = null;
		allContents = null;
	}

	// @Override
	// public Resource getEMFResource() {
	// return res;
	//
	// }

	// @Override
	// public ResourceSet getEMFResourceSet() {
	// return set;
	//
	// }

	public BPMNModelResource(Resource r, IModelResourceFactory p) {
		parser = p;
		res = r;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {

		return getAllContentsSet().iterator();

	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {

		if (allContents == null) {

			allContents = new HashSet<>();

			TreeIterator<EObject> it = EcoreUtil.getAllContents(res, false);

			while (it.hasNext()) {
				EObject next = it.next();
				if (!next.eIsProxy()) {
					// Ensure the element is from the same resource -- even if
					// EMF says its not a proxy!
					if (next.eResource() == res) {
						// same resource - add the object
						allContents.add(new BPMNObject(next));
					} else {
						// this is from a different resource - don't go into its children
						it.prune();
					}
				} else {
					// ignore it as it will resolve later - FIXED!
				}
			}
		}
		return allContents;

	}

	@Override
	public String getType() {
		return parser.getType();
	}

	@Override
	public byte[] getSignature(IHawkObject o) {
		return o.signature();
	}

	public Resource getResource() {
		return res;
	}
}