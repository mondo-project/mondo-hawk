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
package org.hawk.modelio;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;

import org.hawk.core.model.*;

public class ModelioAttribute extends ModelioObject implements IHawkAttribute {

	private EAttribute emfattribute;

	public ModelioAttribute(EAttribute att) {
		super(att);
		emfattribute = att;
	}

	// public EAttribute getEmfattribute() {
	// return emfattribute;
	// }

	public boolean isDerived() {
		return emfattribute.isDerived();
	}

	public String getName() {
		return emfattribute.getName();
	}

	public HashSet<IHawkAnnotation> getAnnotations() {

		HashSet<IHawkAnnotation> ann = new HashSet<IHawkAnnotation>();

		for (EAnnotation e : emfattribute.getEAnnotations()) {

			IHawkAnnotation a = new ModelioAnnotation(e);

			ann.add(a);

		}

		return ann;

	}

	// @Override
	// public EStructuralFeature getEMFattribute() {
	//
	// return emfattribute;
	// }

	@Override
	public boolean isMany() {
		return emfattribute.isMany();
	}

	@Override
	public boolean isUnique() {
		return emfattribute.isUnique();
	}

	@Override
	public boolean isOrdered() {
		return emfattribute.isOrdered();
	}

	@Override
	public IHawkClassifier getType() {
		EClassifier type = emfattribute.getEType();
		if (type instanceof EClass)
			return new ModelioClass((EClass) emfattribute.getEType());
		else if (type instanceof EDataType)
			return new ModelioDataType((EDataType) emfattribute.getEType());
		else {
			// System.err.println("attr: "+emfattribute.getEType());
			return null;
		}
	}

}
