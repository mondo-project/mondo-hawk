package org.hawk.modelio.exml.metamodel;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.modelio.metamodel.MDependency;

public class ModelioReference extends AbstractModelioObject implements IHawkReference {

	private final ModelioClass mClass;
	private final MDependency mDependency;

	public ModelioReference(ModelioClass mc, MDependency mdep) {
		this.mClass = mc;
		this.mDependency = mdep;
	}

	@Override
	public String getName() {
		return mDependency.getName();
	}

	@Override
	public boolean isMany() {
		return mDependency.getIsMany();
	}

	@Override
	public boolean isUnique() {
		return mDependency.getIsUnique();
	}

	@Override
	public boolean isOrdered() {
		return mDependency.getIsOrdered();
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public String getUri() {
		return mClass.getPackageNSURI() + "#" + getUriFragment();
	}

	@Override
	public String getUriFragment() {
		return mDependency.getId();
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		switch (hsf.getName()) {
		case "name": return true;
		default: return false;
		}
	}

	@Override
	public Object get(IHawkAttribute attr) {
		switch (attr.getName()) {
		case "name": return mDependency.getName();
		default: return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public boolean isContainment() {
		return mDependency.getIsConposition();
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		return mClass.getPackage().getResource().getModelioClass(mDependency.getMClass().getName());
	}

	@Override
	public String getExml() {
		return mDependency.getExml();
	}

	@Override
	public String toString() {
		return "ModelioReference [getName()=" + getName() + ", isMany()=" + isMany() + ", isUnique()=" + isUnique()
				+ ", isOrdered()=" + isOrdered() + ", isContainment()=" + isContainment() + ", getType()=" + getType()
				+ "]";
	}
}
