/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
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

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.hawk.bpmn.EMFobject;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.*;

public class BPMNModelResource implements IHawkModelResource {

	Resource res;
	private IModelResourceFactory parser;
	HashSet<IHawkObject> allContents = null;

	@Override
	public void unload() {
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

		// System.err.println(r);

		parser = p;
		res = r;

	}

	@Override
	public Iterator<IHawkObject> getAllContents() {

		if (allContents == null) {

			allContents = new HashSet<>();
			TreeIterator<EObject> it = EcoreUtil.getAllContents(res, false);

			while (it.hasNext()) {
				EObject next = it.next();
				if (!next.eIsProxy()) {
					allContents.add(new EMFobject(next));
				} else {
					// ignore it as it will resolve later - FIXED!
					// System.err
					// .println("PROXY FOUND (emfmodelresource - getAllContents) !!!");
				}
			}
		}
		return allContents.iterator();

	}

	@Override
	public HashSet<IHawkObject> getAllContentsSet() {

		if (allContents == null) {

			allContents = new HashSet<>();

			TreeIterator<EObject> it = EcoreUtil.getAllContents(res, false);

			while (it.hasNext()) {
				EObject next = it.next();
				if (!next.eIsProxy()) {
					allContents.add(new EMFobject(next));
				} else {
					// ignore it as it will resolve later - FIXED!
					// System.err
					// .println("PROXY FOUND (emfmodelresource - getAllContents) !!!");
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
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}

}