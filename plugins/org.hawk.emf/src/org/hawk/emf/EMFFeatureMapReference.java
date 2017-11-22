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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;

public class EMFFeatureMapReference extends EMFModelElement implements IHawkReference {
	private EAttribute r;

	public EMFFeatureMapReference(EAttribute esf, EMFWrapperFactory wf) {
		super(esf, wf);
		r = esf;
	}

	@Override
	public String getName() {
		return r.getName();
	}

	@Override
	public EAttribute getEObject() {
		return r;
	}

	@Override
	public boolean isContainment() {
		return true;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public boolean isMany() {

		return true;
	}

	@Override
	public boolean isOrdered() {
		return true;
	}

	@Override
	public boolean isUnique() {
		return false;
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
