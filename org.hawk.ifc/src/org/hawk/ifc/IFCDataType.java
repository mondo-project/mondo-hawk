/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

import org.hawk.core.model.*;

public class IFCDataType extends IFCObject implements IHawkDataType {

	private EDataType edatatype;

	// private String containingFeatureName = null;

	// private static HashMap<EClass, Collection<EClass>> eAllSubTypes;

	public IFCDataType(EDataType eDataType) {

		super(eDataType);
		edatatype = ((EDataType) eDataType);

	}

	public EObject getEObject() {
		return edatatype;

	}

	@Override
	public String getName() {
		return edatatype.getName();
	}

	@Override
	public String getInstanceType() {

		String it = edatatype.getInstanceClassName();

		return it == null ? "NULL_INSTANCE_TYPE" : it;
	}

	@Override
	public String getPackageNSURI() {
		return edatatype.getEPackage().getNsURI();
	}

}
