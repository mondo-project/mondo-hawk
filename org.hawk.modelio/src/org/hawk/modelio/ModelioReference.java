package org.hawk.modelio;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;

import org.hawk.core.model.*;

public class ModelioReference extends ModelioObject implements IHawkReference {

	EReference r;

	public ModelioReference(EReference re) {
		super(re);
		r = re;
	}

	@Override
	public String getName() {
		return r.getName();
	}

	@Override
	public boolean isContainment() {
		return r.isContainment();
	}

	@Override
	public boolean isContainer() {
		return r.isContainer();
	}

	@Override
	public boolean isMany() {

		return r.isMany();
	}

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
			return new ModelioClass((EClass) r.getEType());
		else if (type instanceof EDataType)
			return new ModelioDataType((EDataType) r.getEType());
		else {
			System.err.println("ref: " + r.getEType());
			return null;
		}
	}

}
