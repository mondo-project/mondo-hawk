/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.bpmn;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;

public class BPMNFeatureMapReference extends BPMNObject implements
		IHawkReference {

	EAttribute r;

	public BPMNFeatureMapReference(EAttribute esf) {
		super(esf);
		r = esf;
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
			return new BPMNClass((EClass) r.getEType());
		else if (type instanceof EDataType)
			return new BPMNDataType((EDataType) r.getEType());
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
