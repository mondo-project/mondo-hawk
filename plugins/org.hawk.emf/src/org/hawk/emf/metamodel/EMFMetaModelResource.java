/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.emf.metamodel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.emf.EMFWrapperFactory;

public class EMFMetaModelResource implements IHawkMetaModelResource {

	private EMFWrapperFactory wf;
	private Resource res;
	private IMetaModelResourceFactory p;

	@Override
	public void unload() {
		res = null;
	}

	public EMFMetaModelResource(Resource r, EMFWrapperFactory wf, IMetaModelResourceFactory pa) {
		this.res = r;
		this.p = pa;
		this.wf = wf;
	}

	@Override
	public Set<IHawkObject> getAllContents() {
		final Iterator<EObject> it = res.getAllContents();
		final Set<IHawkObject> ret = new HashSet<IHawkObject>();
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof EClass)
				ret.add(wf.createClass((EClass) o));
			else if (o instanceof EPackage)
				ret.add(wf.createPackage((EPackage) o, this));
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

	public Resource getResource() {
		return res;
	}

}