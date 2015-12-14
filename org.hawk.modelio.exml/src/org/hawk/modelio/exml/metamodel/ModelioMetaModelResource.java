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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.modelio.metamodel.MAttribute;
import org.modelio.metamodel.MClass;
import org.modelio.metamodel.MMetamodel;
import org.modelio.metamodel.MPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioMetaModelResource implements IHawkMetaModelResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioMetaModelResource.class);

	public static final String META_TYPE_NAME = "ModelioType";
	private static final String STRING_TYPE = "string";

	private final ModelioMetaModelResourceFactory factory;
	private final ModelioPackage metaPackage;

	private MMetamodel metamodel = new MMetamodel();
	private Set<IHawkObject> contents;
	private Map<String, ModelioClass> classesByName;

	public ModelioMetaModelResource(ModelioMetaModelResourceFactory factory) {
		this.factory = factory;
		this.metaPackage = new ModelioPackage(this, createMetaPackage());
		this.classesByName = new HashMap<>();
	}

	private MPackage createMetaPackage() {
		MPackage mpkg = new MPackage("ModelioMetaPackage", "ModelioMetaPackage", "root-meta.exml");
		final MClass mt = new MClass(META_TYPE_NAME, META_TYPE_NAME, mpkg.getExml());
		mt.getMAttributes().add(createStringAttribute(mpkg, mt.getName(), "name"));
		mpkg.getMClass().add(mt);
		return mpkg;
	}

	@Override
	public void unload() {
		metamodel = null;
	}

	@Override
	public Set<IHawkObject> getAllContents() {
		if (contents == null) {
			contents = new HashSet<>();
			addMPackageToContents(metaPackage);
			for (MPackage pkg : metamodel.getMPackages()) {
				addMPackageToContents(new ModelioPackage(this, pkg));
			}
		}
		return contents;
	}

	private void addMPackageToContents(ModelioPackage pkg) {
		contents.add(pkg);
		for (IHawkClassifier cl : pkg.getClasses()) {
			contents.add(cl);
			if (classesByName.put(cl.getName(), (ModelioClass)cl) != null) {
				LOGGER.error("Class name '{}' is not unique", cl.getName());
			}
		}
		for (ModelioPackage subpkg : pkg.getPackages()) {
			addMPackageToContents(subpkg);
		}
	}

	@Override
	public IMetaModelResourceFactory getMetaModelResourceFactory() {
		return factory;
	}

	@Override
	public void save(OutputStream output, Map<Object, Object> options) throws IOException {
		// do nothing
	}

	public ModelioClass getMetaType() {
		return metaPackage.getClassifier(META_TYPE_NAME);
	}

	public ModelioPackage getMetaPackage() {
		return metaPackage;
	}

	public ModelioClass getModelioClass(String className) {
		getAllContents();
		return classesByName.get(className);
	}

	private MAttribute createStringAttribute(MPackage rawPackage, String className, String attrName) {
		final boolean isMany = false;
		final boolean isUnique = false;
		final boolean isOrdered = false;
		return new MAttribute(className + "_" + attrName, attrName,
			rawPackage.getExml(),
			metamodel.getDataTypeByName(STRING_TYPE),
			isMany, isUnique, isOrdered);
	}
}
