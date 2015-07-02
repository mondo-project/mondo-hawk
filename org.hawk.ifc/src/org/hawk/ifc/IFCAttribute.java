package org.hawk.ifc;

import java.util.HashSet;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;

import org.hawk.core.model.*;

public class IFCAttribute extends IFCObject implements IHawkAttribute {

	private EAttribute emfattribute;

	public IFCAttribute(EAttribute att) {
		super(att);
		emfattribute = att;
	}

	// public EAttribute getEmfattribute() {
	// return emfattribute;
	// }

	public boolean isDerived() {
		return emfattribute.isDerived();
	}

	public String getName() {
		return emfattribute.getName();
	}

	public HashSet<IHawkAnnotation> getAnnotations() {

		HashSet<IHawkAnnotation> ann = new HashSet<IHawkAnnotation>();

		for (EAnnotation e : emfattribute.getEAnnotations()) {

			IHawkAnnotation a = new IFCAnnotation(e);

			ann.add(a);

		}

		return ann;

	}

	// @Override
	// public EStructuralFeature getEMFattribute() {
	//
	// return emfattribute;
	// }

	@Override
	public boolean isMany() {
		return emfattribute.isMany();
	}

	@Override
	public boolean isUnique() {
		return emfattribute.isUnique();
	}

	@Override
	public boolean isOrdered() {
		return emfattribute.isOrdered();
	}

	@Override
	public IHawkClassifier getType() {
		EClassifier type = emfattribute.getEType();
		if (type instanceof EClass)
			return new IFCClass((EClass) emfattribute.getEType());
		else if (type instanceof EDataType)
			return new IFCDataType((EDataType) emfattribute.getEType());
		else {
			// System.err.println("attr: "+emfattribute.getEType());
			return null;
		}
	}

}
