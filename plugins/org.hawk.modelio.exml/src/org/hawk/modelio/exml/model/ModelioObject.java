/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 *     Orjuwan Al-Wadeai - Changes to Integrate Modelio Metamodel 3.6
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.metamodel.AbstractModelioObject;
import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioDataType;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.exml.metamodel.register.MetamodelRegister;
import org.hawk.modelio.exml.model.parser.ExmlObject;
import org.hawk.modelio.exml.model.parser.ExmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioObject extends AbstractModelioObject {

	public static final String COMMON_EXML = "modelio-objects.exml";

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioObject.class);

	private final ModelioClass mc;
	private final ExmlObject exml;

	private Map<String, String> mmPackageVersions;

	public ModelioObject(ModelioClass mc, ExmlObject exml,  Map<String, String> mmPackageVersions) {
		assert mc != null;
		assert exml != null;

		this.mc = mc;
		this.exml = exml;
		
		this.mmPackageVersions = mmPackageVersions;
	}

	@Override
	public boolean isRoot() {
		return exml.getParentUID() == null;
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
	public ModelioClass getType() {
		return mc;
	}

	public ModelioClass getRootType() {
		LinkedList<ModelioClass> typeQueue = new LinkedList<>();
		typeQueue.add(getType());

		while (!typeQueue.isEmpty()) {
			ModelioClass current = typeQueue.removeFirst();
			if (current.getSuperTypes().isEmpty()) {
				return current;
			} else {
				for (ModelioClass st : current.getSuperTypes()) {
					typeQueue.addLast(st);
				}
			}
		}

		// This should never happen: we will always have at least one root supertype
		return null;
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		if (hsf instanceof ModelioAttribute) {
			return exml.getAttributes().containsKey(hsf.getName());
		} else if (hsf instanceof ModelioReference) {
			return exml.getLinks().containsKey(hsf.getName())
				|| exml.getCompositions().containsKey(hsf.getName())
				|| hsf.getName().equals(ModelioClass.REF_PARENT) && exml.getParentUID() != null;
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

		if (ref.getName().equals(ModelioClass.REF_PARENT) && exml.getParentUID() != null) {
			ExmlReference parentRef = new ExmlReference(exml.getFile());
			parentRef.setName(ModelioClass.REF_PARENT);
			parentRef.setMClassName(getRootType().getName());
			parentRef.setUID(exml.getParentUID());
			return new ModelioProxy((ModelioClass) ref.getType(), parentRef);
		}

		final List<ExmlReference> links = exml.getLinks().get(ref.getName());
		if (links != null) {
			for (ExmlReference r : links) {
				ModelioClass rMC = MetamodelRegister.INSTANCE.getModelioClass(r.getMClassName(), mmPackageVersions);
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
					ModelioClass rMC = MetamodelRegister.INSTANCE.getModelioClass(r.getMClassName(), mmPackageVersions);
					if (rMC == null) {
						LOGGER.warn("Could not find class with name '{}', ignoring instance", r.getMClassName());
					} else if (r instanceof ExmlObject) {
						linked.add(new ModelioObject(rMC, (ExmlObject)r, mmPackageVersions));
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

	@Override
	public String toString() {
		return "ModelioObject [mc=" + mc.getName() + ", exml=" + getExml() + ", uid=" + exml.getUID() + ", pid=" + exml.getParentUID() + "]";
	}

}
