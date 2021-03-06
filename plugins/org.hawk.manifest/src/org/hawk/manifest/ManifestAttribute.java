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

import java.util.Set;

import org.hawk.core.model.IHawkAnnotation;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class ManifestAttribute extends ManifestObject implements IHawkAttribute {

	private final static String CLASSNAME = "ManifestAttribute";

	private String name;

	public ManifestAttribute(String string) {
		name = string;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isMany() {
		return false;
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
	public boolean isDerived() {
		return false;
	}

	@Override
	public Set<IHawkAnnotation> getAnnotations() {
		return null;
	}

	@Override
	public IHawkClassifier getType() {
		return null;
	}

	@Override
	public Object getEObject() {
		return null;
	}

}
