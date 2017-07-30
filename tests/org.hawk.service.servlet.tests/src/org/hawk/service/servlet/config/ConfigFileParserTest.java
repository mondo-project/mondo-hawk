package org.hawk.service.servlet.config;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Test For ConfigFileParser
 ******************************************************************************/

public class ConfigFileParserTest {
	private static final String SERVERCONFIG_PATH = "resources/ServerConfig/";
	ConfigFileParser parser;
	HawkInstanceConfig config;
	final String xmlFilePath = SERVERCONFIG_PATH + "instance_1.xml";
	final String xsdFilePath = SERVERCONFIG_PATH + "HawkServerConfigurationSchema.xsd";

	@Test
	public void testParseFile() {
		parseXml();
		
		// config config values
		assertEquals("Instance Name:", "instance_1", config.getName());
		assertEquals("Instance Backend:", "org.hawk.orientdb.OrientDatabase", config.getBackend());
		assertEquals("Instance Delay Max:", 512000, config.getDelayMax());
		assertEquals("Instance Delay Min:", 5000, config.getDelayMin());
		
		assertEquals("Instance Number of Plugins:", 6, config.getPlugins().size());
		assertTrue(config.getPlugins().contains("org.hawk.emf.metamodel.EMFMetaModelResourceFactory"));
		assertTrue(config.getPlugins().contains("org.hawk.emf.model.EMFModelResourceFactory"));
		assertTrue(config.getPlugins().contains("org.hawk.graph.syncValidationListener.SyncValidationListener"));
		assertTrue(config.getPlugins().contains("org.hawk.modelio.exml.listeners.ModelioGraphChangeListener"));
		assertTrue(config.getPlugins().contains("org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory"));
		assertTrue(config.getPlugins().contains("org.hawk.modelio.exml.model.ModelioModelResourceFactory"));
		
		assertEquals("Instance Number of Metamodels:", 1, config.getMetamodels().size());
		assertEquals("C:/Users/yaser And Orjuwan/Desktop/Hawk/metamodel_descriptor.xml",config.getMetamodels().get(0).getLocation());
		assertEquals("",config.getMetamodels().get(0).getUri());
		
		assertEquals("Instance Number of Derived Attributes:", 1, config.getDerivedAttributes().size());
		assertEquals("modelio://Modeliosoft.Standard/2.0.00", config.getDerivedAttributes().get(0).getMetamodelUri());
		assertEquals("Class", config.getDerivedAttributes().get(0).getTypeName());
		assertEquals("ownedOperationCount", config.getDerivedAttributes().get(0).getAttributeName());
		assertEquals("Integer", config.getDerivedAttributes().get(0).getAttributeType());
		
		assertEquals(false, config.getDerivedAttributes().get(0).isMany());
		assertEquals(false, config.getDerivedAttributes().get(0).isOrdered());
		assertEquals(false, config.getDerivedAttributes().get(0).isUnique());
		
		assertEquals("EOLQueryEngine", config.getDerivedAttributes().get(0).getDerivationLanguage());
		assertEquals("\n\t\t\t\t\t\treturn Class.all.select(c|c.ownedOperationCount > 0).Name;\n\t\t\t\t\t", config.getDerivedAttributes().get(0).getDerivationLogic());

		
	
		assertEquals("Instance Number of Indexed Attributes:", 1, config.getIndexedAttributes().size());
		assertEquals("modelio://Modeliosoft.Standard/2.0.00", config.getIndexedAttributes().get(0).getMetamodelUri());
		assertEquals("Class", config.getIndexedAttributes().get(0).getTypeName());
		assertEquals("Name", config.getIndexedAttributes().get(0).getAttributeName());
		
		assertEquals("Instance Number of :Repositories", 1, config.getRepositories().size());
		assertEquals("C:/Users/yaser And Orjuwan/Desktop/Hawk/Zoo", config.getRepositories().get(0).getLocation());
		assertEquals("org.hawk.localfolder.LocalFolder", config.getRepositories().get(0).getType());
		assertEquals("", config.getRepositories().get(0).getUser());
		assertEquals("", config.getRepositories().get(0).getPass());
		assertEquals(false, config.getRepositories().get(0).isFrozen());
	}


	private void parseXml() {
		parser = new ConfigFileParser();

		try {
			parser.setSchemaFile(xsdFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		config = parser.parse(new File(xmlFilePath));
	}


	@Test
	public void testSaveConfigToFile() {
		parseXml();	
		parser.saveConfigAsXml(config);
		testParseFile();
	}


}

