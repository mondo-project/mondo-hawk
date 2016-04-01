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
package org.hawk.modelio.exml.metamodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.modelio.exml.listeners.ModelioGraphChangeListener;
import org.modelio.metamodel.MAttribute;
import org.modelio.metamodel.MClass;
import org.modelio.metamodel.MDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioClass extends AbstractModelioObject implements IHawkClass {

	/**
	 * Name of the synthetic reference that Hawk uses to represent Modelio
	 * OID->PID containment. Needed since Modelio containment is sometimes
	 * instance-dependent (which EMF does not like). Antonin mentioned this in
	 * regards to Association and AssociationEnd objects.
	 */
	public static final String REF_PARENT = "hawkParent"; 

	/**
	 * Name of the synthetic derived containment reference that is added to make
	 * the Modelio graph compatible with EMF. See
	 * {@link ModelioGraphChangeListener} for details.
	 */
	public static final String REF_CHILDREN = "hawkChildren";

	/**
	 * MClass that represents the type of the synthetic {@link #REF_PARENT}
	 * reference.
	 */
	public static final String REF_PARENT_MCLASS = "Element";

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioClass.class);


	protected final ModelioPackage mPackage;
	protected final MClass rawClass;
	protected Map<String, ModelioAttribute> attrs;
	protected Map<String, ModelioReference> refs;

	public ModelioClass(ModelioPackage pkg, MClass mc) {
		this.mPackage = pkg;
		this.rawClass = mc;
	}

	@Override
	public String getInstanceType() {
		return rawClass.getName();
	}

	@Override
	public boolean isRoot() {
		// Modelio classes are always inside packages
		return false;
	}

	@Override
	public String getUri() {
		return mPackage.getNsURI() + "#" + rawClass.getId();
	}

	@Override
	public String getUriFragment() {
		return rawClass.getId();
	}

	@Override
	public boolean isFragmentUnique() {
		return true;
	}

	@Override
	public IHawkClassifier getType() {
		return mPackage.getResource().getMetaType();
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
		case "name": return rawClass.getName();
		default: return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		return null;
	}

	@Override
	public String getName() {
		return rawClass.getName();
	}

	public ModelioPackage getPackage() {
		return mPackage;
	}

	@Override
	public String getPackageNSURI() {
		return mPackage.getNsURI();
	}

	public Map<String, ModelioAttribute> getAttributes() {
		if (attrs == null) {
			attrs = new HashMap<>();
			addAttributes(rawClass);
		}
		return attrs;
	}

	private void addAttributes(final MClass mc) {
		for (MAttribute mattr : mc.getMAttributes()) {
			final ModelioAttribute attr = new ModelioAttribute(this, mattr);
			if (attr.getType() != null) {
				attrs.put(mattr.getName(), attr);
			} else {
				LOGGER.warn("Attribute '{}#{}' has an unknown data type, skipping", mc.getName(), mattr.getName());
			}
		}
		for (MClass mcSuper : mc.getMSuperType()) {
			addAttributes(mcSuper);
		}
	}

	@Override
	public Set<IHawkAttribute> getAllAttributes() {
		return new HashSet<IHawkAttribute>(getAttributes().values());
	}

	public Map<String, ModelioReference> getReferences() {
		if (refs == null) {
			refs = new HashMap<>();
			for (MDependency mdep : rawClass.getMDependencys()) {
				refs.put(mdep.getName(), new IgnoreContainmentModelioReference(this, mdep));
			}

			if (rawClass.getMSuperType().isEmpty()) {
				// Add synthetic container/containment references to root Modelio classes
				MClass refTypeClass = mPackage.getResource().getModelioClass(REF_PARENT_MCLASS).rawClass;
				MDependency mContainmentDep = new MDependency("HP", REF_PARENT, "hawk.exml", refTypeClass, false, false, true, false);
				refs.put(mContainmentDep.getName(), new AlwaysContainerModelioReference(this, mContainmentDep));
			}

			for (IHawkClass mcSuper : getSuperTypes()) {
				for (IHawkReference superRef : mcSuper.getAllReferences()) {
					refs.put(superRef.getName(), (ModelioReference)superRef);
				}
			}
		}
		return refs;
	}

	@Override
	public Set<IHawkReference> getAllReferences() {
		return new HashSet<IHawkReference>(getReferences().values());
	}

	@Override
	public Set<IHawkClass> getSuperTypes() {
		final Set<IHawkClass> superClasses = new HashSet<>();
		for (MClass superRawClass : rawClass.getMSuperType()) {
			ModelioClass superClass = mPackage.getResource().getModelioClass(superRawClass.getName());
			superClasses.add(superClass);
		}
		return superClasses;
	}

	@Override
	public boolean isAbstract() {
		// TODO unused by Hawk?
		return false;
	}

	@Override
	public boolean isInterface() {
		// TODO unused by Hawk?
		return false;
	}

	@Override
	public IHawkStructuralFeature getStructuralFeature(String name) {
		ModelioAttribute attr = getAttributes().get(name);
		if (attr != null) {
			return attr;
		}
		return getReferences().get(name);
	}

	@Override
	public String getExml() {
		return rawClass.getExml();
	}

	@Override
	public String toString() {
		return "ModelioClass [name=" + getName() + "]";
	}

	
}
