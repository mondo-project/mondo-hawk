/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup, use covariant return types, use SLF4J
 ******************************************************************************/
package org.hawk.emf;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMFFeatureMapReference extends EMFModelElement implements IHawkReference {
	private static final Logger LOGGER = LoggerFactory.getLogger(EMFFeatureMapReference.class);
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
			LOGGER.warn("Unknown EClassifier subclass: {}", r.getEType());
			return null;
		}
	}

	@Override
	public int hashCode() {
		return r.hashCode();
	}

}
