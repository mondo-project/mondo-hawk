/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Orjuwan Al-Wadeai - Hawk Server Configurator Test
 ******************************************************************************/
package org.hawk.service.servlet.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IVcsManager;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.servlet.processors.HawkThriftIface;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;

public class HawkServerConfiguratorTest {
	private static final String SERVERCONFIG_PATH = "resources/ServerConfig/";

	private static final String metamodelDescriptor = "resources/metamodel/metamodel_descriptor.xml";
	private static final String modelsZoo = "resources/Zoo";

	static final String xmlFileName_1 = "instance_1.xml";
	static final String xmlFileName_2 = "instance_2.xml";

	static HManager manager;
	static File configurationFolder;

	private static HawkThriftIface hawkIface;

	private static HawkServerConfigurator serverConfigurator;

	private boolean finishedRetrievingInstanceInfo;
	
	Collection<IVcsManager> repos;
	Collection<IndexedAttributeParameters> derivedAttributes;
	Collection<IndexedAttributeParameters> indexedAttributes;
	List<String> metamodels;
	
	@BeforeClass
	static public void setup() {
		initConfigurationFolder();
		setupAndCopyPathsInXmlFile(xmlFileName_1);
		setupAndCopyPathsInXmlFile(xmlFileName_2);
		manager = HManager.getInstance();
		hawkIface = new HawkThriftIface(ThriftProtocol.TUPLE, null, null);
		serverConfigurator = new HawkServerConfigurator(hawkIface);
		serverConfigurator.loadHawkServerConfigurations();		
	}

	@Test
	public void testHawkServerConfigurator_instance1() throws Exception {		
		testhawkInstance(xmlFileName_1);
	}

	@Test
	public void testHawkServerConfigurator_instance2() throws Exception {
		testhawkInstance(xmlFileName_2);
	}

	private void testhawkInstance(String xmlFileName) throws Exception {
		final ConfigFileParser parser = new ConfigFileParser();
		final HawkInstanceConfig config = parser.parse(new File(SERVERCONFIG_PATH, xmlFileName));
		final HModel instance = manager.getHawkByName(config.getName());

		assertTrue(instance.isRunning());
		assertEquals(config.getName(), instance.getName());
		assertEquals(config.getBackend(), instance.getDbType());
		assertArrayEquals(config.getPlugins().toArray(), instance.getEnabledPlugins().toArray());
		assertTrue(hawkIface.listPluginDetails().size() > 0);

		finishedRetrievingInstanceInfo = false;
		instance.configurePolling(0, 0);

		instance.getHawk().getModelIndexer().scheduleTask(new Callable<Void>(){
			@Override
			public Void call() {
				metamodels = instance.getRegisteredMetamodels();
				repos = instance.getRunningVCSManagers();
				derivedAttributes = instance.getDerivedAttributes();
				indexedAttributes = instance.getIndexedAttributes();
				finishedRetrievingInstanceInfo = true;
				return null;
			}
		}, 0);;
		
		while(!finishedRetrievingInstanceInfo) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// repositories
		assertTrue(compareRepositories(config.getRepositories(), repos));

		//		 metamodels
		assertTrue(metamodels.contains("modelio://ModelioMetaPackage"));
		assertTrue(metamodels.contains("modelio://Modeliosoft.Archimate/1.0.03"));
		assertTrue(metamodels.contains("modelio://Modeliosoft.Analyst/2.0.00"));
		assertTrue(metamodels.contains("modelio://Modeliosoft.modelio.kernel/1.0.00"));
		assertTrue(metamodels.contains("modelio://Modeliosoft.Standard/2.0.00"));
		assertTrue(metamodels.contains("modelio://Modeliosoft.Infrastructure/2.1.00"));
		
		// derived attributes
		assertTrue(derivedAttributes.size() > 0);
		assertTrue(derivedAttributes.contains(config.getDerivedAttributes().get(0)));

		// indexed attributes
		assertEquals(indexedAttributes.size(), config.getIndexedAttributes().size());
		assertTrue(indexedAttributes.contains(config.getIndexedAttributes().get(0)));
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

