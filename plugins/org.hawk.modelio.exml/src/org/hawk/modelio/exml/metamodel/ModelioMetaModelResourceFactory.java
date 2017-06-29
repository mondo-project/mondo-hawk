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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.modelio.metamodel.parser.MMetamodelParser;

public class ModelioMetaModelResourceFactory implements IMetaModelResourceFactory {

	private ModelioMetaModelResource modelioMetamodel;
	private final Set<String> metamodelExtensions;
	
	public static  ModelioMetaModelResource staticMetamodel;


	private ModelioMetaModelResource getMetamodel(File f) {
		
		if (modelioMetamodel == null) {
			MMetamodelParser  parser = new MMetamodelParser();
			
			modelioMetamodel = new ModelioMetaModelResource(parser.parse(f), this);
			staticMetamodel = modelioMetamodel;
		}
		return modelioMetamodel;
	}

	public ModelioMetaModelResourceFactory() {
		super();
		metamodelExtensions = new HashSet<String>();
		metamodelExtensions.add(".xml");
		
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio metamodel resource factory";
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		return getMetamodel(f);
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		return Collections.emptySet();
	}

	@Override
	public void shutdown() {
		modelioMetamodel = null; 
	}

	@Override
	public boolean canParse(File f) {		
		String[] split = f.getPath().split("\\.");
		String extension = split[split.length - 1];
		return getMetamodelExtensions().contains("." + extension);
	}
	
	public Set<String> getMetamodelExtensions() {
		return metamodelExtensions;
	}

	@Override
	public Collection<String> getMetaModelExtensions() {
		return metamodelExtensions;
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {
		// @todo: need to check 
		return null;//getMetamodel();
	}

	@Override
	public void removeMetamodel(String property) {
		// ignore
		System.err
		.println("ModelioMetaModelResourceFactory cannot remove metamodels, for now. need to be changed");
		
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		// unsupported
		return "";
	}

}
