/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 *     Orjuwan Al-Wadeai -  Integrate Modelio Metamodel 3.6
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.metamodel.mlib.MDataType;

public class ModelioDataType extends AbstractModelioObject implements IHawkDataType {

	private final ModelioPackage mPackage;
	private final MDataType mDataType;

	public ModelioDataType(ModelioPackage pkg, MDataType mDataType) {
		this.mPackage = pkg;
		this.mDataType = mDataType;
	}

	@Override
	public String getInstanceType() {
		String it = mDataType.getJavaEquivalent();

		// remove java.lang for java.lang.X 
		String[] arrayString = it.split("\\.");
		if(arrayString.length > 0) {
			it = arrayString[arrayString.length - 1];
		}

		it = it == null ? "NULL_INSTANCE_TYPE" : it;

		return it;
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public String getUri() {
		return mPackage.getNsURI() + "#" + getUriFragment();
	}

	@Override
	public String getUriFragment() {
		return mDataType.getId();
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
	}

	@Override
	public IHawkClassifier getType() {
		return mPackage.getResource().getMetaType();
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
		case "name": return mDataType.getName();
		default: return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public String getName() {
		return mDataType.getName();
	}

	@Override
	public String getPackageNSURI() {
		return mPackage.getNsURI();
	}

	@Override
	public String getExml() {
		return mPackage.getExml();
	}

	@Override
	public String toString() {
		return "ModelioDataType [getInstanceType()=" + getInstanceType() + ", getName()=" + getName() + "]";
	}
}
