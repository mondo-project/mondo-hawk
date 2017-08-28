/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Hawk Server Configurator Test
 ******************************************************************************/
package org.hawk.service.servlet.config;

import static org.junit.Assert.*;

import org.hawk.core.IModelIndexer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hawk.service.servlet.Activator;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.servlet.processors.HawkThriftIface;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.io.Files;

public class HawkServerConfiguratorTest {
	private static final String SERVERCONFIG_PATH = "resources/ServerConfig/";

	private static final String metamodelDescriptor = "resources/metamodel/metamodel_descriptor.xml";
	private static final String modelsZoo = "resources/Zoo";

	static final String xmlFileName_1 = "instance_1.xml";

	static HManager manager;
	static File configurationFolder;

	private static HawkThriftIface hawkIface;

	private static HawkServerConfigurator serverConfigurator;

	@BeforeClass
	static public void setup() {
		initConfigurationFolder();
		setupAndCopyPathsInXmlFile(xmlFileName_1);
		manager = HManager.getInstance();
		hawkIface = new HawkThriftIface(ThriftProtocol.TUPLE, null, null);
		serverConfigurator = new HawkServerConfigurator(hawkIface);
		serverConfigurator.loadHawkServerConfigurations();

	}

	@AfterClass
	static public void teardown() {
		manager.stopAllRunningInstances(IModelIndexer.ShutdownRequestType.ALWAYS);
	}

	@Test
	public void testHawkServerConfigurator_instance1() {
		testhawkInstance(xmlFileName_1);
	}


	private void testhawkInstance(String xmlFileName) {
		ConfigFileParser parser = new ConfigFileParser();
		HawkInstanceConfig config = parser.parse(new File(SERVERCONFIG_PATH, xmlFileName));

		// check first instance
		HModel instance = manager.getHawkByName(config.getName());
		

		assertTrue(instance.isRunning());

		assertEquals(config.getName(), instance.getName());

		assertEquals(config.getBackend(), instance.getDbType());

		assertArrayEquals(config.getPlugins().toArray(), instance.getEnabledPlugins().toArray());

		assertTrue(compareRepositories(config.getRepositories(), instance.getRunningVCSManagers()));

		//		 metamodels
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://ModelioMetaPackage"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Archimate/1.0.03"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Analyst/2.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.modelio.kernel/1.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Standard/2.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Infrastructure/2.1.00"));


		//System.out.println("***********getDerivedAttributes size: " + instance.getDerivedAttributes().size());
		//assertTrue(instance.getDerivedAttributes().size() > 0);
		//System.out.println("***********getIndexedAttributes size: " + instance.getIndexedAttributes().size());
		//assertEquals(instance.getIndexedAttributes().size(), config.getIndexedAttributes().size());
		//assertTrue(instance.getIndexedAttributes().contains(config.getIndexedAttributes().get(0)));
		
		instance.stop(IModelIndexer.ShutdownRequestType.ALWAYS);

	}
	
	private boolean compareRepositories(List<RepositoryParameters> repositories, Collection<IVcsManager> collection) {

		if(repositories.size() != collection.size()) {
			return false;
		}

		boolean allEqual = true;
		for (IVcsManager vcsManager : collection) {
			boolean FoundAndEqual = false;
			for(RepositoryParameters repos : repositories) {
				if( repos.getType().equals(vcsManager.getType()) && 
						repos.getLocation().equals(vcsManager.getLocation()) &&
						repos.isFrozen() == vcsManager.isFrozen())
				{
					FoundAndEqual = true;
					if(vcsManager.getUsername() != null) {
						FoundAndEqual = repos.getUser().equals(vcsManager.getUsername()); 
					} else {
						FoundAndEqual = (repos.getUser().isEmpty() || repos.getUser() == null);
					}

					if(vcsManager.getPassword() != null) {
						FoundAndEqual = repos.getUser().equals(vcsManager.getPassword()); 
					} else {
						FoundAndEqual = (repos.getPass().isEmpty() || repos.getPass() == null);
					}

					break;
				}
			}

			if(!FoundAndEqual) {
				allEqual = false;
				break;
			}
		}

		return allEqual;
	}



	static public void setupAndCopyPathsInXmlFile(String xmlFileName) {
		ConfigFileParser parser = new ConfigFileParser();
		HawkInstanceConfig config = parser.parse(new File(SERVERCONFIG_PATH, xmlFileName));
		File location = new File(metamodelDescriptor);
		config.getMetamodels().get(0).setLocation(location.getAbsolutePath());
		config.getRepositories().get(0).setLocation(getLocalFilePath(modelsZoo));
		parser.saveConfigAsXml(config);

		// copy
		try {
			Files.copy(new File(SERVERCONFIG_PATH, xmlFileName), new File(configurationFolder, xmlFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static public String getLocalFilePath(String fileLocation) {
		String repositoryURI = fileLocation;

		try {
			Path path;
			path = Paths.get(fileLocation);

			File canonicalFile = null;
			canonicalFile = path.toFile().getCanonicalFile();

			Path rootLocation = canonicalFile.toPath();
			repositoryURI = rootLocation.toUri().toString();

		} catch (IOException e) {
			e.printStackTrace();
		}


		return repositoryURI;
	}


/*	static public void deleteConfigurationFolder() {
		URL installURL = Platform.getConfigurationLocation().getURL();
		String path = null;
		try {
			path = FileLocator.toFileURL(installURL).getPath();


			File configurationFolder = new File(path);

			if (configurationFolder.exists() && configurationFolder.isDirectory()) {
				configurationFolder.delete();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}*/



	static public void initConfigurationFolder() {
		try {
			URL installURL = Platform.getConfigurationLocation().getURL();
			String path = FileLocator.toFileURL(installURL).getPath();

			configurationFolder = new File(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

