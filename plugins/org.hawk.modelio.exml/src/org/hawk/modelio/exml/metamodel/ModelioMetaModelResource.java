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
import org.hawk.modelio.exml.metamodel.mlib.MAttribute;
import org.hawk.modelio.exml.metamodel.mlib.MClass;
import org.hawk.modelio.exml.metamodel.mlib.MMetamodel;
import org.hawk.modelio.exml.metamodel.mlib.MPackage;
import org.hawk.modelio.exml.metamodel.parser.MMetamodelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioMetaModelResource implements IHawkMetaModelResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioMetaModelResource.class);

	public static final String META_PKG_NAME = "ModelioMetaPackage";
	public static final String META_TYPE_NAME = "ModelioType";
	public static final String META_PKG_VERSION = "00.00.00";

	protected static final String STRING_TYPE = "java.lang.String";

	private final ModelioMetaModelResourceFactory factory;
	private final ModelioPackage metaPackage;

	private MMetamodel metamodel;
	private Set<IHawkObject> contents;
	private Map<String, ModelioClass> classesById;

	public ModelioMetaModelResource(MMetamodelDescriptor metamodelDescriptor,
			ModelioMetaModelResourceFactory factory) {

		metamodel = new MMetamodel(metamodelDescriptor);

		this.factory = factory;
		this.metaPackage = new ModelioPackage(this, createMetaPackage());
		this.classesById = new HashMap<>();	
	}

	public MMetamodel getMetamodel() {
		return metamodel;
	}

	private MPackage createMetaPackage() {
		MPackage mpkg = new MPackage(META_PKG_NAME, META_PKG_NAME, META_PKG_VERSION, "");
		final MClass mt = new MClass(META_TYPE_NAME, META_TYPE_NAME);
		mt.getMAttributes().add(createStringAttribute(mpkg, mt.getName(), "name"));
		mpkg.getMClass().add(mt);
		return mpkg;
	}

	@Override
	public void unload() {
		metamodel = null;
		contents = null;
		classesById = null;
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

	/**
	 * Returns the {@link ModelioPackage} with the specified name,
	 * or <code>null</code> if not found.
	 */
	public ModelioPackage getModelioPackage(String name) {
		for (IHawkObject o : getAllContents()) {
			if (o instanceof ModelioPackage) {
				ModelioPackage pkg = (ModelioPackage)o;
				if (name.equals(pkg.getName())) {
					return pkg;
				}
			}
		}

		return null;
	}

	private void addMPackageToContents(ModelioPackage pkg) {
		contents.add(pkg);
		for (IHawkClassifier cl : pkg.getClasses()) {
			contents.add(cl);
			if (classesById.put(((ModelioClass)cl).getId(), (ModelioClass)cl) != null) {
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

	public ModelioClass getModelioClassById(String classId) {
		getAllContents();
		final ModelioClass mc = classesById.get(classId);
		if (mc != null) {
			return mc;
		}

		return null;
	}

	private MAttribute createStringAttribute(MPackage rawPackage, String className, String attrName) {
		final boolean isMany = false;
		final boolean isUnique = false;
		final boolean isOrdered = false;
		return new MAttribute(className + "_" + attrName, attrName,
				metamodel.getDataTypeByName(STRING_TYPE),
				isMany, isUnique, isOrdered);
	}

	public ModelioDataType getStringDataType() {
		return new ModelioDataType(getMetaPackage(), metamodel.getDataTypeByName(STRING_TYPE));
	}
}
