/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup and use covariant return types
 ******************************************************************************/
package org.hawk.emf;

import org.eclipse.emf.ecore.EDataType;
import org.hawk.core.model.IHawkDataType;

public class EMFDataType extends EMFModelElement implements IHawkDataType {
	private EDataType eDataType;

	public EMFDataType(EDataType eDataType, EMFWrapperFactory wf) {
		super(eDataType, wf);
		this.eDataType = ((EDataType) eDataType);
	}

	public EDataType getEObject() {
		return eDataType;
	}

	@Override
	public String getName() {
		return eDataType.getName();
	}

	@Override
	public String getInstanceType() {

		String it = eDataType.getInstanceClassName();

		it = it == null ? "NULL_INSTANCE_TYPE" : it;
		
		switch (it) {
		case "long":
			return Long.class.getName();
		case "int":
			return Integer.class.getName();
		case "float":
			return Float.class.getName();
		case "double":
			return Double.class.getName();
		case "boolean":
			return Boolean.class.getName();
		}

		return it;
	}

	@Override
	public String getPackageNSURI() {
		return eDataType.getEPackage().getNsURI();
	}

	@Override
	public int hashCode() {
		return eDataType.hashCode();

	}

}
