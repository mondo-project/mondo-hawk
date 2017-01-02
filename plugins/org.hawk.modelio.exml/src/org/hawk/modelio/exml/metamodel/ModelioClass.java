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

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioClass.class);


	protected final ModelioPackage mPackage;
	protected final MClass rawClass;
	protected Map<String, ModelioAttribute> ownAttributes, allAttributes;
	protected Map<String, ModelioReference> ownReferences, allReferences;

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

	@Override
	public Set<IHawkAttribute> getAllAttributes() {
		return new HashSet<IHawkAttribute>(getAllAttributesMap().values());
	}

	public Map<String, ModelioAttribute> getAllAttributesMap() {
		if (allAttributes == null) {
			allAttributes = new HashMap<>();
			allAttributes.putAll(getOwnAttributesMap());
			for (IHawkClass cSuper : getAllSuperTypes()) {
				final ModelioClass mcSuper = (ModelioClass)cSuper;
				allAttributes.putAll(mcSuper.getOwnAttributesMap());
			}
		}
		return allAttributes;
	}

	public Map<String, ModelioAttribute> getOwnAttributesMap() {
		if (ownAttributes == null) {
			ownAttributes = new HashMap<>();
			for (MAttribute mattr : rawClass.getMAttributes()) {
				final ModelioAttribute attr = new ModelioAttribute(this, mattr);
				if (attr.getType() != null) {
					ownAttributes.put(mattr.getName(), attr);
				} else {
					LOGGER.warn("Attribute '{}#{}' has an unknown data type, skipping", rawClass.getName(), mattr.getName());
				}
			}
		}
		return ownAttributes;
	}

	@Override
	public Set<IHawkReference> getAllReferences() {
		return new HashSet<IHawkReference>(getAllReferencesMap().values());
	}

	/**
	 * Returns a map with all the references in this class (own and inherited).
	 */
	public Map<String, ModelioReference> getAllReferencesMap() {
		if (allReferences == null) {
			allReferences = new HashMap<>();
			allReferences.putAll(getOwnReferencesMap());
			for (IHawkClass cSuper : getAllSuperTypes()) {
				final ModelioClass mcSuper = (ModelioClass)cSuper;
				allReferences.putAll(mcSuper.getOwnReferencesMap());
			}
		}
		return allReferences;
	}

	/**
	 * Returns a map with only the references in this class (excluding the inherited).
	 */
	public Map<String, ModelioReference> getOwnReferencesMap() {
		if (ownReferences == null) {
			ownReferences = new HashMap<>();

			for (MDependency mdep : rawClass.getMDependencys()) {
				ownReferences.put(mdep.getName(), new IgnoreContainmentModelioReference(this, mdep));
			}
			if (rawClass.getMSuperType().isEmpty()) {
				// Add synthetic container/containment references to root Modelio classes
				MDependency mContainmentDep = new MDependency("HP", REF_PARENT, "hawk.exml", rawClass, false, false, true, false);
				ownReferences.put(mContainmentDep.getName(), new AlwaysContainerModelioReference(this, mContainmentDep));
			}
		}

		return ownReferences;
	}

	@Override
	public Set<ModelioClass> getAllSuperTypes() {
		final Set<ModelioClass> superClasses = new HashSet<>();
		for (MClass superRawClass : rawClass.getMSuperType()) {
			ModelioClass superClass = mPackage.getResource().getModelioClass(superRawClass.getName());
			if (superClasses.add(superClass)) {
				superClasses.addAll(superClass.getAllSuperTypes());
			}
		}
		return superClasses;
	}

	@Override
	public Set<ModelioClass> getSuperTypes() {
		return getAllSuperTypes();
	}

	/**
	 * Returns only the direct supertypes.
	 */
	public Set<IHawkClass> getOwnSuperTypes() {
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
		ModelioAttribute attr = getAllAttributesMap().get(name);
		if (attr != null) {
			return attr;
		}
		return getAllReferencesMap().get(name);
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
