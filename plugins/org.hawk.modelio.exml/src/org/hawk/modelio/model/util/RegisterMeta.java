/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
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
	private final static Map<String, ModelioPackage> registeredMetamodels = new HashMap<String, ModelioPackage>();

	public static Collection<ModelioPackage> getRegisteredPackages() {
		return registeredMetamodels.values();
	}

	public void clean() {
		registeredMetamodels.clear();
	}

	// registers metamodel
	/**
	 * register Modelio package 
	 * 
	 * @param pkg
	 */
	public static int registerPackages(ModelioPackage pkg) {

		if (registeredMetamodels.put(pkg.getName(), pkg) == null) {
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
		for( ModelioPackage pkg : registeredMetamodels.values()) {
			ModelioClass mc = pkg.getClassifier(className);
			if( mc != null) {
				return mc;
			}
		}
		return null;
	}
	
	private static ModelioClass getMClass(String pkgName, String className) {
		ModelioPackage pkg = registeredMetamodels.get(pkgName);
		if(pkg != null) {	
			return pkg.getClassifier(className);
		}
		return null;
	}
}
