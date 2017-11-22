/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup and use covariant return types
 ******************************************************************************/
package org.hawk.emf;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class EMFClass extends EMFModelElement implements IHawkClass {
	protected EClass eClass;

	public EMFClass(EClass o, EMFWrapperFactory wf) {
		super(o, wf);
		eClass = ((EClass) o);
	}

	@Override
	public EClass getEObject() {
		return eClass;
	}

	@Override
	public String getName() {
		return eClass.getName();
	}

	@Override
	public String getInstanceType() {

		String it = eClass.getInstanceClassName();

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

		EPackage ep = eClass.getEPackage();

		if (eClass.eIsProxy())
			System.err.println("WARNING -- proxy class: " + eClass.toString());

		return ep == null ? "NULL_EPACKAGE" : ep.getNsURI();
	}

	@Override
	public HashSet<IHawkAttribute> getAllAttributes() {

		HashSet<IHawkAttribute> atts = new HashSet<IHawkAttribute>();

		for (EAttribute att : eClass.getEAllAttributes())
				atts.add(wf.createAttribute(att));

		return atts;
	}

	@Override
	public Set<IHawkClass> getAllSuperTypes() {
		Set<IHawkClass> c = new HashSet<IHawkClass>();

		for (EClass e : eClass.getEAllSuperTypes()) {
			c.add(wf.createClass(e));
		}
		return c;
	}

	@Override
	public Set<IHawkClass> getSuperTypes() {
		return getAllSuperTypes();
	}

	@Override
	public HashSet<IHawkReference> getAllReferences() {

		HashSet<IHawkReference> c = new HashSet<IHawkReference>();

		for (EReference e : eClass.getEAllReferences()) {

			c.add(wf.createReference(e));

		}

		for (EAttribute att : eClass.getEAllAttributes()) {
			if (att.getEType().getInstanceClass() == FeatureMap.Entry.class)
				c.add(wf.createFeatureMapReference(att));
		}

		return c;

	}

	@Override
	public boolean isAbstract() {
		return eClass.isAbstract();
	}

	@Override
	public boolean isInterface() {
		return eClass.isInterface();
	}

	@Override
	public IHawkStructuralFeature getStructuralFeature(String name) {

		EStructuralFeature esf = eClass.getEStructuralFeature(name);

		if (esf instanceof EAttribute) {

			if (esf.getEType().getInstanceClass() == FeatureMap.Entry.class)
				return wf.createFeatureMapReference((EAttribute) esf);

			else
				return wf.createAttribute((EAttribute) esf);

		} else if (esf instanceof EReference)
			return wf.createReference((EReference) esf);
		else {
			System.err.println("getEStructuralFeature( " + name
					+ " ) is not an attribute or a reference, debug:");
			return null;
		}
	}

	@Override
	public int hashCode() {
		return eClass.hashCode();

	}

}
