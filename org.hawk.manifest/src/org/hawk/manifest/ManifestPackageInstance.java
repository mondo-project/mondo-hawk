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
package org.hawk.manifest;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class ManifestPackageInstance extends ManifestClass {

	final static String CLASSNAME = "ManifestPackageInstance";

	private IHawkAttribute version;

	public ManifestPackageInstance(
			ManifestMetamodel p) {
		ep = p;
		version = new ManifestAttribute("version");
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
		ret.add(version);
		return ret;
	}

	@Override
	public Set<IHawkClass> getSuperTypes() {
		return new HashSet<>();
	}

	@Override
	public Set<IHawkReference> getAllReferences() {
		return new HashSet<>();
	}

	@Override
	public IHawkStructuralFeature getStructuralFeature(String name) {
		if (name.equals("version"))
			return version;
		return null;
	}

}
