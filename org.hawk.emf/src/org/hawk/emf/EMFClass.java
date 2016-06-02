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
package org.hawk.emf;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class EMFClass extends EMFObject implements IHawkClass {

	private EClass eclass;

	// private String containingFeatureName = null;

	// private static HashMap<EClass, Collection<EClass>> eAllSubTypes;

	public EMFClass(EClass o) {

		super(o);
		eclass = ((EClass) o);

	}

	public EObject getEObject() {
		return eclass;

	}

	@Override
	public String getName() {
		return eclass.getName();
	}

	@Override
	public String getInstanceType() {

		String it = eclass.getInstanceClassName();

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

		EPackage ep = eclass.getEPackage();

		if (eclass.eIsProxy())
			System.err.println("WARNING -- proxy class: " + eclass.toString());

		return ep == null ? "NULL_EPACKAGE" : ep.getNsURI();
	}

	@Override
	public HashSet<IHawkAttribute> getAllAttributes() {

		HashSet<IHawkAttribute> atts = new HashSet<IHawkAttribute>();

		for (EAttribute att : eclass.getEAllAttributes())
				atts.add(new EMFAttribute(att));

		return atts;
	}

	@Override
	public Set<IHawkClass> getAllSuperTypes() {
		Set<IHawkClass> c = new HashSet<IHawkClass>();

		for (EClass e : eclass.getEAllSuperTypes()) {
			c.add(new EMFClass(e));
		}
		return c;
	}

	@Override
	public HashSet<IHawkReference> getAllReferences() {

		HashSet<IHawkReference> c = new HashSet<IHawkReference>();

		for (EReference e : eclass.getEAllReferences()) {

			c.add(new EMFReference(e));

		}

		for (EAttribute att : eclass.getEAllAttributes()) {
			if (att.getEType().getInstanceClass() == FeatureMap.Entry.class)
				c.add(new EMFFeatureMapReference(att));
		}

		return c;

	}

	@Override
	public boolean isAbstract() {
		return eclass.isAbstract();
	}

	@Override
	public boolean isInterface() {
		return eclass.isInterface();
	}

	@Override
	public IHawkStructuralFeature getStructuralFeature(String name) {

		EStructuralFeature esf = eclass.getEStructuralFeature(name);

		if (esf instanceof EAttribute) {

			if (esf.getEType().getInstanceClass() == FeatureMap.Entry.class)
				return new EMFFeatureMapReference((EAttribute) esf);

			else
				return new EMFAttribute((EAttribute) esf);

		} else if (esf instanceof EReference)
			return new EMFReference((EReference) esf);
		else {
			System.err.println("getEStructuralFeature( " + name
					+ " ) is not an attribute or a reference, debug:");
			return null;
		}
	}

	@Override
	public int hashCode() {
		return eclass.hashCode();

	}

}
