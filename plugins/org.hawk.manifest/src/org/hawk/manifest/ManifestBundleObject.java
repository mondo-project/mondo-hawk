/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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

public class ManifestBundleObject extends ManifestObject {

	private String symbolicName;

	public ManifestBundleObject(String symbolicName, ManifestModelResource res) {
		this.symbolicName = "Bundle_" + symbolicName;
		this.res = res;
	}

	@Override
	public boolean URIIsRelative() {
		return false;
	}

	@Override
	public String getUriFragment() {
		return symbolicName;
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
	}

	@Override
	public IHawkClassifier getType() {
		return res.getType(ManifestBundle.CLASSNAME);
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		String name = hsf.getName();
		switch (name) {
		case "symbolicName":
			return symbolicName != null;
		default:
			return false;
		}
	}

	@Override
	public Object get(IHawkAttribute attr) {
		String name = attr.getName();
		switch (name) {
		case "symbolicName":
			return symbolicName;
		default:
			System.err.println("attribute: " + name
					+ " not found for ManifestBundleObject, returning null");
			return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public String getUri() {
		return res.getUri() + "#" + getUriFragment();
	}
}
