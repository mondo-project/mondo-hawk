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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.metamodel.ModelioPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMeta {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioClass.class);

	// First by name, then by version
	private static Map<String, SortedMap<String, ModelioPackage>> registeredMetamodelsByName = new HashMap<String, SortedMap<String, ModelioPackage>>();

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
	public static void registerPackages(ModelioPackage pkg) {
		SortedMap<String, ModelioPackage> versions = registeredMetamodelsByName.get(pkg.getName());
		if(versions == null) {
			versions = new TreeMap<String, ModelioPackage>();
			registeredMetamodelsByName.put(pkg.getName(), versions);
		}

		if(versions.put(pkg.getVersion(), pkg) == null) {
			LOGGER.info("registered package: " + pkg.getName() + "(" + pkg.getNsURI() + ")");
		}
	}

	public static void registerPackages(ModelioMetaModelResource r) {
		for (IHawkObject e : r.getAllContents()) {
			if (e instanceof ModelioPackage) {
				registerPackages((ModelioPackage) e);
			}
		}
	}

	private static ModelioClass getMClass(String className, Map<String, String> mmPackageVersions) {
		// Always limit the packages to those in the .dat file if provided
		Set<String> pkgNames;
		if (mmPackageVersions == null) {
			pkgNames = registeredMetamodelsByName.keySet();
		} else {
			pkgNames = mmPackageVersions.keySet();
		}

		for (String pkgName : pkgNames) {
			final SortedMap<String, ModelioPackage> versions = registeredMetamodelsByName.get(pkgName);
			if (versions == null) {
				continue;
			}

			for (ModelioPackage pkg : versions.values()) {
				final ModelioClass mc = pkg.getClassifier(className);

				if (mc != null) {
					// Try to find exact match
					final ModelioClass requiredMClass = findRequiredVersion(versions, className, pkg.getName(), mmPackageVersions);
					if (requiredMClass != null) {
						return requiredMClass;
					}

					// If not, try to find the latest version
					// FIXME shouldn't this be "closest match" instead?
					final ModelioClass latestMClass = findLatestVersion(versions, className, pkg.getName());
					if (latestMClass != null) {
						return latestMClass;
					}

					// Fall back on the first metaclass found
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
