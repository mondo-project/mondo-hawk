/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
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
package org.hawk.manifest;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class ManifestPackage extends ManifestClass {

	final static String CLASSNAME = "ManifestPackage";

	private IHawkAttribute name;

	public ManifestPackage(ManifestMetamodel p) {
		ep = p;
		name = new ManifestAttribute("name");
	}

	@Override
	public String getInstanceType() {
		return "org.hawk.MANIFEST#" + CLASSNAME + "Object";
	}

	@Override
	public String getUri() {
		return ep.getNsURI() + "#" + CLASSNAME;
	}

	@Override
	public String getUriFragment() {
		return CLASSNAME;
	}

	@Override
	public String getName() {
		return CLASSNAME;
	}

	@Override
	public String getPackageNSURI() {
		return ep.getNsURI();
	}

	@Override
	public Set<IHawkAttribute> getAllAttributes() {
		Set<IHawkAttribute> ret = new HashSet<>();
		ret.add(name);
		return ret;
	}

	@Override
	public Set<IHawkClass> getAllSuperTypes() {
		return new HashSet<>();
	}

	@Override
	public Set<IHawkReference> getAllReferences() {
		return new HashSet<>();
	}

	@Override
	public IHawkStructuralFeature getStructuralFeature(String sname) {
		if (sname.equals("name"))
			return name;
		return null;
	}

}
