/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup, use covariant return types, add SLF4J
 ******************************************************************************/
package org.hawk.emf;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMFReference extends EMFModelElement implements IHawkReference {
	private static final Logger LOGGER = LoggerFactory.getLogger(EMFReference.class);

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
			LOGGER.warn("Unknown EClassifier subclass: {}", r.getEType());
			return null;
		}
	}

	@Override
	public int hashCode() {
		return r.hashCode();

	}

}
