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
import java.util.concurrent.Callable;

import org.apache.thrift.TException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Hawk.Iface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HawkServerConfigurator  {

	private static final Logger LOGGER = LoggerFactory.getLogger(HawkServerConfigurator.class);

	private List<HawkInstanceConfig> hawkInstanceConfigs;
	private Iface iface;
	private HManager manager;
	private ConfigFileParser parser;
	
	private int numberOfConfiguredInstances = 0;
	
	public HawkServerConfigurator(Iface iface) {
		this.iface = iface;

		hawkInstanceConfigs = new ArrayList<HawkInstanceConfig>();
		manager = HManager.getInstance();
		parser = new ConfigFileParser();
		numberOfConfiguredInstances = 0;
	}
	
	public int getNumberOfConfiguredInstances() {
		return numberOfConfiguredInstances;
	}
	
	public void loadHawkServerConfigurations() {
		numberOfConfiguredInstances = 0;
		int configCount = 0;
		for (File file : getHawkServerConfigurationFiles()) {
			LOGGER.info("Configuring hawk instances from {}", file.getName());
			HawkInstanceConfig config = parser.parse(file);
			if(config != null) {
				LOGGER.info("Configuring Hawk instance {}", config.getName());
				hawkInstanceConfigs.add(config); // add to list
				configureHawkInstance(config);
			}
			
			configCount++;
			while(numberOfConfiguredInstances < configCount) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<File> getHawkServerConfigurationFiles() {
		try {
			File configurationFolder = null;
			Location configurationLocation = Platform.getConfigurationLocation();
			if(configurationLocation == null) {
				// create a configuration folder
				configurationFolder = new File("configuration");
			} else {
				URL configurationURL = configurationLocation.getURL();
				String path = FileLocator.toFileURL(configurationURL).getPath();
				configurationFolder = new File(path);
			}

			LOGGER.info("Looking for configuration files in {}", configurationFolder.getAbsolutePath());
			FilenameFilter filter = getXmlFilenameFilter();
			if (configurationFolder.exists() && configurationFolder.isDirectory()) {
				return new ArrayList<File>(Arrays.asList(configurationFolder.listFiles(filter)));
			} else {
				configurationFolder.mkdir(); // make directory
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
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

	private void configureHawkInstance(final HawkInstanceConfig config) {
		HModel hawkInstance = manager.getHawkByName(config.getName());

		try {
			if (hawkInstance == null) {
				hawkInstance = createHawkInstance(config); // create new instance
			} 

			/** apply configuration */
			// TODO set db, need to check if changed 
			// hawkInstance.setDbType(config.getBackend());

			addMissingPlugins(hawkInstance, config);	
			
			// set polling to 0,0, set to required polling later when config is done
			hawkInstance.configurePolling(0, 0);

			// start instance
			hawkInstance.start(manager);
			hawkInstance.getIndexer().waitFor(HawkState.RUNNING , 3000);

			final HModel hModel = hawkInstance;
			hawkInstance.getHawk().getModelIndexer().scheduleTask(new Callable<Void>(){
				@Override
				public Void call() {
					// add metamodels, Do it first before adding attributes or repositories
					try { 
						addMetamodels(hModel, config);
					} catch (Exception e) {
						LOGGER.error("Configuring Hawk instance: Exception when adding metamodels");
					}

					// add repositories, don't delete any
					addMissingRepositories(hModel, config);

					try {
						// derived Attributes
						addMissingDerivedAttributes(hModel, config);

						// indexed Attributes
						addMissingIndexedAttributes(hModel, config);
					} catch (Exception e) {
						LOGGER.error("Configuring Hawk instance: Exception when adding derived/indexed attributes", e);
					}

					// set the required polling values
					hModel.configurePolling(config.getDelayMin(), config.getDelayMax());

					numberOfConfiguredInstances++;
					return null;
				}
			}, 0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HModel createHawkInstance(HawkInstanceConfig config) throws TException {
		
		/** use iface.createInstance (instead of HModel.create) to set the right storage folder */
		// set polling to 0,0, set to required polling later when config is done
		iface.createInstance(config.getName(), config.getBackend(),
				0, 0, config.getPlugins(), config.getFactory());
		
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
					LOGGER.error("Metamodel {} is not registered!", params.getMetamodelUri());
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
					LOGGER.error("Metamodel {} is not registered!", params.getMetamodelUri());
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

	private void addMetamodels(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		for (MetamodelParameters params : config.getMetamodels()) {
			if(params.getLocation() != null && !params.getLocation().isEmpty()) {
				File file = new File(params.getLocation());
				hawkInstance.registerMeta(file);
			}
		}
	}

	private void addMissingPlugins(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		final List<String> availablePlugins = new ArrayList<>();
		for (IHawkPlugin hp : manager.getAvailablePlugins()) {
			switch (hp.getCategory()) {
			case GRAPH_CHANGE_LISTENER:
			case METAMODEL_RESOURCE_FACTORY:
			case MODEL_RESOURCE_FACTORY:
			case MODEL_UPDATER:
				availablePlugins.add(hp.getType());
				break;
			default:
				break;
			}
		}

		final List<String> existingPlugins = hawkInstance.getEnabledPlugins();
		final List<String> missingPlugins = new ArrayList<String>();
		for (String plugin : config.getPlugins()) {
			if(availablePlugins.contains(plugin)) {
				if (!existingPlugins.contains(plugin)) {
					missingPlugins.add(plugin);
				}
			} else {
				LOGGER.error("Plugin {} is not available!", plugin);
			}
		}

		if (!missingPlugins.isEmpty()) {
			hawkInstance.addPlugins(missingPlugins);
			manager.saveHawkToMetadata(hawkInstance, true);
		}
	}
}
