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
import org.hawk.modelio.exml.metamodel.parser.MMetamodelDescriptor;
import org.hawk.modelio.exml.metamodel.parser.MMetamodelParser;
import org.hawk.modelio.exml.metamodel.register.MetamodelRegister;
import org.xml.sax.InputSource;

public class ModelioMetaModelResourceFactory implements IMetaModelResourceFactory {

	
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
	public ModelioMetaModelResource parseFromString(String name, String contents) throws Exception {
		if ("".equals(contents)) {
			// Empty metamodel resource - this is the case for the meta package
			return new ModelioMetaModelResource(new MMetamodelDescriptor(), this);
		}
		return getMetamodelResource(new InputSource(new StringReader(contents)));
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		return ((ModelioPackage) ePackage).getXml();
	}

	private ModelioMetaModelResource getMetamodelResource(InputSource is) {
		ModelioMetaModelResource modelioMetamodel = new ModelioMetaModelResource(parser.parse(is), this);
		MetamodelRegister.INSTANCE.registerPackages(modelioMetamodel);
		return modelioMetamodel;
	}
}
