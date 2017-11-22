/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup and use covariant return types
 ******************************************************************************/
package org.hawk.emf;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;

public class EMFReference extends EMFModelElement implements IHawkReference {

	EReference r;

	public EMFReference(EReference re, EMFWrapperFactory wf) {
		super(re, wf);
		r = re;
	}

	@Override
	public String getName() {
		return r.getName();
	}

	@Override
	public EReference getEObject() {
		return r;
	}

	@Override
	public boolean isContainment() {
		return r.isContainment();
	}

	@Override
	public boolean isContainer() {
		return r.isContainer();
	}

	@Override
	public boolean isMany() {
		return r.isMany();
	}

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
			return wf.createClass((EClass) r.getEType());
		else if (type instanceof EDataType)
			return wf.createDataType((EDataType) r.getEType());
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
