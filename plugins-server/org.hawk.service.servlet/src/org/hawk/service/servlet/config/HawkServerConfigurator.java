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
import java.util.TimerTask;

import org.apache.thrift.TException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.servlet.config.ConfigFileParser;
import org.hawk.service.servlet.config.HawkInstanceConfig;
import org.hawk.service.servlet.config.RepositoryParameters;

public class HawkServerConfigurator  {

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
			System.out.println("configuring hawk instances:");
			System.out.println("prasing file: " + file.getName());
			HawkInstanceConfig config = parser.parse(file);
			if(config != null) {
				System.out.println("configuring Hawk instance: " + config.getName());
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

			System.out.println("Looking for configuration files in " + configurationFolder.getAbsolutePath());
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
			hawkInstance.getHawk().getModelIndexer().scheduleTask(new TimerTask(){
				@Override
				public void run() {
					// add metamodels, Do it first before adding attributes or repositories
					addMetamodels(hModel, config);

					// add repositories, don't delete any
					addMissingRepositories(hModel, config);

					try {
						// derived Attributes
						addMissingDerivedAttributes(hModel, config);

						// indexed Attributes
						addMissingIndexedAttributes(hModel, config);
					} catch (Exception e) {
						System.out.println("Configuring Hawk instance: Exception when adding derived/indexed attributes");
						e.printStackTrace();
					}
					
					// set the required polling values
					hModel.configurePolling(config.getDelayMin(), config.getDelayMax());
	
					numberOfConfiguredInstances++;
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
				0, 0,
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
}
