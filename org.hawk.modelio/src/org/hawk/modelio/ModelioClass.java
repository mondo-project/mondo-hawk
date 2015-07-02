package org.hawk.modelio;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import org.hawk.core.model.*;

public class ModelioClass extends ModelioObject implements IHawkClass {

	private EClass eclass;

	// private String containingFeatureName = null;

	// private static HashMap<EClass, Collection<EClass>> eAllSubTypes;

	public ModelioClass(EClass o) {

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
		return eclass.getEPackage().getNsURI();
	}

	@Override
	public HashSet<IHawkAttribute> getAllAttributes() {

		HashSet<IHawkAttribute> atts = new HashSet<IHawkAttribute>();

		for (EAttribute att : eclass.getEAllAttributes())
			atts.add(new ModelioAttribute(att));

		return atts;
	}

	@Override
	public HashSet<IHawkClass> getSuperTypes() {

		HashSet<IHawkClass> c = new HashSet<IHawkClass>();

		for (EClass e : eclass.getESuperTypes()) {

			c.add(new ModelioClass(e));

		}

		return c;

	}

	@Override
	public HashSet<IHawkReference> getAllReferences() {

		HashSet<IHawkReference> c = new HashSet<IHawkReference>();

		for (EReference e : eclass.getEAllReferences()) {

			c.add(new ModelioReference(e));

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
			return new ModelioAttribute((EAttribute) esf);
		else if (esf instanceof EReference)
			return new ModelioReference((EReference) esf);
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

}
