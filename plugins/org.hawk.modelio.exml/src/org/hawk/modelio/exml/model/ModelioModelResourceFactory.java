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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipFile;

import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlParser;

public class ModelioModelResourceFactory implements IModelResourceFactory {
	Map<String, String> mmPackageVersions;
	
	public Map<String, String> getMmPackageVersions() {
		return mmPackageVersions;
	}

	private static final String EXML_EXT = ".exml";
	private static final String MMVERSION_DAT = "mmversion.dat";
	private static final Set<String> MODEL_EXTS = new HashSet<String>();
	static {
		MODEL_EXTS.add(EXML_EXT);
		MODEL_EXTS.add(".ramc");
		MODEL_EXTS.add(".modelio.zip");
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio exml-based model factory";
	}

	@Override
	public IHawkModelResource parse(IFileImporter importer, File f) throws Exception {

		// TODO use importer to grab MMVERSION_DAT instead of assuming it'll appear here.
		extracVersionsForModel(importer, f);

		if (f.getName().toLowerCase().endsWith(EXML_EXT)) {
			try (final FileInputStream fIS = new FileInputStream(f)) {
				final ExmlParser parser = new ExmlParser();
				final ExmlObject object = parser.getObject(f, fIS);
				return new ModelioModelResource(object, this);
			}
		} else {
			try (final ZipFile zf = new ZipFile(f)) {
				final ExmlParser parser = new ExmlParser();
				final Iterable<ExmlObject> objects = parser.getObjects(f);
				return new ModelioModelResource(objects, this);
			}
		}
	}

	@Override
	public void shutdown() {
	}

	@Override
	public boolean canParse(File f) {
		for (String ext : MODEL_EXTS) {
			if (f.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<String> getModelExtensions() {
		return MODEL_EXTS;
	}
	
	public String getPackgeVersion(String pkgName) {
		if(mmPackageVersions == null) {
			return null;
		}
		
		return mmPackageVersions.get(pkgName);
	}
	
	private void extracVersionsForModel(IFileImporter importer, File f) throws IOException {
		
		int maxSearchDepth = 4;
		String parentName = "";
		Boolean lastTry = false;
		
		File parentFile = f.getParentFile(); // get parent folder name
		
		for(int i = 0; i < maxSearchDepth; i++) {
			if(parentFile != null) {
				parentName = parentFile.getName();
				parentFile = parentFile.getParentFile();
			} else {
				parentName = "";
				lastTry = true;
			}
			
			String versionPath = (parentName +  "/admin2/mmversion.dat");
			File versionFile = importer.importFile(versionPath);
			if(versionFile.exists()) {
				readMMVersionDat(versionFile);
				break;
			}
			
			if(lastTry) {
				break;
			}
		}
	}
	
	private void readMMVersionDat(File f) throws IOException{
		if(mmPackageVersions == null) {
			mmPackageVersions = new HashMap<String, String>();
		} else {
			mmPackageVersions.clear();
		}
		
		Scanner sc = new Scanner(f);
		
        while (sc.hasNextLine()) {
            String pkgName = sc.nextLine();
            String version = "";
            
            if(sc.hasNextLine()) {
            	version = sc.nextLine();
            }
    		
            if(version.matches("^\\d+(\\.\\d+){2}$")) {
            	mmPackageVersions.put(pkgName, version);
            }
            
            System.out.println(pkgName + ": verison " + version);
            
        }
        sc.close();
    }

}
