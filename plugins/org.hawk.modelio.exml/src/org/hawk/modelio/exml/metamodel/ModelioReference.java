/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 *     Orjuwan Al-Wadeai - Changes to Integrate Modelio Metamodel 3.6
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.metamodel.mlib.MClass;
import org.hawk.modelio.exml.metamodel.mlib.MDependency;

public class ModelioReference extends AbstractModelioObject implements IHawkReference {

	private final ModelioClass mClass;
	private final MDependency mDependency;

	/**
	 * Creates a new reference.
	 * 
	 * @param mc
	 *            Modelio class containing the reference.
	 * @param mdep
	 *            Modelio dependency from the metamodel library.
	 * @param forcedContainer
	 *            If not <code>null</code>, {@link #isContainer()} uses this
	 *            value instead of going to the {@link MDependency}.
	 * @param forcedContainment
	 *            If not <code>null</code>, {@link #isContainment()} uses this
	 *            value instead of going to the {@link MDependency}.
	 */
	public ModelioReference(ModelioClass mc, MDependency mdep) {
		this.mClass = mc;
		this.mDependency = mdep;
	}

	public MDependency getRawDependency() {
		return mDependency;
	}

	@Override
	public String getName() {
		return mDependency.getName();
	}

	@Override
	public boolean isMany() {
		return mDependency.isMany();
	}

	@Override
	public boolean isUnique() {
		return mDependency.isUnique();
	}

	@Override
	public boolean isOrdered() {
		return mDependency.isOrdered();
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
		return mDependency.getId();
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
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
		case "name": return mDependency.getName();
		default: return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public boolean isContainment() {
		return mDependency.isComposition();
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		final MClass mDepClass = mDependency.getMClass();
		return mClass.getPackage().getResource().getModelioClassById(mDepClass.getId());
	}

	@Override
	public String getExml() {
		return null; // exml is not used for Modelio metamodels anymore
	}

	@Override
	public String toString() {
		return "ModelioReference [getName()=" + getName() + ", isMany()=" + isMany() + ", isUnique()=" + isUnique()
				+ ", isOrdered()=" + isOrdered() + ", isContainment()=" + isContainment() + ", getType()=" + getType()
				+ "]";
	}
}
