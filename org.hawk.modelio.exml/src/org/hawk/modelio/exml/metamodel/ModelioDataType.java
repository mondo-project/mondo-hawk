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

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.modelio.metamodel.MDataType;

public class ModelioDataType extends AbstractModelioObject implements IHawkDataType {

	private final ModelioPackage mPackage;
	private final MDataType mDataType;

	public ModelioDataType(ModelioPackage pkg, MDataType mDataType) {
		this.mPackage = pkg;
		this.mDataType = mDataType;
	}

	@Override
	public String getInstanceType() {
		return mDataType.getJavaEquivalent();
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public String getUriFragment() {
		return mDataType.getId();
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
