/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - remove dependency on eclipse.ui
 ******************************************************************************/
package org.hawk.osgiserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IMetaModelIntrospector;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.util.HawkConfig;
import org.hawk.core.util.HawksConfig;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(HManager.class);

	public static final String BACKEND_CLASS_ATTRIBUTE = "store";
	public static final String MUPDATER_CLASS_ATTRIBUTE = "ModelUpdater";
	public static final String MPARSER_CLASS_ATTRIBUTE = "ModelParser";
	public static final String MMPARSER_CLASS_ATTRIBUTE = "MetaModelParser";
	public static final String VCSMANAGER_CLASS_ATTRIBUTE = "VCSManager";
	public static final String HAWKFACTORY_CLASS_ATTRIBUTE = "class";
	public static final String QUERYLANG_CLASS_ATTRIBUTE = "query_language";
	public static final String GCHANGEL_CLASS_ATTRIBUTE = "class";
	public static final String INTROSPECTOR_CLASS_ATTRIBUTE = "introspector";

	private static HManager inst;

	protected void stateChanged(HModel m) {
		// implementations can use this to update UI or other elements (logs
		// etc)
	}

	protected void infoChanged(HModel m) {
		// implementations can use this to update UI or other elements (logs
		// etc)
	}

	public static HManager getInstance() {
		if (inst == null)
			inst = new HManager();
		return inst;
	}

	public static IEclipsePreferences getPreferences() {
		final String bundleName = FrameworkUtil.getBundle(HManager.class)
				.getSymbolicName();
		return InstanceScope.INSTANCE.getNode(bundleName);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> createExecutableExtensions(final String propertyName,
			final List<IConfigurationElement> elements) {
		final List<T> exts = new ArrayList<>();
		for (IConfigurationElement i : elements) {
			try {
				exts.add((T) i.createExecutableExtension(propertyName));
			} catch (CoreException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return exts;
	}

	private static Set<String> getAttributeFor(final String attributeName,
			final List<IConfigurationElement> elems) {
		Set<String> indexes = new HashSet<String>();
		for (IConfigurationElement i : elems) {
			indexes.add(i.getAttribute(attributeName));
		}
		return indexes;
	}

	private static List<IConfigurationElement> getConfigurationElementsFor(
			final String extensionPointId) {
		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {
			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(extensionPointId);

			els.addAll(Arrays.asList(e));
		}
		return els;
	}

	protected Set<HModel> all = new HashSet<HModel>();

	protected boolean firstRun = true;

	public HManager() {
		try {
			getBackends();
			createExecutableExtensions(VCSMANAGER_CLASS_ATTRIBUTE, getVCS());
			createExecutableExtensions(MMPARSER_CLASS_ATTRIBUTE, getMmps());
			createExecutableExtensions(MPARSER_CLASS_ATTRIBUTE, getMps());
			createExecutableExtensions(MUPDATER_CLASS_ATTRIBUTE, getUps());
			createExecutableExtensions(HAWKFACTORY_CLASS_ATTRIBUTE, getHawkFactories());
			createExecutableExtensions(INTROSPECTOR_CLASS_ATTRIBUTE, getIntrospectors());
			getLanguages();
		} catch (Exception e) {
			HModel.getConsole().printerrln(e);
		}
	}

	public boolean addHawk(HModel e) {
		if (e != null) {
			all.add(e);
			return true;
		} else
			return false;
	}

	public IGraphDatabase createGraph(IHawk hawk) throws Exception {
		for (IConfigurationElement i : getBackends()) {
			if (i.getAttribute(BACKEND_CLASS_ATTRIBUTE).equals(hawk.getDatabaseType())) {
				return (IGraphDatabase) i.createExecutableExtension(BACKEND_CLASS_ATTRIBUTE);
			}
		}
		throw new Exception("cannot instantiate this type of graph: "
				+ hawk.getDatabaseType());
	}

	/**
	 * Creates a new instance of the specified VCSManager.
	 * 
	 * @throws CoreException
	 *             There was an exception while creating the VCSManager.
	 * @throws NoSuchElementException
	 *             No VCSManager with that name exists.
	 */
	public IVcsManager createVCSManager(String s) throws CoreException {
		for (IConfigurationElement i : getVCS()) {
			if (i.getAttribute(VCSMANAGER_CLASS_ATTRIBUTE).equals(s)) {
				return (IVcsManager) i
						.createExecutableExtension(VCSMANAGER_CLASS_ATTRIBUTE);
			}
		}
		throw new NoSuchElementException(
				"cannot instantiate this type of manager: " + s);
	}

	public void delete(HModel o, boolean exists) throws BackingStoreException {
		if (all.contains(o)) {
			if (exists) {
				o.delete();
			} else {
				o.removeHawkFromMetadata(o.getHawkConfig());
			}
			all.remove(o);
		} else {
			o.removeHawkFromMetadata(o.getHawkConfig());
		}
	}

	public List<IConfigurationElement> getBackends() {
		return getConfigurationElementsFor("org.hawk.core.BackEndExtensionPoint");
	}

	public Object[] getElements(Object parent) {
		if (firstRun)
			loadHawksFromMetadata();
		return all.toArray();
	}

	public IGraphDatabase getGraphByIndexerName(String indexerName) {
		for (HModel hm : all)
			if (hm.getName().equals(indexerName))
				return hm.getGraph();
		return null;
	}

	public Set<HModel> getHawks() {
		if (firstRun)
			loadHawksFromMetadata();
		return all;
	}

	/**
	 * Returns the {@link HModel} instance with the specified name among the
	 * existing Hawk instances, or <code>null</code> if no such instance exists.
	 */
	public HModel getHawkByName(String name) {
		for (HModel m : getHawks()) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public Collection<String> getIndexerNames() {
		ArrayList<String> name = new ArrayList<String>();
		for (HModel hm : all)
			if (hm.isRunning())
				name.add(hm.getName());
		return name;
	}

	public Collection<String> getLocalIndexerNames() {
		ArrayList<String> name = new ArrayList<String>();
		for (HModel hm : all)
			if (hm.isLocal() && hm.isRunning())
				name.add(hm.getName());
		return name;
	}

	public Set<String> getIndexTypes() {
		return getAttributeFor(BACKEND_CLASS_ATTRIBUTE, getBackends());
	}

	public List<IConfigurationElement> getLanguages() {
		return getConfigurationElementsFor("org.hawk.core.QueryExtensionPoint");
	}

	public Set<String> getLanguageTypes() {
		return getAttributeFor(QUERYLANG_CLASS_ATTRIBUTE, getLanguages());
	}

	public IMetaModelUpdater getMetaModelUpdater() throws CoreException {

		IConfigurationElement[] e = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						"org.hawk.core.MetaModelUpdaterExtensionPoint");

		IConfigurationElement i = null;
		for (IConfigurationElement ii : e) {
			if (i == null) {
				i = ii;
			} else {
				HModel.getConsole().printerrln("more than one metamodel updater found, only one allowed");
			}
		}

		if (i != null)
			return (IMetaModelUpdater) i
					.createExecutableExtension("metamodelupdater");
		else
			return null;

	}

	public Set<String> getMetaModelTypes() {
		return getAttributeFor(MMPARSER_CLASS_ATTRIBUTE, getMmps());
	}

	public List<IConfigurationElement> getMmps() {
		return getConfigurationElementsFor("org.hawk.core.MetaModelExtensionPoint");
	}

	public Set<String> getModelTypes() {
		return getAttributeFor(MPARSER_CLASS_ATTRIBUTE, getMps());
	}

	public List<IConfigurationElement> getMps() {
		return getConfigurationElementsFor("org.hawk.core.ModelExtensionPoint");
	}

	public Set<String> getUpdaterTypes() {
		return getAttributeFor(MUPDATER_CLASS_ATTRIBUTE, getUps());
	}

	public List<IConfigurationElement> getUps() {
		return getConfigurationElementsFor("org.hawk.core.ModelUpdaterExtensionPoint");
	}

	public List<IConfigurationElement> getVCS() {
		return getConfigurationElementsFor("org.hawk.core.VCSExtensionPoint");
	}

	public Set<String> getVCSTypes() {
		return getAttributeFor(VCSMANAGER_CLASS_ATTRIBUTE, getVCS());
	}

	/**
	 * Returns a list with all the plugins available to Hawk.
	 */
	public List<IHawkPlugin> getAvailablePlugins() {
		List<IHawkPlugin> all = new ArrayList<>();
		
		List<IHawkPlugin> backends = createExecutableExtensions(BACKEND_CLASS_ATTRIBUTE, getBackends());
		all.addAll(backends);

		List<IHawkPlugin> gcl = createExecutableExtensions(GCHANGEL_CLASS_ATTRIBUTE, getGraphChangeListeners());
		all.addAll(gcl);

		List<IHawkPlugin> factories = createExecutableExtensions(HAWKFACTORY_CLASS_ATTRIBUTE, getHawkFactories());
		all.addAll(factories);

		List<IHawkPlugin> mmParsers = createExecutableExtensions(MMPARSER_CLASS_ATTRIBUTE, getMmps());
		all.addAll(mmParsers);

		List<IHawkPlugin> mParsers = createExecutableExtensions(MPARSER_CLASS_ATTRIBUTE, getMps());
		all.addAll(mParsers);

		List<IHawkPlugin> mUpdaters = createExecutableExtensions(MUPDATER_CLASS_ATTRIBUTE, getUps());
		all.addAll(mUpdaters);

		List<IHawkPlugin> queryEngines = createExecutableExtensions(QUERYLANG_CLASS_ATTRIBUTE, getLanguages());
		all.addAll(queryEngines);

		all.addAll(getVCSInstances());

		return all;
	}

	public List<IVcsManager> getVCSInstances() {
		return createExecutableExtensions(VCSMANAGER_CLASS_ATTRIBUTE, getVCS());
	}

	public List<IConfigurationElement> getHawkFactories() {
		return getConfigurationElementsFor("org.hawk.core.HawkFactoryExtensionPoint");
	}

	public List<IConfigurationElement> getGraphChangeListeners() {
		return getConfigurationElementsFor("org.hawk.core.GraphChangeListenerExtensionPoint");
	}

	public Set<String> getGraphChangeListenerTypes() {
		return getAttributeFor(GCHANGEL_CLASS_ATTRIBUTE, getGraphChangeListeners());
	}

	public Map<String, IHawkFactory> getHawkFactoryInstances() {
		final Map<String, IHawkFactory> ids = new HashMap<>();
		for (IConfigurationElement elem : getHawkFactories()) {
			try {
				ids.put(elem.getAttribute(HAWKFACTORY_CLASS_ATTRIBUTE),
						(IHawkFactory) elem
								.createExecutableExtension(HAWKFACTORY_CLASS_ATTRIBUTE));
			} catch (InvalidRegistryObjectException | CoreException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return ids;
	}

	public IHawkFactory createHawkFactory(String factoryClass)
			throws CoreException {
		for (IConfigurationElement elem : getHawkFactories()) {
			if (factoryClass.equals(elem
					.getAttribute(HAWKFACTORY_CLASS_ATTRIBUTE))) {
				return (IHawkFactory) elem
						.createExecutableExtension(HAWKFACTORY_CLASS_ATTRIBUTE);
			}
		}
		return null;
	}

	public List<IConfigurationElement> getIntrospectors() {
		return getConfigurationElementsFor("org.hawk.core.IndexerIntrospectionExtensionPoint");
	}

	public IMetaModelIntrospector getIntrospectorFor(IModelIndexer idx) {
		for (IConfigurationElement conf : getIntrospectors()) {
			try {
				IMetaModelIntrospector.Factory impl = (IMetaModelIntrospector.Factory)
					conf.createExecutableExtension(INTROSPECTOR_CLASS_ATTRIBUTE);
				if (impl.canIntrospect(idx)) {
					return impl.createFor(idx);
				}
			} catch (CoreException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	public boolean stopAllRunningInstances(ShutdownRequestType reqType) {
		HModel.getConsole().println("Shutting down hawk:");
		for (HModel hm : all) {
			if (hm.isRunning()) {
				HModel.getConsole().println(String.format("Stopping %s (%s)", hm.getName(), hm.getFolder()));
				hm.stop(reqType);
			}
		}
		return true;
	}

	protected void loadHawksFromMetadata() {
		IEclipsePreferences preferences = getPreferences();
		String xml = preferences.get("config", null);

		boolean success = true;
		if (xml != null) {
			try {
				XStream stream = new XStream(new DomDriver());
				stream.processAnnotations(HawksConfig.class);
				stream.processAnnotations(HawkConfig.class);
				stream.setClassLoader(HawksConfig.class.getClassLoader());

				HawksConfig hc = (HawksConfig) stream.fromXML(xml);
				Set<HawkConfig> hawks = new HashSet<>();
				for (HawkConfig s : hc.getConfigs()) {
					/*
					 * The storage folder may have been deleted since then: check if it still
					 * exists.
					 */
					if (new File(s.getStorageFolder()).exists()) {
						hawks.add(s);
					}
				}

				for (HawkConfig s : hawks) {
					success = success && addHawk(HModel.load(s, this));
				}

			} catch (Exception e) {
				LOGGER.error("Failed to load configuration: started with "
						+ xml.substring(0, Math.min(xml.length() - 1, 20)), e);
				success = false;
			}
		}

		if (!success) {
			preferences.remove("config");
			try {
				preferences.flush();
			} catch (BackingStoreException e) {
				LOGGER.error("Failed to flush preferences", e);
			}
		}

		firstRun = false;
	}

	public ICredentialsStore getCredentialsStore() {
		// TODO provide extension point?
		return new SecurePreferencesCredentialsStore();
	}

	public void saveHawkToMetadata(HModel e) throws BackingStoreException {
		saveHawkToMetadata(e, false);
	}
	
	public void saveHawkToMetadata(HModel e, boolean replace) throws BackingStoreException {
		final IEclipsePreferences preferences = getPreferences();
		final String oldXML = preferences.get("config", null);

		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawksConfig.class);
		stream.processAnnotations(HawkConfig.class);
		stream.setClassLoader(HawksConfig.class.getClassLoader());

		HawksConfig hc = null;
		try {
			if (oldXML != null) {
				hc = (HawksConfig) stream.fromXML(oldXML);
			}

			Set<HawkConfig> locs = new HashSet<HawkConfig>();
			if (hc != null) {
				locs.addAll(hc.getConfigs());
			}

			if(replace && locs.contains(e.getHawkConfig())) {
				locs.remove(e.getHawkConfig());
			}
			
			locs.add(e.getHawkConfig());
			final String xml = stream.toXML(new HawksConfig(locs));
			preferences.put("config", xml);
		} catch (Exception ex) {
			preferences.put("config", oldXML);
		}
		preferences.flush();
	}

}
