package org.hawk.ifc;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;

import org.hawk.core.model.*;

public class IFCReference extends IFCObject implements IHawkReference {

	EReference r;

	public IFCReference(EReference re) {
		super(re);
		r = re;

	}

	@Override
	public String getName() {
		return r.getName();
	}

	// @Override
	// public EStructuralFeature getEMFreference() {
	// return r;
	// }

	@Override
	public boolean isContainment() {
		return r.isContainment();
	}

	@Override
	public boolean isMany() {

		return r.isMany();
	}

	// @Override
	// public boolean isChangeable() {
	//
	// return r.isChangeable();
	// }

	// @Override
	// public int getUpperBound() {
	//
	// return r.getUpperBound();
	// }

	// @Override
	// public HawkClass getType() {
	//
	// return new EMFclass((EClass) r.getEType());
	// }

	@Override
	public boolean isOrdered() {
		return r.isOrdered();
	}

	@Override
	public boolean isUnique() {
		return r.isUnique();
	}

	@Override
	public IHawkClassifier getType() {
		EClassifier type = r.getEType();
		if (type instanceof EClass)
			return new IFCClass((EClass) r.getEType());
		else if (type instanceof EDataType)
			return new IFCDataType((EDataType) r.getEType());
		else {
			System.err.println("ref: " + r.getEType());
			return null;
		}
	}

}
