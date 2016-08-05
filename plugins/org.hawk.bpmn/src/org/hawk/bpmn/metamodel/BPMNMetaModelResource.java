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
package org.hawk.bpmn.metamodel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.bpmn.BPMNClass;
import org.hawk.bpmn.BPMNPackage;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;

public class BPMNMetaModelResource implements IHawkMetaModelResource {

	Resource res;
	IMetaModelResourceFactory p;
	HashSet<IHawkObject> contents = new HashSet<>();

	@Override
	public void unload() {
		res = null;

	}

	// @Override
	// public Resource getEMFResource() {
	// return res;
	//
	// }
	//
	// @Override
	// public ResourceSet getEMFResourceSet() {
	// return set;
	//
	// }

	public BPMNMetaModelResource(Resource r, IMetaModelResourceFactory pa) {

		res = r;
		p = pa;

	}

	@Override
	public HashSet<IHawkObject> getAllContents() {

		TreeIterator<EObject> it = res.getAllContents();
		HashSet<IHawkObject> ret = new HashSet<IHawkObject>();

		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof EClass)
				ret.add(new BPMNClass((EClass) o));
			else if (o instanceof EPackage)
				ret.add(new BPMNPackage((EPackage) o, this));
		}
		return ret;

	}

	@Override
	public IMetaModelResourceFactory getMetaModelResourceFactory() {
		return p;
	}

	@Override
	public void save(OutputStream output, Map<Object, Object> hashMap)
			throws IOException {
		res.save(output, hashMap);
	}

	@Override
	public String toString() {

		return res.getURI().toString();

	}

}