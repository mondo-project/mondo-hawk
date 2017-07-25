/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Initial Implementation of Hawk Server Configuration
 ******************************************************************************/
package org.hawk.service.servlet.config;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IVcsManager;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.servlet.config.ConfigFileParser;
import org.hawk.service.servlet.config.DerivedAttributeParameters;
import org.hawk.service.servlet.config.HawkInstanceConfig;
import org.hawk.service.servlet.config.IndexedAttributeParameters;
import org.hawk.service.servlet.config.RepositoryParameters;
import org.xml.sax.InputSource;

public class HawkServerConfigurator {
	List<HawkInstanceConfig> hawkInstanceConfigs;
	Iface iface;
	HManager manager;

	public HawkServerConfigurator(Iface iface) {
		this.iface = iface;

		hawkInstanceConfigs = new ArrayList<HawkInstanceConfig>();
		manager = HManager.getInstance();
	}

	void loadHawkServerConfigurations() {
		// get all files and loop through them
	}

	void saveHawkServerConfigurations() {

	}

	List<File> getHawkServerConfigurationFiles() {
		URL installURL = Platform.getConfigurationLocation().getURL();
		// installURL = Platform.getInstallLocation().getURL();
		// installURL = Platform.getInstanceLocation().getURL();
		// installURL = Platform.g().getURL();
		String protocol;
		try {
			protocol = FileLocator.toFileURL(installURL).getProtocol();

			String path = FileLocator.toFileURL(installURL).getPath();
			System.err.println(path + "   " + protocol);
			File configurationFolder = new File(path + "\\configuration");
			FilenameFilter filter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.endsWith(".xml")) {
						return true;
					} else {
						return false;
					}
				}
			};

			if (configurationFolder.exists()
					&& configurationFolder.isDirectory()) {
				// get all files
				return new ArrayList<File>(Arrays.asList(configurationFolder
						.listFiles(filter)));
			} else {
				configurationFolder.mkdir();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	private void configureHawkInstances() {
		for (File file : getHawkServerConfigurationFiles()) {
			try {
				//InputSource inputSource = new InputSource(new FileReader(file));
				ConfigFileParser parser = new ConfigFileParser();
				HawkInstanceConfig config = parser.parse(file);
				hawkInstanceConfigs.add(config);

				HModel hawkInstance = manager.getHawkByName(
						config.getName());

				// create a new instance it is not present
				if (hawkInstance == null) {
					// create new instance
					iface.createInstance(config.getName(), config.getBackend(),
							config.getDelayMin(), config.getDelayMax(),
							config.getPlugins());
					hawkInstance = manager.getHawkByName(
							config.getName());
				}

				// apply configuration
				if (hawkInstance != null) {


					// check parameters and if different change
					hawkInstance.configurePolling(config.getDelayMin(),
							config.getDelayMax());

					// add new plugins, don't delete
					addMissingPlugins(hawkInstance, config);

					// set Db
					//hawkInstance.getHawk().setDbtype(config.getBackend());

					manager.saveHawkToMetadata(hawkInstance);
					//manager.getHawks();

					// start instance
					hawkInstance.start(manager);

					while(!hawkInstance.isRunning());

					// add metamodels, Do it first before adding attributes or repositories
					addMetamodels(hawkInstance, config);

					// add repositories, don't delete any
					addMissingRepositories(hawkInstance, config);

					// derived Attributes
					addMissingDerivedAttributes(hawkInstance, config);

					// indexed Attributes
					addMissingIndexedAttributes(hawkInstance, config);



				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void addMissingDerivedAttributes(HModel hawkInstance,
			HawkInstanceConfig config) {
		Collection<String> existingDerivedAttributes = hawkInstance.getDerivedAttributes();
		List<String> metamodels = hawkInstance.getRegisteredMetamodels();

		for (DerivedAttributeParameters params : config.getDerivedAttributes()) {
			String derivedParameterString = attributeToString(params);


			if (!existingDerivedAttributes.contains(derivedParameterString)) {
				try {
					// check metamodel exist 
					if(metamodels.contains(params.getMetamodelUri())){
						hawkInstance.addDerivedAttribute(params.getMetamodelUri(),
								params.getTypeName(), params.getAttributeName(),
								params.getAttributeType(), params.isMany(),
								params.isOrdered(), params.isUnique(),
								params.getDerivationLanguage(),
								params.getDerivationLogic());
					} else {
						System.err.println("HawkServerConfigurator.addMissingDerivedAttributes: metamodel " + params.getMetamodelUri() + " is not registered!");

					}




				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void addMissingIndexedAttributes(HModel hawkInstance,
			HawkInstanceConfig config) {
		List<String> metamodels = hawkInstance.getRegisteredMetamodels();

		Collection<String> existingIndexedAttributes = hawkInstance
				.getIndexedAttributes();

		for (IndexedAttributeParameters params : config.getIndexedAttributes()) {
			String indexedParameterString = attributeToString(params);

			if (!existingIndexedAttributes.contains(indexedParameterString)) {
				try {

					// check metamodel exist 

					if(metamodels.contains(params.getMetamodelUri())) {
						hawkInstance.addIndexedAttribute(params.getMetamodelUri(), params.getTypeName(), params.getAttributeName());
					} else {
						System.err.println("HawkServerConfigurator.addMissingIndexedAttributes: metamodel " + params.getMetamodelUri() + " is not registered!");

					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String attributeToString(IndexedAttributeParameters params) {
		String indexedParameterString = String.format("%s::%s::%s", params.getMetamodelUri(), params.getTypeName(), params.getAttributeName());
		return indexedParameterString;
	}

	private IndexedAttributeParameters stringToAttribute(String string) {
		String[] data = string.split("::");
		if(data.length == 3) {
			return (new IndexedAttributeParameters(data[0], data[1], data[2]));
		}
		return null; 
	}

	private void addMissingRepositories(HModel hawkInstance,
			HawkInstanceConfig config) {
		Collection<String> existingLocations = hawkInstance.getLocations();

		for (RepositoryParameters params : config.getRepositories()) {
			if (!existingLocations.contains(params.getLocation())) {
				hawkInstance.addVCS(params.getLocation(), params.getType(),
						params.getUser(), params.getPass(), params.isFrozen());
			}
		}
	}

	private void addMetamodels(HModel hawkInstance, HawkInstanceConfig config) {

		for (String metamodelPath : config.getMetamodels()) {
			File file = new File(metamodelPath);
			try {
				hawkInstance.registerMeta(file);
			} catch (Exception e) {
				System.err.println("HawkServerConfigurator.addMetamodels: metamodel " + metamodelPath + " failed!");

				e.printStackTrace();
			}
		}
	}

	void addMissingPlugins(HModel hawkInstance, HawkInstanceConfig config)  {
		List<String> availableplugins = manager.getAvailablePlugins();
		List<String> existingplugins = hawkInstance.getEnabledPlugins();
		List<String> missingPlugins = new ArrayList<String>();

		for (String plugin : config.getPlugins()) {

			// check plugin is available
			if(availableplugins.contains(plugin)) {
				if (!existingplugins.contains(plugin)) {
					missingPlugins.add(plugin);
				}
			} else {
				System.err.println("HawkServerConfigurator.addMissingPlugins: plugin " + plugin + " is not available!");
				// TODO list available plugins
			}
		}

		if (!missingPlugins.isEmpty()) {
			try {
				hawkInstance.addPlugins(missingPlugins);
			} catch (Exception e) {
				System.err.println("HawkServerConfigurator.addMissingPlugins: plugin Failed!");
				e.printStackTrace();
			}
		}
	}

	public void loadConfig() {
		configureHawkInstances();
	}

	public void saveConfig() {
		for(HawkInstanceConfig config : hawkInstanceConfigs) {

			// find file or create one and save all info
			HModel hawkInstance =  	manager.getHawkByName(config.getName());

			// there are no way to change delay backend and plugins, but save it anyway
			config.setBackend(hawkInstance.getDbType());
			if(hawkInstance.getEnabledPlugins() != null) {
				config.setPlugins(hawkInstance.getEnabledPlugins());
			}


			// what is the point of saving these values , they are saved in the instance anyways and no need to store
			// save registered metamodels
			if(hawkInstance.getRegisteredMetamodels() != null) {
				//config.getMetamodels().addAll(hawkInstance.getRegisteredMetamodels());
			}


			// save all derived attributes, cannot get derivation language and logic
			addNewDerivedAttributesToConfig(hawkInstance.getDerivedAttributes(), config.getDerivedAttributes());
			
			// save all indexed attributes
			addNewIndexedAttributesToConfig(hawkInstance.getIndexedAttributes(), config.getIndexedAttributes());

			// save all Repositories
			addNewRepositoriesToConfig(hawkInstance, config);

		}
	}
	
	private void addNewDerivedAttributesToConfig(Collection<String> instanceAttributes,
			List<DerivedAttributeParameters> configAttributes) {
		List<DerivedAttributeParameters> newAttrs = new ArrayList<DerivedAttributeParameters>();

		for(String indexedAttribute : instanceAttributes) {
			boolean isNew = true;
			for(IndexedAttributeParameters params : configAttributes) {
				if(indexedAttribute.equals(attributeToString(params))) {
					isNew = false;
					break;
				}
			}

			if(isNew) {
				DerivedAttributeParameters params = (DerivedAttributeParameters) stringToAttribute(indexedAttribute);
				if(params != null) 
					newAttrs.add(params);
			}
		}
		
		if(!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}
	}

	private void addNewIndexedAttributesToConfig(Collection<String> instanceAttributes, List<IndexedAttributeParameters> configAttributes)
	 {

		List<IndexedAttributeParameters> newAttrs = new ArrayList<IndexedAttributeParameters>();

		for(String indexedAttribute : instanceAttributes) {
			boolean isNew = true;
			for(IndexedAttributeParameters params : configAttributes) {
				if(indexedAttribute.equals(attributeToString(params))) {
					isNew = false;
					break;
				}
			}

			if(isNew) {
				IndexedAttributeParameters params = stringToAttribute(indexedAttribute);
				if(params != null) 
					newAttrs.add(params);
			}
		}
		
		if(!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}

	}
	
	
	

	private void addNewRepositoriesToConfig(HModel hawkInstance,
			HawkInstanceConfig config) {

		List<RepositoryParameters> newRepos = new ArrayList<RepositoryParameters>();

		for (IVcsManager vcsManager : hawkInstance.getVCSInstances()) {
			boolean isNew = true;
			for(RepositoryParameters repos : config.getRepositories()) {
				if(repos.getType().equals(vcsManager.getType()) && 
						repos.getLocation().equals(vcsManager.getLocation())) {

					repos.setFrozen(vcsManager.isFrozen());
					repos.setUser(vcsManager.getUsername());
					repos.setPass(vcsManager.getPassword());
					isNew = false;
					break;
				}
			}

			if(isNew) {
				RepositoryParameters newRepo = new RepositoryParameters(vcsManager.getType(),
						vcsManager.getLocation(),
						vcsManager.getUsername(),
						vcsManager.getPassword(),
						vcsManager.isFrozen());
				newRepos.add(newRepo);
			}
		}

		if(!newRepos.isEmpty()) {
			config.getRepositories().addAll(newRepos);
		}
	}


	
}
