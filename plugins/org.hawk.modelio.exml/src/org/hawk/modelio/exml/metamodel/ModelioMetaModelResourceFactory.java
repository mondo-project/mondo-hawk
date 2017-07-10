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
import java.io.FileReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.modelio.metamodel.parser.MMetamodelParser;
import org.hawk.modelio.model.util.RegisterMeta;
import org.xml.sax.InputSource;

public class ModelioMetaModelResourceFactory implements IMetaModelResourceFactory {

	private ModelioMetaModelResource modelioMetamodel;
	private final Set<String> metamodelExtensions;
	MMetamodelParser  parser;

	public ModelioMetaModelResourceFactory() {
		super();
		parser = new MMetamodelParser();

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
		return getMetamodelResource(new InputSource(new FileReader(f)));
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
		return getMetamodelResource(new InputSource(new StringReader(contents)));
	}

	@Override
	public void removeMetamodel(String property) {
		// clear registry
		RegisterMeta.clean();
		
		System.err
		.println("ModelioMetaModelResourceFactory doesnot support remove metamodels, TODO Implement removeMetamodel");
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		if(((ModelioPackage) ePackage).getXml().isEmpty()) {
			((ModelioPackage) ePackage).setXml(parser.dumpPackageToXmlString((ModelioPackage) ePackage));
		} 
		return ((ModelioPackage) ePackage).getXml();
	}

	private ModelioMetaModelResource getMetamodelResource(InputSource is) {
		if (modelioMetamodel == null) {
			modelioMetamodel = new ModelioMetaModelResource(parser.parse(is), this);
		} else {
			modelioMetamodel.setMetamodel(parser.parse(is));
		}
		
		RegisterMeta.registerPackages(modelioMetamodel);

		return modelioMetamodel;
	}
}
