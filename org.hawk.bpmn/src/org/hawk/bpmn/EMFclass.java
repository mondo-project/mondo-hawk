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
package org.hawk.bpmn;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.hawk.core.model.*;

public class EMFclass extends EMFobject implements IHawkClass {

	private static boolean hasAtLeastOneIgnoredIDproperty = false;

	private EClass eclass;

	// private String containingFeatureName = null;

	// private static HashMap<EClass, Collection<EClass>> eAllSubTypes;

	public EMFclass(EClass o) {

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
	public String getPackageNSURI() {

		EPackage ep = eclass.getEPackage();

		if (eclass.eIsProxy())
			System.err.println("WARNING -- proxy class: " + eclass.toString());

		return ep == null ? "NULL_EPACKAGE" : ep.getNsURI();
	}

	@Override
	public HashSet<IHawkAttribute> getAllAttributes() {

		HashSet<IHawkAttribute> atts = new HashSet<IHawkAttribute>();

		for (EAttribute att : eclass.getEAllAttributes()) {

			// FIXME major -- attributes named "id" are ignored
			if (att.getName().equals("id")) {
				if (!hasAtLeastOneIgnoredIDproperty) {
					hasAtLeastOneIgnoredIDproperty = true;
					System.err
							.println("warning, type: "
									+ getName()
									+ " has a property named \"id\" which is ignored by Hawk (any other types with this property will also have it ignored)");
				}
				break;
			}

			atts.add(new EMFattribute(att));

		}
		return atts;
	}

	@Override
	public HashSet<IHawkClass> getSuperTypes() {

		HashSet<IHawkClass> c = new HashSet<IHawkClass>();

		for (EClass e : eclass.getESuperTypes()) {

			c.add(new EMFclass(e));

		}

		return c;

	}

	@Override
	public HashSet<IHawkReference> getAllReferences() {

		HashSet<IHawkReference> c = new HashSet<IHawkReference>();

		for (EReference e : eclass.getEAllReferences()) {

			c.add(new EMFreference(e));

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

		if (esf instanceof EAttribute)
			return new EMFattribute((EAttribute) esf);
		else if (esf instanceof EReference)
			return new EMFreference((EReference) esf);
		else {
			System.err.println("getEStructuralFeature( " + name
					+ " ) is not an attribute or a reference, debug:");
			return null;
		}
	}

	// @Override
	// public Set<HawkClass> eAllContents() {
	// Iterator<EObject> it = eclass.eAllContents();
	//
	// HashSet<HawkClass> ret = new HashSet<HawkClass>();
	//
	// while (it.hasNext()) {
	//
	// ret.add(new EMFclass(((EClass) it.next())));
	//
	// }
	//
	// return ret;
	//
	// }

	// @Override
	// public boolean isContained() {
	//
	// for (EClassifier e : eclass.getEPackage().getEClassifiers()) {
	//
	// if (e instanceof EClass) {
	//
	// for (EReference r : ((EClass) e).getEAllContainments()) {
	//
	// // System.err.println(r.getName() + " ->" + r.getEType());
	// EClassifier type = r.getEType();
	//
	// if (type instanceof EClass) {
	//
	// // System.err.print(eclass.getName()+" :: ");
	//
	// Collection<EClass> eclasssubtypes = getEAllSubTypes(((EClass) eclass));
	//
	// for (EClass s : eclasssubtypes) {
	//
	// // System.err.print(s.getName()+" ");
	//
	// if (//!eclasssubtypes.contains(e) &&
	// s.getName().equals(type.getName())) {
	// System.err.println("containment found! "
	// + eclass.getName()
	// + " is contained by: " + r.getName()
	// + " in " + e.getName());
	// containingFeatureName = r.getName();
	// // why 3 times the check on success?? and why all these containments on
	// commenting:
	// //return true;
	//
	// }
	// }
	// // System.err.println();
	// }
	// }
	// }
	// }
	//
	// System.err.println("warning isContained called on class: "
	// + eclass.getName() + " but this class is not contained!");
	// return false;
	//
	// }

	// @Override
	// public String eContainingFeatureName() {
	// return containingFeatureName;
	// }

	// private Collection<EClass> getEAllSubTypes(EClass eClass) {
	//
	// if (eAllSubTypes == null) {
	//
	// eAllSubTypes = new HashMap<>();
	//
	// for (EClassifier e1 : eclass.getEPackage().getEClassifiers()) {
	//
	// if (e1 instanceof EClass) {
	// for (EClass e2 : ((EClass) e1).getEAllSuperTypes()) {
	//
	// Collection<EClass> col = eAllSubTypes.get(e1);
	//
	// if (col != null) {
	// col.add(e2);
	// eAllSubTypes.put((EClass) e1, col);
	// } else {
	// col = new HashSet<>();
	// col.add((EClass) e1);
	// col.add(e2);
	// eAllSubTypes.put((EClass) e1, col);
	// }
	//
	// }
	// }
	//
	// }
	// }
	//
	// // for(EClass e : eAllSubTypes.keySet()){
	// // System.err.print(e.getName()+" :: ");
	// // for(EClass e2 : eAllSubTypes.get(e)){
	// // System.err.print(e2.getName()+" ");
	// // }
	// // System.err.println();
	// // }
	//
	// Collection<EClass> ret = eAllSubTypes.get(eClass);
	//
	// return ret == null ? new HashSet<EClass>() : ret;
	//
	// }

	@Override
	public int hashCode() {
		return eclass.hashCode();

	}

}
