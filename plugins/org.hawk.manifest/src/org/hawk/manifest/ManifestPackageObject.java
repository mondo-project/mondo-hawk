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

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.manifest.model.ManifestModelResource;

public class ManifestPackageObject extends ManifestObject {

	private String name;

	public ManifestPackageObject(String name,
			ManifestModelResource manifestModelResource) {

		this.res = manifestModelResource;
		this.name = "Package_" + name;

	}

	@Override
	public String getUri() {
		return res.getUri() + "#" + getUriFragment();
	}

	@Override
	public boolean URIIsRelative() {
		return false;
	}

	@Override
	public String getUriFragment() {
		return name;
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
	}

	@Override
	public IHawkClassifier getType() {
		return res.getType(ManifestPackage.CLASSNAME);
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		String name = hsf.getName();
		switch (name) {
		case "name":
			return this.name != null;
		default:
			return false;
		}
	}

	@Override
	public Object get(IHawkAttribute attr) {
		String name = attr.getName();
		switch (name) {
		case "name":
			return this.name;
		default:
			return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

}
