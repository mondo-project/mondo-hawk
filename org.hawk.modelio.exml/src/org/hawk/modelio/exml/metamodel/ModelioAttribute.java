/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import java.util.Collections;
import java.util.Set;

import org.hawk.core.model.IHawkAnnotation;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.modelio.metamodel.MAttribute;

public class ModelioAttribute extends AbstractModelioObject implements IHawkAttribute {

	private final ModelioClass mClass;
	private final MAttribute mAttr;

	public ModelioAttribute(ModelioClass mc, MAttribute mattr) {
		this.mClass = mc;
		this.mAttr = mattr;
	}

	@Override
	public String getName() {
		return mAttr.getName();
	}

	@Override
	public boolean isMany() {
		return mAttr.getIsMany();
	}

	@Override
	public boolean isUnique() {
		return mAttr.getIsUnique();
	}

	@Override
	public boolean isOrdered() {
		return mAttr.getIsOrdered();
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public String getUri() {
		return mClass.getPackageNSURI() + "#" + getUriFragment();
	}

	@Override
	public String getUriFragment() {
		return mAttr.getId();
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		switch (hsf.getName()) {
		case "name": return true;
		default: return false;
		}
	}

	@Override
	public Object get(IHawkAttribute attr) {
		switch (attr.getName()) {
		case "name": return mAttr.getName();
		default: return null;
		}
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
		return Collections.emptySet();
	}

	@Override
	public IHawkClassifier getType() {
		if (mAttr.getMDataType() != null) {
			return new ModelioDataType(mClass.getPackage().getResource().getMetaPackage(), mAttr.getMDataType());
		} else {
			return null;
		}
	}

	@Override
	public String getExml() {
		return mAttr.getExml();
	}

	@Override
	public String toString() {
		return "ModelioAttribute [getName()=" + getName() + ", isMany()=" + isMany() + ", isUnique()=" + isUnique()
				+ ", isOrdered()=" + isOrdered() + ", getType()=" + getType() + "]";
	}
	
}
