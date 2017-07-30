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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IVcsManager;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.servlet.config.ConfigFileParser;
import org.hawk.service.servlet.config.HawkInstanceConfig;
import org.hawk.service.servlet.config.RepositoryParameters;

public class HawkServerConfigurator  {
	List<HawkInstanceConfig> hawkInstanceConfigs;
	Iface iface;
	HManager manager;
	ConfigFileParser parser;

	public HawkServerConfigurator(Iface iface) {
		this.iface = iface;

		hawkInstanceConfigs = new ArrayList<HawkInstanceConfig>();
		manager = HManager.getInstance();
		parser = new ConfigFileParser();
	}

	public void loadHawkServerConfigurations() {
		for (File file : getHawkServerConfigurationFiles()) {
			HawkInstanceConfig config = parser.parse(file);
			if(config != null) {
				hawkInstanceConfigs.add(config); // add to list
				configureHawkInstance(config);
			}
		}

		// save to configuration file (some new attributes and metamodels might be added to instance)
		saveHawkServerConfigurations();
	}

	public void saveHawkServerConfigurations() {
		for(HawkInstanceConfig config : hawkInstanceConfigs) {
			saveHawkInstanceConfig(config);
		}
	}

	private List<File> getHawkServerConfigurationFiles() {
		try {
			URL installURL = Platform.getConfigurationLocation().getURL();
			String path = FileLocator.toFileURL(installURL).getPath();

			File configurationFolder = new File(path, "configuration");

			FilenameFilter filter = getXmlFilenameFilter();

			if (configurationFolder.exists() && configurationFolder.isDirectory()) {
				return new ArrayList<File>(Arrays.asList(configurationFolder.listFiles(filter)));
			} else {
				configurationFolder.mkdir(); // make directory
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	private FilenameFilter getXmlFilenameFilter() {
		return new FilenameFilter() {

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
	}

	private void configureHawkInstance(HawkInstanceConfig config) {
		HModel hawkInstance = manager.getHawkByName(config.getName());

		try {
			if (hawkInstance == null) {
				hawkInstance = createHawkInstance(config); // create new instance
			} 

			/** apply configuration */
			// TODO set db, need to check if changed 
			// hawkInstance.setDbType(config.getBackend());
			
			addMissingPlugins(hawkInstance, config);	

			hawkInstance.configurePolling(config.getDelayMin(), config.getDelayMax());

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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HModel createHawkInstance(HawkInstanceConfig config) throws TException {
		
		/** use iface.createInstance (instead of HModel.create) to set the right storage folder */
		iface.createInstance(config.getName(), config.getBackend(),
				config.getDelayMin(), config.getDelayMax(),
				config.getPlugins());
		
		HModel hawkInstance = manager.getHawkByName(config.getName());
		
		return hawkInstance;
	}

	private void addMissingDerivedAttributes(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		Collection<IndexedAttributeParameters> existingDerivedAttributes = hawkInstance.getDerivedAttributes();

		List<String> metamodels = hawkInstance.getRegisteredMetamodels();

		for (DerivedAttributeParameters params : config.getDerivedAttributes()) {
			if(!existingDerivedAttributes.contains(params)) {
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
			}
		}
	}

	private void addMissingIndexedAttributes(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		List<String> metamodels = hawkInstance.getRegisteredMetamodels();
		Collection<IndexedAttributeParameters> existingIndexedAttributes = hawkInstance.getIndexedAttributes();

		for (IndexedAttributeParameters params : config.getIndexedAttributes()) {
			if (!existingIndexedAttributes.contains(params)) {
				if(metamodels.contains(params.getMetamodelUri())) {
					hawkInstance.addIndexedAttribute(params.getMetamodelUri(), params.getTypeName(), params.getAttributeName());
				} else {
					System.err.println("HawkServerConfigurator.addMissingIndexedAttributes: metamodel " + params.getMetamodelUri() + " is not registered!");
				}
			}
		}
	}

	private void addMissingRepositories(HModel hawkInstance, HawkInstanceConfig config) {
		Collection<String> existingLocations = hawkInstance.getLocations();

		for (RepositoryParameters params : config.getRepositories()) {
			if (!existingLocations.contains(params.getLocation())) {
				hawkInstance.addVCS(params.getLocation(), params.getType(), params.getUser(), params.getPass(), params.isFrozen());
			}
		}
	}

	private void addMetamodels(HModel hawkInstance, HawkInstanceConfig config) {
		for (MetamodelParameters params : config.getMetamodels()) {
			if(params.getLocation() != null && !params.getLocation().isEmpty()) {
				File file = new File(params.getLocation());
				hawkInstance.registerMeta(file);
			}
		}
	}

	private void addMissingPlugins(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		List<String> availableplugins = manager.getAvailablePlugins();
		List<String> existingplugins = hawkInstance.getEnabledPlugins();
		List<String> missingPlugins = new ArrayList<String>();

		for (String plugin : config.getPlugins()) {
			if(availableplugins.contains(plugin)) {
				if (!existingplugins.contains(plugin)) {
					missingPlugins.add(plugin);
				}
			} else {
				System.err.println("HawkServerConfigurator.addMissingPlugins: plugin " + plugin + " is not available!");
			}
		}

		if (!missingPlugins.isEmpty()) {
			hawkInstance.addPlugins(missingPlugins);
			manager.saveHawkToMetadata(hawkInstance, true);
		}
	}

	private void saveHawkInstanceConfig(HawkInstanceConfig config) {
		HModel hawkInstance =  	manager.getHawkByName(config.getName());

		/** delay, backend and plugins don't change during running of an instance, set it anyway */
		config.setBackend(hawkInstance.getDbType());

		/** Only successfully-added plugins are saved */ 
		if(hawkInstance.getEnabledPlugins() != null) {
			config.setPlugins(hawkInstance.getEnabledPlugins());
		}
		
		/**  
		 * 	The following Elements are saved (the ones originally in the file(even if was unsuccessful), and the ones added during hawk instance running:
		 *  - Metamodels
		 *  - Derived Attributes
		 * 	- Indexed Attributes
		 * 	- Repositories
		 * */
		addNewMetamodelsUriToConfig(hawkInstance, config);
		addNewDerivedAttributesToConfig(hawkInstance, config);
		addNewIndexedAttributesToConfig(hawkInstance, config);
		addNewRepositoriesToConfig(hawkInstance, config);

		parser.saveConfigAsXml(config);
	}

	private void addNewMetamodelsUriToConfig(HModel hawkInstance, HawkInstanceConfig config) {
		List<MetamodelParameters> newMetamodels = new ArrayList<MetamodelParameters>();

		for (String metamodelUri : hawkInstance.getRegisteredMetamodels()) {
			boolean isNew = true;
			for(MetamodelParameters params : config.getMetamodels()) {
				if(params.getUri().equals(metamodelUri)) {
					isNew = false;
					break;
				}
			}

			if(isNew) {
				newMetamodels.add(new MetamodelParameters(metamodelUri, null));
			}
		}

		if(!newMetamodels.isEmpty()) {
			config.getMetamodels().addAll(newMetamodels);
		}
	}

	private void addNewDerivedAttributesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		Collection<IndexedAttributeParameters> instanceAttributes = hawkInstance.getDerivedAttributes();
		List<DerivedAttributeParameters> configAttributes= config.getDerivedAttributes();		
		List<DerivedAttributeParameters> newAttrs = new ArrayList<DerivedAttributeParameters>();

		for(IndexedAttributeParameters indexedAttribute : instanceAttributes) {
			if(indexedAttribute instanceof DerivedAttributeParameters) {
				boolean isNew = true;
				for(DerivedAttributeParameters params : configAttributes) {
					if(indexedAttribute.equals(params)) {
						isNew = false;
						break;
					}
				}

				if(isNew && indexedAttribute != null) {
					newAttrs.add((DerivedAttributeParameters) indexedAttribute);
				}
			}
		}

		if(!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}
	}

	private void addNewIndexedAttributesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		Collection<IndexedAttributeParameters> instanceAttributes = hawkInstance.getIndexedAttributes();
		List<IndexedAttributeParameters> configAttributes= config.getIndexedAttributes();		
		List<IndexedAttributeParameters> newAttrs = new ArrayList<IndexedAttributeParameters>();

		for(IndexedAttributeParameters indexedAttribute : instanceAttributes) {
			boolean isNew = true;
			for(IndexedAttributeParameters params : configAttributes) {
				if(indexedAttribute.equals(params)) {
					isNew = false;
					break;
				}
			}

			if(isNew && indexedAttribute != null) {
				newAttrs.add(indexedAttribute);
			}
		}

		if(!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}

	}

	private void addNewRepositoriesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		List<RepositoryParameters> newRepos = new ArrayList<RepositoryParameters>();

		for (IVcsManager vcsManager : hawkInstance.getRunningVCSManagers()) {
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
