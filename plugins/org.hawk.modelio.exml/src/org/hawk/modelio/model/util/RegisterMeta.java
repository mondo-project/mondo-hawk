/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio Metamodels Registry
 ******************************************************************************/
package org.hawk.modelio.model.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.metamodel.ModelioPackage;

public class RegisterMeta {

	private static int registered = 0;
	
	// TODO needs to be changed to support different metamodel versions
	// - different metamodel versions will have same name , but different id (thus URI)
	// - retrieve classes by Package Name, Class Name and Package version
	
	// TODO change to registeredMetamodelsByUri or ById
	private final static Map<String, ModelioPackage> registeredMetamodelsByName = new HashMap<String, ModelioPackage>();

	public static Collection<ModelioPackage> getRegisteredPackages() {
		return registeredMetamodelsByName.values();
	}

	public static void clean() {
		registeredMetamodelsByName.clear();
	}

	// registers metamodel
	/**
	 * register Modelio package 
	 * 
	 * @param pkg
	 */
	public static int registerPackages(ModelioPackage pkg) {

		if (registeredMetamodelsByName.put(pkg.getName(), pkg) == null) {
			System.err.println("registering package: " + pkg.getName()
					+ "(" + pkg.getNsURI() + ")");
			registered++;
		}

		return registered;
	}

	public static void registerPackages(ModelioMetaModelResource r) {
		for (IHawkObject e : r.getAllContents()) {
			if (e instanceof ModelioPackage) {
				registerPackages((ModelioPackage) e);
			} 
		}
	}
	
	// TODO should be changed to public static ModelioClass getModelioClass(String  className, String pkgVersion)
	public static ModelioClass getModelioClass(String  className) {
		String pkgName;
		String mcName;
		ModelioClass mc = null;

		final int idxDot = className.indexOf(".");
		if(idxDot > -1) {
			pkgName = className.substring(0, idxDot);
			mcName = className.substring(idxDot + 1);
			mc = getMClass(pkgName, mcName); 
		} else {

			mc = getMClass(className);
		}

		return mc;
	}
	
	private static ModelioClass getMClass(String  className) {
		for( ModelioPackage pkg : registeredMetamodelsByName.values()) {
			ModelioClass mc = pkg.getClassifier(className);
			if( mc != null) {
				return mc;
			}
		}
		return null;
	}
	
	// TODO private static ModelioClass getMClass(String pkgName, String pkgVersion, String className)
	private static ModelioClass getMClass(String pkgName, String className) {
		ModelioPackage pkg = registeredMetamodelsByName.get(pkgName);
		if(pkg != null) {	
			return pkg.getClassifier(className);
		}
		return null;
	}
}
