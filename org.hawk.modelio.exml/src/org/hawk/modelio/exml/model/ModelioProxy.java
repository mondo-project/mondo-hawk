package org.hawk.modelio.exml.model;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.metamodel.AbstractModelioObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlReference;

public class ModelioProxy extends AbstractModelioObject {

	private final ModelioClass mc;
	private final ExmlReference exml;

	public ModelioProxy(ModelioMetaModelResource metamodel, ExmlReference r) {
		this.mc = metamodel.getModelioClass(r.getMClassName());
		this.exml = r;
	}

	@Override
	public boolean isRoot() {
		// There's no way to know from here!
		return false;
	}

	@Override
	public String getUri() {
		return getExml() + "#" + getUriFragment();
	}

	@Override
	public String getUriFragment() {
		return exml.getUID();
	}

	@Override
	public IHawkClassifier getType() {
		return mc;
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		return false;
	}

	@Override
	public Object get(IHawkAttribute attr) {
		return null;
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public String getExml() {
		return ModelioObject.COMMON_EXML;
	}

}
