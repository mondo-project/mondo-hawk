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

import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.hawk.core.model.IHawkAnnotation;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.emf.EMFPackage;
import org.hawk.emf.EMFWrapperFactory;

/**
 * Tweaks a UML profile mapped to an EMF package to add a suffix to its nsURI
 * with the version. It also adds a "virtual" type that represents the profile
 * itself, which we can use to quickly find all the applications of a profile
 * through <code>ProfileApplied.all</all>, where <code>Profile</code> is the
 * name of our profile.
 */
public class UMLProfile extends EMFPackage {

	public UMLProfile(EPackage o, EMFWrapperFactory wf, IHawkMetaModelResource umlProfileResource) {
		super(o, wf, umlProfileResource);
	}

	@Override
	public String getNsURI() {
		final String baseURI = super.getNsURI();

		for (IHawkAnnotation ann : getAnnotations()) {
			if ("PapyrusVersion".equals(ann.getSource())) {
				return String.format("%s/%s", baseURI, ann.getDetails().get("Version"));
			}
		}

		return baseURI;
	}

	@Override
	public Set<IHawkClassifier> getClasses() {
		Set<IHawkClassifier> ret = super.getClasses();
		ret.add(getVirtualProfileClass());
		return ret;
	}

	public UMLProfileVirtualClass getVirtualProfileClass() {
		/*
		 * Virtual EClass: its name is the name of the profile plus the Profile
		 * suffix, and it has no supertypes of its own. We can find packages and
		 * profiles just fine already.
		 *
		 * We do not want to add it to any EPackage, to keep those clean - we'll
		 * just override any methods related to the package.
		 */
		final EClass eClass = EcorePackage.eINSTANCE.getEcoreFactory().createEClass();
		eClass.setName(getName() + "Application");
		final UMLProfileVirtualClass vClass = new UMLProfileVirtualClass(eClass, this, wf);
		return vClass;
	}

}
