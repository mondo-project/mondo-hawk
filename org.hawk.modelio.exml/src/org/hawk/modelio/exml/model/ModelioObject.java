/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
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
import org.hawk.modelio.exml.metamodel.ModelioDataType;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioObject extends AbstractModelioObject {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioObject.class);
	public static final String COMMON_EXML = "modelio-objects.exml";
	private final ModelioClass mc;
	private final ExmlObject exml;

	public ModelioObject(ModelioClass mc, ExmlObject exml) {
		assert mc != null;
		assert exml != null;

		this.mc = mc;
		this.exml = exml;
	}

	@Override
	public boolean isRoot() {
		return exml.getParentUID() != null;
	}

	@Override
	public String getUri() {
		return exml.getFile().toURI().toString() + "#" + getUriFragment();
	}

	@Override
	public String getUriFragment() {
		return exml.getUID();
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
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
		final String rawValue = exml.getAttribute(attr.getName());
		if (rawValue != null) {
			ModelioDataType mdt = (ModelioDataType)attr.getType();
			switch (mdt.getInstanceType()) {
			case "Short": return Short.valueOf(rawValue);
			case "Long": return Long.valueOf(rawValue);
			case "Integer": return Integer.valueOf(rawValue);
			case "Float": return Float.valueOf(rawValue);
			case "Double": return Double.valueOf(rawValue);
			case "Character": return rawValue.charAt(0);
			case "Byte": return Byte.valueOf(rawValue);
			case "Boolean": return Boolean.valueOf(rawValue);
			default: return rawValue;
			}
		}
		return rawValue;
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		final List<IHawkObject> linked = new ArrayList<>();

		final ModelioMetaModelResource metamodel = mc.getPackage().getResource();
		final List<ExmlReference> links = exml.getLinks().get(ref.getName());
		if (links != null) {
			for (ExmlReference r : links) {
				ModelioClass rMC = metamodel.getModelioClass(r.getMClassName());
				if (rMC == null) {
					LOGGER.warn("Could not find class with name '{}', ignoring instance", r.getMClassName());
				} else {
					linked.add(new ModelioProxy(rMC, r));
				}
			}
		} else {
			List<ExmlReference> cmp = exml.getCompositions().get(ref.getName());
			if (cmp != null) {
				for (ExmlReference r : cmp) {
					ModelioClass rMC = metamodel.getModelioClass(r.getMClassName());
					if (rMC == null) {
						LOGGER.warn("Could not find class with name '{}', ignoring instance", r.getMClassName());
					} else if (r instanceof ExmlObject) {
						linked.add(new ModelioObject(rMC, (ExmlObject)r));
					} else {
						linked.add(new ModelioProxy(rMC, r));
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
		return exml.getFile().getPath();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exml == null) ? 0 : exml.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelioObject other = (ModelioObject) obj;
		if (exml == null) {
			if (other.exml != null)
				return false;
		} else if (!exml.equals(other.exml))
			return false;
		return true;
	}
}
