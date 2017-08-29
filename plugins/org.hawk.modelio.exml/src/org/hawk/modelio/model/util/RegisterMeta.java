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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.event.ListSelectionEvent;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.metamodel.ModelioPackage;
import org.hawk.modelio.exml.model.ModelioModelResourceFactory;

public class RegisterMeta {

	private static int registered = 0;

	private static Map<String, SortedMap<String, ModelioPackage>> registeredMetamodelsByName = new HashMap<String, SortedMap<String,ModelioPackage>>();

	public static Collection<ModelioPackage> getRegisteredPackages() {
		Collection<ModelioPackage> registeredPackages = new ArrayList<ModelioPackage>();
		for (Map<String, ModelioPackage> versions : registeredMetamodelsByName.values()) {
			for(ModelioPackage pkg: versions.values()) {
				registeredPackages.add(pkg);
			}
		}
		return registeredPackages;
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

		SortedMap<String, ModelioPackage> versions = registeredMetamodelsByName.get(pkg.getName());
		if(versions == null) {
			versions = new TreeMap<String, ModelioPackage>();
			registeredMetamodelsByName.put(pkg.getName(), versions);
		} 
		
		if(versions.put(pkg.getVersion(), pkg) == null) {
			System.out.println("registering package: " + pkg.getName() + "(" + pkg.getNsURI() + ")");
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

	private static ModelioClass getMClass(String className, Map<String, String> mmPackageVersions) {
		for (SortedMap<String, ModelioPackage> versions : registeredMetamodelsByName.values()) {
			for(ModelioPackage pkg: versions.values()) {
				ModelioClass mc = pkg.getClassifier(className);

				if (mc != null) {
					ModelioClass tmpMc = findRequiredVersion(versions, className, pkg.getName(), mmPackageVersions);
					if(tmpMc != null) {
						mc = tmpMc; //set to required
					} else {
						tmpMc = findLatestVersion(versions, className, pkg.getName());
						if(tmpMc != null) {
							mc = tmpMc; // set to latest
						}
					}
				}
				
				if (mc != null) {
					return mc;
				}
			}
		}
		return null;
	}

	
	private static ModelioClass findRequiredVersion(SortedMap<String, ModelioPackage> versions,  String className, String pkgName, Map<String, String> mmPackageVersions) {
		String requiredVersion = null;
		ModelioClass tmpmc = null;
		if(mmPackageVersions != null) {
			requiredVersion = mmPackageVersions.get(pkgName);
		}
		if(requiredVersion != null) {
			// check if he required version can be found
			ModelioPackage tmpPkg = versions.get(requiredVersion);
			
			if(tmpPkg != null) {
				tmpmc = tmpPkg.getClassifier(className);
			}
		}
		return tmpmc;
	}


	private static ModelioClass findLatestVersion(SortedMap<String, ModelioPackage> versions,  String className, String pkgName) {
		ModelioClass tmpmc = null;
		String latestVerison = getLatestVersion(pkgName);
		if(latestVerison != null) {
			ModelioPackage tmpPkg = versions.get(latestVerison);
			
			if(tmpPkg != null) {
				tmpmc = tmpPkg.getClassifier(className);
			}
		}
		return tmpmc;
	}

	private static ModelioClass getMClass(String pkgName, String className, String requiredVersion) {
		Map<String, ModelioPackage> versions = registeredMetamodelsByName.get(pkgName);
		
		if (versions != null) {
			ModelioPackage pkg = null;
			
			// try to get the right required version
			if(requiredVersion != null) {
				pkg = versions.get(requiredVersion);
			}
			
			if(pkg == null) {
				// get latest version
				pkg = versions.get(getLatestVersion(pkgName));
			}
			
			if(pkg != null) {
				return pkg.getClassifier(className);
			}
		}
		
		return null;
	}

	private static String getLatestVersion(String pkgName) {
		
		SortedMap<String, ModelioPackage> versions = registeredMetamodelsByName.get(pkgName);
		if(versions != null) {
			return versions.lastKey();
		}
		
		return null;
	}
	
	public static ModelioClass getModelioClass(String className, Map<String, String> mmPackageVersions) {
		String pkgName;
		String mcName;
		ModelioClass mc = null;
		String version= null;
		final int idxDot = className.indexOf(".");
		if (idxDot > -1) {
			pkgName = className.substring(0, idxDot);
			mcName = className.substring(idxDot + 1);
			
			if(mmPackageVersions != null) {
				version = mmPackageVersions.get(pkgName);
			}
			
			mc = getMClass(pkgName, mcName, version);
			
		} else {
			mc = getMClass(className, mmPackageVersions);
		}
		
		return mc;
	}
}
