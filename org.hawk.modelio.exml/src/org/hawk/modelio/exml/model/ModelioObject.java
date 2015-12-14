package org.hawk.modelio.exml.model;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.metamodel.AbstractModelioObject;
import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlReference;

public class ModelioObject extends AbstractModelioObject {

	public static final String COMMON_EXML = "modelio-objects.exml";
	private final ModelioClass mc;
	private final ExmlObject exml;

	public ModelioObject(ModelioMetaModelResource metamodel, ExmlObject exml) {
		this.mc = metamodel.getModelioClass(exml.getMClassName());
		this.exml = exml;
	}

	@Override
	public boolean isRoot() {
		return exml.getParentUID() != null;
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
		if (hsf instanceof ModelioAttribute) {
			return exml.getAttributes().containsKey(hsf.getName());
		} else if (hsf instanceof ModelioReference) {
			return exml.getLinks().containsKey(hsf.getName()) || exml.getCompositions().containsKey(hsf.getName());
		}
		return false;
	}

	@Override
	public Object get(IHawkAttribute attr) {
		return exml.getAttribute(attr.getName());
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		final List<IHawkObject> linked = new ArrayList<>();

		final ModelioMetaModelResource metamodel = mc.getPackage().getResource();
		final List<ExmlReference> links = exml.getLinks().get(ref.getName());
		if (links != null) {
			for (ExmlReference r : links) {
				linked.add(new ModelioProxy(metamodel, r));
			}
		} else {
			List<ExmlReference> cmp = exml.getCompositions().get(ref.getName());
			if (cmp != null) {
				for (ExmlReference r : cmp) {
					if (r instanceof ExmlObject) {
						linked.add(new ModelioObject(metamodel, (ExmlObject)r));
					} else {
						linked.add(new ModelioProxy(metamodel, r));
					}
				}
			}
		}

		if (ref.isMany()) {
			return linked;
		} else if (!linked.isEmpty()) {
			return linked.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String getExml() {
		// TODO can't report the actual .exml here, since Modelio links by ID and not by .exml+ID
		return COMMON_EXML;
	}

}
