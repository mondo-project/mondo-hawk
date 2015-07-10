/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.bpmn;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;

public class EMFreference extends EMFobject implements IHawkReference {

	EReference r;

	public EMFreference(EReference re) {
		super(re);
		r = re;

	}

	@Override
	public String getName() {
		return r.getName();
	}

	// @Override
	// public EStructuralFeature getEMFreference() {
	// return r;
	// }

	@Override
	public boolean isContainment() {
		return r.isContainment();
	}

	@Override
	public boolean isMany() {

		return r.isMany();
	}

	// @Override
	// public boolean isChangeable() {
	//
	// return r.isChangeable();
	// }

	// @Override
	// public int getUpperBound() {
	//
	// return r.getUpperBound();
	// }

	// @Override
	// public HawkClass getType() {
	//
	// return new EMFclass((EClass) r.getEType());
	// }

	@Override
	public boolean isOrdered() {
		return r.isOrdered();
	}

	@Override
	public boolean isUnique() {
		return r.isUnique();
	}

	@Override
	public IHawkClassifier getType() {
		EClassifier type = r.getEType();
		if (type instanceof EClass)
			return new EMFclass((EClass) r.getEType());
		else if (type instanceof EDataType)
			return new EMFDataType((EDataType) r.getEType());
		else {
			System.err.println("ref: " + r.getEType());
			return null;
		}
	}

	@Override
	public int hashCode() {
		return r.hashCode();

	}

}
