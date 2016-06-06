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

import java.util.Set;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public abstract class ManifestClass extends ManifestObject implements
		IHawkClass {

	IHawkPackage ep;

	public String getPackageNSURI() {
		return ep.getNsURI();
	}

	public boolean isAbstract() {
		return false;
	}

	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isFragmentUnique() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		return null;
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
	public Set<IHawkClass> getSuperTypes() {
		return getAllSuperTypes();
	}

}
