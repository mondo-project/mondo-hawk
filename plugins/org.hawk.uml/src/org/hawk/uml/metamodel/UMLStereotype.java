/*******************************************************************************
 * Copyright (c) 2017-2018 Aston University.
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
 ******************************************************************************/
package org.hawk.uml.metamodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFWrapperFactory;

/**
 * Small wrapper over UML stereotypes mapped to EMF EClasses to use the same
 * /X.Y.Z as for profile EPackages.
 */
public class UMLStereotype extends EMFClass {
	public UMLStereotype(EClass o, EMFWrapperFactory wf) {
		super(o, wf);
	}

	@Override
	public String getPackageNSURI() {
		final EPackage pkg = eClass.getEPackage();
		return new UMLProfile(pkg, wf, null).getNsURI();
	}
}
