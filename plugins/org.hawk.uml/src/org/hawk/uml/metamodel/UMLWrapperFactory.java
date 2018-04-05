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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.profile.standard.StandardPackage;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFObject;
import org.hawk.emf.EMFPackage;
import org.hawk.emf.EMFWrapperFactory;
import org.hawk.uml.model.UMLProfiledPackage;

public class UMLWrapperFactory extends EMFWrapperFactory {

	@Override
	public EMFPackage createPackage(EPackage pkg, IHawkMetaModelResource res) {
		if (isProfile(pkg)) {
			return new UMLProfile(pkg, this, res);
		} else {
			return super.createPackage(pkg, res);
		}
	}

	@Override
	public EMFClass createClass(EClass cls) {
		if (isProfile(cls.getEPackage())) {
			return new UMLStereotype(cls, this);
		} else {
			return super.createClass(cls);
		}
	}

	@Override
	public EMFObject createObject(EObject obj) {
		if (obj instanceof org.eclipse.uml2.uml.Package) {
			// This is when we're indexing a UML package in a model, not in a metamodel
			org.eclipse.uml2.uml.Package umlPackage = (org.eclipse.uml2.uml.Package) obj;
			return new UMLProfiledPackage(umlPackage, this);
		}
		return super.createObject(obj);
	}

	public boolean isProfile(EPackage pkg) {
		// Papyrus custom metamodels have versioning annotations
		final boolean isPapyrusProfile = pkg.getEAnnotation("PapyrusVersion") != null;
		if (isPapyrusProfile) {
			return true;
		}

		// The standard profile is a regular Ecore generated EPackage
		final boolean isStandardProfile = pkg instanceof StandardPackage;
		if (isStandardProfile) {
			return true;
		}

		// UML and Ecore profiles are .profile.uml files with annotations, but not the Papyrus ones
		for (EObject parent = pkg.eContainer(); parent != null; parent = parent.eContainer()) {
			if (parent instanceof Profile) {
				return true;
			}
		}

		return false;
	}

}
