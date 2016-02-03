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

public class ManifestReference extends ManifestObject implements IHawkReference {

	private final static String CLASSNAME = "ManifestReference";

	String name;
	boolean ismany;
	IHawkClassifier type;

	public ManifestReference(String string, boolean ismany, IHawkClassifier type) {
		name = string;
		this.type = type;
		this.ismany = ismany;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public boolean isOrdered() {
		return false;
	}

	@Override
	public String getUri() {
		return "org.hawk.MANIFEST#" + CLASSNAME;
	}

	@Override
	public String getUriFragment() {
		return CLASSNAME;
	}

	@Override
	public boolean isFragmentUnique() {
		return false;
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		return false;
	}

	@Override
	public Object get(IHawkAttribute attr) {
		return null;
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public IHawkClassifier getType() {
		return type;
	}

	@Override
	public boolean isContainment() {
		return false;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public boolean isMany() {
		return ismany;
	}

}
