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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.activemq.artemis.utils.FileUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.api.Hawk.Processor;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.servlet.processors.HawkThriftIface;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.io.Files;

public class HawkServerConfiguratorTest {
	private static final String SERVERCONFIG_PATH = "resources/ServerConfig/";
	static final String xmlFileName_1 = "instance_1.xml";
	static final String xmlFileName_2 = "instance_2.xml";

	static HawkThriftIface hawkIface;
	static HawkServerConfigurator serverConfigurator;
	static HManager manager;

	@BeforeClass
	static public void setup() {
		manager = HManager.getInstance();
		addFilesToConfigurationFolder();
		hawkIface = new HawkThriftIface(ThriftProtocol.TUPLE, null, null);
		serverConfigurator = new HawkServerConfigurator(hawkIface);
		
	}

	
	@Test
	public void testHawkServerConfigurator() throws BackingStoreException {
		serverConfigurator.loadHawkServerConfigurations();
		testhawkInstance(xmlFileName_1);
		testhawkInstance(xmlFileName_2);
		manager.stopAllRunningInstances(IModelIndexer.ShutdownRequestType.ALWAYS);
	}
	

	void testhawkInstance(String xmlFileName) {
		ConfigFileParser parser = new ConfigFileParser();
		HawkInstanceConfig config = parser.parse(new File(SERVERCONFIG_PATH, xmlFileName));
		
		
		// check first instance
		HModel instance = manager.getHawkByName(config.getName());
		
		assertEquals(config.getName(), instance.getName());
		
		assertEquals(config.getBackend(), instance.getDbType());
		
		assertArrayEquals(config.getPlugins().toArray(), instance.getEnabledPlugins().toArray());
		
		assertTrue(compareRepositories(config.getRepositories(), instance.getRunningVCSManagers()));

		
		// metamodels
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://ModelioMetaPackage"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Archimate/1.0.03"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Analyst/2.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.modelio.kernel/1.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Standard/2.0.00"));
		assertTrue(instance.getRegisteredMetamodels().contains("modelio://Modeliosoft.Infrastructure/2.1.00"));
		 
		
		// derived attributes
		assertTrue(instance.getDerivedAttributes().containsAll(config.getDerivedAttributes()));
		
		
		// indexed attributes
		assertTrue(instance.getIndexedAttributes().containsAll(config.getIndexedAttributes()));
		
		
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


	static public void addFilesToConfigurationFolder() {
		try {
			URL installURL = Platform.getConfigurationLocation().getURL();
			String path = FileLocator.toFileURL(installURL).getPath();

			File configurationFolder = new File(path, "configuration");

			if (!configurationFolder.exists() || !configurationFolder.isDirectory()) {
				configurationFolder.mkdir(); // make directory
			}

			
			Files.copy(new File(SERVERCONFIG_PATH, xmlFileName_1), new File(configurationFolder, xmlFileName_1));
			Files.copy(new File(SERVERCONFIG_PATH, xmlFileName_2), new File(configurationFolder, xmlFileName_2));
			

			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
//	public List<File> getHawkServerConfigurationFiles() {
//		List files = new ArrayList<File>();
//		files.add(new File(xmlFilePath));
//		return files;
//	}
	
	
	@Test
	public void testLoadHawkServerConfigurations() {
	}

	@Test
	public void testSaveHawkServerConfigurations() {
	}

}

