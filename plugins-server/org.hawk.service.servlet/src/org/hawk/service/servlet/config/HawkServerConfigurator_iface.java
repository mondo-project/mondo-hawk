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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.hawk.core.IVcsManager;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.Credentials;
import org.hawk.service.api.DerivedAttributeSpec;
import org.hawk.service.api.FailedQuery;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.api.HawkInstance;
import org.hawk.service.api.HawkInstanceNotFound;
import org.hawk.service.api.HawkInstanceNotRunning;
import org.hawk.service.api.HawkQueryOptions;
import org.hawk.service.api.InvalidDerivedAttributeSpec;
import org.hawk.service.api.InvalidIndexedAttributeSpec;
import org.hawk.service.api.InvalidMetamodel;
import org.hawk.service.api.InvalidPollingConfiguration;
import org.hawk.service.api.InvalidQuery;
import org.hawk.service.api.ModelElement;
import org.hawk.service.api.QueryResult;
import org.hawk.service.api.Repository;
import org.hawk.service.api.Subscription;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.UnknownQueryLanguage;
import org.hawk.service.api.UnknownRepositoryType;
import org.hawk.service.api.VCSAuthenticationFailed;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.IndexedAttributeSpec;
import org.hawk.service.servlet.config.ConfigFileParser;
import org.hawk.service.servlet.config.HawkInstanceConfig;
import org.hawk.service.servlet.config.RepositoryParameters;

public class HawkServerConfigurator_iface {
	List<HawkInstanceConfig> hawkInstanceConfigs;
	Iface iface;

	HManager manager;
	ConfigFileParser parser;

	public HawkServerConfigurator_iface(Iface iface) {
		this.iface = iface;

		hawkInstanceConfigs = new ArrayList<HawkInstanceConfig>();
		manager = HManager.getInstance();
		parser = new ConfigFileParser();
	}

	public void loadHawkServerConfigurations() {
		for (File file : getHawkServerConfigurationFiles()) {
			System.out.println("configuring hawk instances:");
			System.out.println("prasing file: " + file.getName());
			HawkInstanceConfig config = parser.parse(file);
			if (config != null) {
				System.out.println("configuring hawk instance: " + config.getName());
				hawkInstanceConfigs.add(config); // add to list
				configureHawkInstance(config);
			}
		}

		// save to configuration file (some new attributes and metamodels might
		// be added to instance)
		//saveHawkServerConfigurations();
	}

	public void saveHawkServerConfigurations() {
		for (HawkInstanceConfig config : hawkInstanceConfigs) {
			System.out.println("saving config for hawk instance: " + config.getName());

			saveHawkInstanceConfig(config);
		}
	}

	private List<File> getHawkServerConfigurationFiles() {
		try {
			File configurationFolder = null;
			Location configurationLocation = Platform.getConfigurationLocation();
			if (configurationLocation == null) {
				// create a configuration folder
				configurationFolder = new File("configuration");
			} else {
				URL configurationURL = configurationLocation.getURL();
				String path = FileLocator.toFileURL(configurationURL).getPath();
				configurationFolder = new File(path);
			}
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
				hawkInstance = createHawkInstance(config); // create new
															// instance
			}

			/** apply configuration */
			// TODO set db, need to check if changed
			// hawkInstance.setDbType(config.getBackend());

			//addMissingPlugins(hawkInstance, config);
			iface.startInstance(config.getName());

			while (!hawkInstance.isRunning());
			//hawkInstance.configurePolling(0, 0);
			iface.configurePolling(config.getName(), 0, 0);

			// start instance
			//hawkInstance.start(manager);

			

			// add metamodels, Do it first before adding attributes or
			// repositories
			addMetamodels(hawkInstance, config);

			// add repositories, don't delete any
			addMissingRepositories(hawkInstance, config);

			// derived Attributes
			addMissingDerivedAttributes(hawkInstance, config);

			// indexed Attributes
			addMissingIndexedAttributes(hawkInstance, config);
			
			//hawkInstance.configurePolling(config.getDelayMin(), config.getDelayMax());
			iface.configurePolling(config.getName(), config.getDelayMin(), config.getDelayMax());


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HModel createHawkInstance(HawkInstanceConfig config) throws TException {

		/**
		 * use iface.createInstance (instead of HModel.create) to set the right
		 * storage folder
		 */
		iface.createInstance(config.getName(), config.getBackend()/*, config.getDelayMin(), config.getDelayMax(),*/,0,0, config.getPlugins());

		HModel hawkInstance = manager.getHawkByName(config.getName());

		return hawkInstance;
	}

	private void addMissingDerivedAttributes(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
	//	Collection<IndexedAttributeParameters> existingDerivedAttributes = hawkInstance.getDerivedAttributes();
		Collection<DerivedAttributeSpec> existingDerivedAttributes = iface.listDerivedAttributes(config.getName());
		//List<String> metamodels = hawkInstance.getRegisteredMetamodels();
		List<String> metamodels = iface.listMetamodels(config.getName());

		/*for (DerivedAttributeParameters params : config.getDerivedAttributes()) {
			if (!existingDerivedAttributes.contains(params)) {
				if (metamodels.contains(params.getMetamodelUri())) {
					hawkInstance.addDerivedAttribute(params.getMetamodelUri(), params.getTypeName(), params.getAttributeName(), params.getAttributeType(), params.isMany(), params.isOrdered(), params.isUnique(), params.getDerivationLanguage(), params.getDerivationLogic());
				} else {
					System.err.println("HawkServerConfigurator.addMissingDerivedAttributes: metamodel " + params.getMetamodelUri() + " is not registered!");
				}
			}
		}*/
	}

	private void addMissingIndexedAttributes(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		List<String> metamodels = hawkInstance.getRegisteredMetamodels();
		//Collection<IndexedAttributeParameters> existingIndexedAttributes = hawkInstance.getIndexedAttributes();
		Collection<IndexedAttributeSpec> existingIndexedAttributes = iface.listIndexedAttributes(config.getName());

		/*for (IndexedAttributeParameters params : config.getIndexedAttributes()) {
			if (!existingIndexedAttributes.contains(params)) {
				if (metamodels.contains(params.getMetamodelUri())) {
					hawkInstance.addIndexedAttribute(params.getMetamodelUri(), params.getTypeName(), params.getAttributeName());
				} else {
					System.err.println("HawkServerConfigurator.addMissingIndexedAttributes: metamodel " + params.getMetamodelUri() + " is not registered!");
				}
			}
		}*/
	}

	private void addMissingRepositories(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		//Collection<String> existingLocations = hawkInstance.getLocations();
		Collection<Repository> existingLocations = iface.listRepositories(config.getName());
		
		
		
		for (RepositoryParameters params : config.getRepositories()) {
			// // reformat repository string to URI
			// IVcsManager mo = manager.createVCSManager(params.getType());
			// if (mo.isPathLocationAccepted()) {
			// params.setLocation(new
			// File(params.getLocation()).toURI().toString());
			// }

			//if (!existingLocations.contains(params.getLocation())) {
				//hawkInstance.addVCS(params.getLocation(), params.getType(), params.getUser(), params.getPass(), params.isFrozen());
//			Boolean exists = false;
//			for(Repository repo : existingLocations) {
//				if(params.getLocation().equals(repo.uri)) {
//					exists = true;
//				}
//			}
//			
			Credentials creds = new Credentials();
			creds.username = params.getUser();
			creds.password = params.getPass();
			if (creds.username == null) { creds.username = "anonymous"; }
			if (creds.password == null) { creds.password = "anonymous"; }

			// TODO tell Kostas that LocalFolder does not work if the path has a trailing separator
			Repository repo = new Repository(params.getLocation(), params.getType());
			repo.setIsFrozen(params.isFrozen());
			
			
			iface.addRepository(config.getName(), repo, creds);
			//}
		}
	}

	private void addMetamodels(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		for (MetamodelParameters params : config.getMetamodels()) {
			if (params.getLocation() != null && !params.getLocation().isEmpty()) {
				File file = new File(params.getLocation());
				//hawkInstance.registerMeta(file);
				List<org.hawk.service.api.File> files = new ArrayList<>();
				files.add(APIUtils.convertJavaFileToThriftFile(file));
				iface.registerMetamodels(config.getName(), files);
			}
		}
	}

	private void addMissingPlugins(HModel hawkInstance, HawkInstanceConfig config) throws Exception {
		List<String> availableplugins = manager.getAvailablePlugins();
		List<String> existingplugins = hawkInstance.getEnabledPlugins();
		List<String> missingPlugins = new ArrayList<String>();

		for (String plugin : config.getPlugins()) {
			if (availableplugins.contains(plugin)) {
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
		HModel hawkInstance = manager.getHawkByName(config.getName());

		/**
		 * delay, backend and plugins don't change during running of an
		 * instance, set it anyway
		 */
		config.setBackend(hawkInstance.getDbType());

		/** Only successfully-added plugins are saved */
		if (hawkInstance.getEnabledPlugins() != null) {
			config.setPlugins(hawkInstance.getEnabledPlugins());
		}

		/**
		 * The following Elements are saved (the ones originally in the
		 * file(even if was unsuccessful), and the ones added during hawk
		 * instance running: - Metamodels - Derived Attributes - Indexed
		 * Attributes - Repositories
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
			for (MetamodelParameters params : config.getMetamodels()) {
				if (params.getUri().equals(metamodelUri)) {
					isNew = false;
					break;
				}
			}

			if (isNew) {
				newMetamodels.add(new MetamodelParameters(metamodelUri, null));
			}
		}

		if (!newMetamodels.isEmpty()) {
			config.getMetamodels().addAll(newMetamodels);
		}
	}

	private void addNewDerivedAttributesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		Collection<IndexedAttributeParameters> instanceAttributes = hawkInstance.getDerivedAttributes();
		//Collection<DerivedAttributeSpec> instanceAttributes = iface.listDerivedAttributes(config.getName());
		List<DerivedAttributeParameters> configAttributes = config.getDerivedAttributes();
		List<DerivedAttributeParameters> newAttrs = new ArrayList<DerivedAttributeParameters>();

		for (IndexedAttributeParameters indexedAttribute : instanceAttributes) {
			if (indexedAttribute instanceof DerivedAttributeParameters) {
				boolean isNew = true;
				for (DerivedAttributeParameters params : configAttributes) {
					if (indexedAttribute.equals(params)) {
						isNew = false;
						break;
					}
				}

				if (isNew && indexedAttribute != null) {
					newAttrs.add((DerivedAttributeParameters) indexedAttribute);
				}
			}
		}

		if (!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}
	}

	private void addNewIndexedAttributesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		Collection<IndexedAttributeParameters> instanceAttributes = hawkInstance.getIndexedAttributes();
		List<IndexedAttributeParameters> configAttributes = config.getIndexedAttributes();
		List<IndexedAttributeParameters> newAttrs = new ArrayList<IndexedAttributeParameters>();

		for (IndexedAttributeParameters indexedAttribute : instanceAttributes) {
			boolean isNew = true;
			for (IndexedAttributeParameters params : configAttributes) {
				if (indexedAttribute.equals(params)) {
					isNew = false;
					break;
				}
			}

			if (isNew && indexedAttribute != null) {
				newAttrs.add(indexedAttribute);
			}
		}

		if (!newAttrs.isEmpty()) {
			configAttributes.addAll(newAttrs);
		}

	}

	private void addNewRepositoriesToConfig(HModel hawkInstance, HawkInstanceConfig config) {

		List<RepositoryParameters> newRepos = new ArrayList<RepositoryParameters>();

		for (IVcsManager vcsManager : hawkInstance.getRunningVCSManagers()) {
			boolean isNew = true;
			for (RepositoryParameters repos : config.getRepositories()) {
				if (repos.getType().equals(vcsManager.getType()) && repos.getLocation().equals(vcsManager.getLocation())) {

					repos.setFrozen(vcsManager.isFrozen());
					repos.setUser(vcsManager.getUsername());
					repos.setPass(vcsManager.getPassword());
					isNew = false;
					break;
				}
			}

			if (isNew) {
				RepositoryParameters newRepo = new RepositoryParameters(vcsManager.getType(), vcsManager.getLocation(), vcsManager.getUsername(), vcsManager.getPassword(), vcsManager.isFrozen());
				newRepos.add(newRepo);
			}
		}

		if (!newRepos.isEmpty()) {
			config.getRepositories().addAll(newRepos);
		}
	}

}
