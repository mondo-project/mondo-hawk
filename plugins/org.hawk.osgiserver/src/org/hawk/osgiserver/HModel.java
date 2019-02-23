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
 *     Antonio Garcia-Dominguez - use explicit HManager instances, add support for
 *                                remote locations, use lambdas to simplify code
 ******************************************************************************/
package org.hawk.osgiserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IMetaModelIntrospector;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IStateListener;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.util.HawkConfig;
import org.hawk.core.util.HawkProperties;
import org.hawk.core.util.HawksConfig;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.core.util.SLF4JConsole;
import org.osgi.service.prefs.BackingStoreException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HModel implements IStateListener {
	public static String DEFAULT_INFO = "Sleeping...";

	private String info = DEFAULT_INFO;
	private HawkState status;

	protected void setStatus(HawkState status) {
		if (this.status != status) {
			this.status = status;
			manager.stateChanged(this);
		}
	}

	public HawkState getStatus() {
		return status;
	}

	public void setInfo(String info) {
		if (!this.info.equals(info)) {
			this.info = info;
			manager.infoChanged(this);
		}
	}

	public String getInfo() {
		return info;
	}

	private static IConsole CONSOLE = new SLF4JConsole();

	public static IConsole getConsole() {
		if (CONSOLE == null) {
			CONSOLE = new SLF4JConsole();
		}
		return CONSOLE;
	}

	public static void setConsole(IConsole c) {
		CONSOLE = c;
	}

	/**
	 * Creates a new Hawk instance in a local folder, and saves its metadata into
	 * the {@link HManager}.
	 *
	 * @param hawkFactory   Factory which should create the indexer.
	 * @param name          Name of the Hawk index.
	 * @param storageFolder Folder to use to store preferences and embedded DB (if
	 *                      any).
	 * @param location      URL of the remote Hawk server (if using a remote
	 *                      instance).
	 * @param dbType        Value of {@link IGraphDatabase#getType()} which
	 *                      identifies the backend to use.
	 * @param plugins       Values of {@link IHawkPlugin#getType()} which identify
	 *                      the plugins to use.
	 * @param manager       Manager associated with this Hawk index.
	 * @param credStore     Credentials store (for storing remote VCS
	 *                      usernames/passwords).
	 * @param minDelay      Minimum delay between VCS checks in milliseconds (0
	 *                      disables this behaviour, requiring manual syncing).
	 * @param maxDelay      Maximum delay between VCS checks in milliseconds (0
	 *                      disables this behaviour, requiring manual syncing).
	 *
	 * @throws IllegalArgumentException The minimum and maximum delays are not valid
	 *                                  or not consistent with each other.
	 */
	public static HModel create(IHawkFactory hawkFactory, String name, File storageFolder, String location,
			String dbType, List<String> plugins, HManager manager, ICredentialsStore credStore, int minDelay,
			int maxDelay) throws Exception {

		if (minDelay > maxDelay) {
			throw new IllegalArgumentException("minimum delay must be less than or equal to maximum delay");
		} else if (minDelay < 0) {
			throw new IllegalArgumentException("minimum delay must not be negative");
		} else if (maxDelay < 0) {
			throw new IllegalArgumentException("maximum delay must not be negative");
		}

		HModel hm = new HModel(manager, hawkFactory, name, storageFolder, location, credStore, plugins);
		if (dbType != null) {
			hm.hawk.setDatabaseType(dbType);
		}
		if (hm.getModelUpdaters() != null && hm.getModelUpdaters().isEmpty()) {
			throw new IllegalArgumentException("Must add at least one updater");
		}

		// TODO use plugins list to enable only these plugins
		IGraphDatabase db = null;
		final IConsole console = getConsole();
		try {
			// create the indexer with relevant database
			console.println("Creating Hawk indexer...");

			if (hawkFactory.instancesCreateGraph()) {
				console.println("Setting up hawk's back-end store:");
				db = manager.createBackendGraph(hm.hawk);
				db.run(storageFolder, console);
				hm.hawk.getModelIndexer().setDB(db, true);
			}

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = manager.getMetaModelUpdater();
			console.println("Setting up hawk's metamodel updater:\n" + metaModelUpdater.getType());
			hm.hawk.getModelIndexer().setMetaModelUpdater(metaModelUpdater);
			hm.hawk.getModelIndexer().init(minDelay, maxDelay);

			manager.addHawk(hm);
			manager.saveHawkToMetadata(hm);
			console.println("Created Hawk indexer!");
			return hm;
		} catch (Exception e) {
			console.printerrln("Adding of indexer aborted, please try again.\n"
					+ "Shutting down and removing back-end (if it was created)");
			console.printerrln(e);

			try {
				if (db != null) {
					db.delete();
				}
			} catch (Exception e2) {
				throw e2;
			}

			console.printerrln("aborting finished.");
			throw e;
		}

	}

	/**
	 * Loads a previously existing Hawk instance from its {@link HawkConfig}.
	 */
	public static HModel load(HawkConfig config, HManager manager) throws Exception {

		try {
			final IHawkFactory hawkFactory = manager.createHawkFactory(config.getHawkFactory());
			final HModel hm = new HModel(manager, hawkFactory, config.getName(), new File(config.getStorageFolder()),
					config.getLocation(), manager.getCredentialsStore(), config.getEnabledPlugins());

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = manager.getMetaModelUpdater();
			hm.hawk.getModelIndexer().setMetaModelUpdater(metaModelUpdater);
			return hm;
		} catch (Throwable e) {
			getConsole().printerrln("Exception in trying to add create Indexer from folder:");
			getConsole().printerrln(e);
			getConsole().printerrln("Adding of indexer aborted, please try again");
			return null;
		}
	}

	public boolean isLocal() {
		return hawk.getModelIndexer().getGraph() != null;
	}

	public IHawk getHawk() {
		return hawk;
	}
	
	public String getDbType() {
		return this.hawk.getDatabaseType();
	}

	public void setDbType(String dbType) throws Exception {
		hawk.setDatabaseType(dbType);
		if (hawkFactory.instancesCreateGraph()) {
			IGraphDatabase db = manager.createBackendGraph(this.hawk);
			db.run(new File(this.getHawkConfig().getStorageFolder()), getConsole());
			this.hawk.getModelIndexer().setDB(db, true);
		}
	}

	/**
	 * Either <code>null</code> (all plugins are implicitly enabled) or a collection of plugins (as reported by HManager
	 */
	private List<String> enabledPlugins;
	private final IHawk hawk;
	private final IHawkFactory hawkFactory;
	private final HManager manager;
	private final String hawkLocation;

	/**
	 * Constructor for loading existing local Hawk instances and
	 * creating/loading custom {@link IHawk} implementations.
	 * @param plugins 
	 * @param plugins 
	 */
	public HModel(HManager manager, IHawkFactory hawkFactory, String name, File storageFolder, String location, ICredentialsStore credStore, List<String> plugins) throws Exception {
		this.hawkFactory = hawkFactory;
		this.hawk = hawkFactory.create(name, storageFolder, location, credStore, getConsole(), plugins);
		this.manager = manager;
		this.hawkLocation = location;
		this.enabledPlugins = plugins;

		enablePlugins();
		hawk.getModelIndexer().addStateListener(this);
	}

	public void addPlugins(List<String> plugins) throws Exception {
		if(this.enabledPlugins == null) {
			this.enabledPlugins = plugins;
		} else if (!enabledPlugins.isEmpty()){
			this.enabledPlugins.addAll(plugins);
		}
		enablePlugins();
	}
	
	public void removePlugins(List<String> plugins) throws Exception {
		if(this.enabledPlugins != null && !this.enabledPlugins.isEmpty()) {
			this.enabledPlugins.removeAll(plugins);
			disablePlugins(plugins);
		}
	}

	private void enablePlugins() throws Exception {
		if (!hawkFactory.instancesAreExtensible()) {
			return;
		}
		final IConsole console = getConsole();
		final IModelIndexer indexer = this.hawk.getModelIndexer();

		console.println("adding metamodel resource factories:");
		enablePlugins(manager.getMmps(), HManager.METAMODEL_PARSER_CLASS_ATTRIBUTE,
			IMetaModelResourceFactory.class, indexer::addMetaModelResourceFactory);

		console.println("adding model resource factories:");
		enablePlugins(manager.getMps(), HManager.MODEL_PARSER_CLASS_ATTRIBUTE,
				IModelResourceFactory.class, indexer::addModelResourceFactory);

		console.println("adding query engines:");
		enablePlugins(manager.getLanguages(), HManager.QUERY_LANG_CLASS_ATTRIBUTE,
				IQueryEngine.class, indexer::addQueryEngine);

		console.println("adding model updaters:");
		enablePlugins(manager.getUps(), HManager.MODEL_UPDATER_CLASS_ATTRIBUTE,
				IModelUpdater.class, indexer::addModelUpdater);

		console.println("adding graph change listeners:");
		enablePlugins(manager.getGraphChangeListeners(), HManager.GRAPH_CHANGE_LISTENER_CLASS_ATTRIBUTE,
				IGraphChangeListener.class, indexer::addGraphChangeListener);
	}

	private <T extends IHawkPlugin> void enablePlugins(final List<IConfigurationElement> elems, final String attribute, final Class<T> klass, final Consumer<T> addMethod) throws CoreException {
		final IConsole console = getConsole();

		for (IConfigurationElement elem : elems) {
			@SuppressWarnings("unchecked")
			final T f = (T) elem.createExecutableExtension(attribute);

			if (enabledPlugins == null || enabledPlugins.contains(f.getType())) {
				addMethod.accept(f);
				console.println(f.getHumanReadableName());
			}
		}
	}

	private void disablePlugins(List<String> plugins) throws Exception {
		if (hawkFactory.instancesAreExtensible() && plugins != null) {
			final IConsole console = getConsole();
			console.println("removing metamodel resource factories:");

			final IModelIndexer indexer = this.hawk.getModelIndexer();
			disablePlugins(plugins, IMetaModelResourceFactory.class,
				indexer.getMetaModelParsers(), indexer::removeMetaModelResourceFactory);

			console.println("removing model resource factories:");
			disablePlugins(plugins, IModelResourceFactory.class,
				indexer.getModelParsers(), indexer::removeModelResourceFactory);

			console.println("removing model updaters:");
			disablePlugins(plugins, IModelUpdater.class,
				hawk.getModelIndexer().getModelUpdaters(),
				indexer::removeModelUpdater);

			console.println("removing graph change listeners:");
			disablePlugins(plugins, IGraphChangeListener.class,
				indexer.getCompositeGraphChangeListener(),
				hawk.getModelIndexer()::removeGraphChangeListener);
		}
	}

	@FunctionalInterface
	private interface FlakyConsumer <T> {
		void accept(T t) throws Exception;
	}

	private <T extends IHawkPlugin> void disablePlugins(List<String> pluginTypesToRemove, Class<T> klass, Collection<T> plugins, FlakyConsumer<T> remover) throws Exception {
		for (T t : new ArrayList<>(plugins)) {
			if (pluginTypesToRemove.contains(t.getType())) {
				remover.accept(t);
			}
		}
	}

	public void addDerivedAttribute(String metamodeluri, String typename, String attributename, String attributetype,
			Boolean isMany, Boolean isOrdered, Boolean isUnique, String derivationlanguage, String derivationlogic)
					throws Exception {
		hawk.getModelIndexer().addDerivedAttribute(metamodeluri, typename, attributename, attributetype, isMany,
				isOrdered, isUnique, derivationlanguage, derivationlogic);
	}

	private void loadVCS(String loc, String type, boolean isFrozen) throws Exception {
		final IModelIndexer indexer = hawk.getModelIndexer();
		final IVcsManager mo = manager.createVCSManager(type);
		mo.init(loc, indexer);
		if (!this.getLocations().contains(mo.getLocation())) {
			mo.run();
			mo.setFrozen(isFrozen);
			indexer.addVCSManager(mo, false);
		}
	}

	public void addIndexedAttribute(String metamodeluri, String typename, String attributename) throws Exception {
		hawk.getModelIndexer().addIndexedAttribute(metamodeluri, typename, attributename);
	}

	public void addVCS(String loc, String type, String user, String pass, boolean isFrozen) {
		try {
			IVcsManager mo = manager.createVCSManager(type);
			mo.init(loc, hawk.getModelIndexer());

			if (!this.getLocations().contains(mo.getLocation())) {
				mo.setCredentials(user, pass, hawk.getModelIndexer().getCredentialsStore());
				mo.run();
				mo.setFrozen(isFrozen);
				hawk.getModelIndexer().addVCSManager(mo, true);
			}
		} catch (Exception e) {
			getConsole().printerrln(e);
		}
	}

	/**
	 * Registers a new graph change listener into the model indexer, if it
	 * wasn't already registered. Otherwise, it does nothing.
	 */
	public boolean addGraphChangeListener(IGraphChangeListener changeListener) {
		return hawk.getModelIndexer().addGraphChangeListener(changeListener);
	}

	/**
	 * Removes a new graph change listener from the model indexer, if it was
	 * already registered. Otherwise, it does nothing.
	 */
	public boolean removeGraphChangeListener(IGraphChangeListener changeListener) {
		return hawk.getModelIndexer().removeGraphChangeListener(changeListener);
	}

	/**
	 * Performs a query and returns its result. The result must be a Double, a
	 * String, an Integer, a ModelElement, the null reference or an Iterable of
	 * these things.
	 * 
	 * @throws NoSuchElementException
	 *             Unknown query language.
	 */
	public Object query(File query, String ql, Map<String, Object> context) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages().get(ql);
		if (q == null) {
			throw new NoSuchElementException();
		}
		return q.query(hawk.getModelIndexer(), query, context);
	}

	/**
	 * Performs a query and returns its result. For the result types, see
	 * {@link #contextFullQuery(File, String, Map)}.
	 * 
	 * @throws NoSuchElementException
	 *             Unknown query language.
	 */
	public Object query(String query, String ql, Map<String, Object> context) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages().get(ql);
		if (q == null) {
			throw new NoSuchElementException();
		}
		return q.query(hawk.getModelIndexer(), query, context);
	}

	public void delete() throws BackingStoreException {
		removeHawkFromMetadata(getHawkConfig());

		File f = hawk.getModelIndexer().getParentFolder();
		if (this.isRunning()) {
			try {
				hawk.getModelIndexer().shutdown(ShutdownRequestType.ONLY_LOCAL);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (f.exists()) {
			getConsole().println("Hawk instance removed from ui but persistence remains at: " + f);
		}
	}

	/**
	 * Returns a {@link HawkConfig} from which this instance can be reloaded.
	 */
	public HawkConfig getHawkConfig() {
		return new HawkConfig(getName(), getFolder(), hawkLocation, hawkFactory.getClass().getName(), enabledPlugins);
	}

	public boolean exists() {
		return hawk != null && hawk.exists();
	}

	public List<String> getEnabledPlugins() {
		return enabledPlugins;
	}

	public Collection<IMetaModelResourceFactory> getMetamodelParsers() {
		List<IMetaModelResourceFactory> parsers = new ArrayList<>();
		for (String type : hawk.getModelIndexer().getKnownMetaModelParserTypes()) {
			IMetaModelResourceFactory parser = hawk.getModelIndexer().getMetaModelParser(type);
			parsers.add(parser);
		}
		return parsers;
	}

	public Collection<IndexedAttributeParameters> getDerivedAttributes() {
		return hawk.getModelIndexer().getDerivedAttributes();
	}

	public String getFolder() {
		return hawk.getModelIndexer().getParentFolder().toString();
	}

	public IGraphDatabase getGraph() {
		return hawk.getModelIndexer().getGraph();
	}

	public IMetaModelIntrospector getIntrospector() {
		return manager.getIntrospectorFor(hawk.getModelIndexer());
	}

	public Collection<IndexedAttributeParameters> getIndexedAttributes() {
		return hawk.getModelIndexer().getIndexedAttributes();
	}

	public Collection<IModelUpdater> getModelUpdaters() {
		return hawk.getModelIndexer().getModelUpdaters();
	}

	public Collection<String> getIndexes() {
		return hawk.getModelIndexer().getIndexes();
	}

	public Set<String> getKnownQueryLanguages() {
		return hawk.getModelIndexer().getKnownQueryLanguages().keySet();
	}

	public Collection<String> getLocations() {
		List<String> locations = new ArrayList<String>();
		for (IVcsManager o : getRunningVCSManagers()) {
			locations.add(o.getLocation());
		}
		return locations;
	}

	public Collection<IVcsManager> getRunningVCSManagers() {
		return hawk.getModelIndexer().getRunningVCSManagers();
	}

	public String getName() {
		return hawk.getModelIndexer().getName();
	}

	public List<String> getRegisteredMetamodels() {
		return new ArrayList<String>(hawk.getModelIndexer().getKnownMMUris());
	}

	public List<IVcsManager> getVCSInstances() {
		return manager.getVCSInstances();
	}

	public boolean isRunning() {
		return hawk.getModelIndexer().isRunning();
	}

	public HManager getManager() {
		return manager;
	}

	public boolean registerMeta(File... f) {
		try {
			hawk.getModelIndexer().registerMetamodels(f);
		} catch (Exception e) {
			getConsole().printerrln(e);
			return false;
		}
		return true;
	}

	public void removeHawkFromMetadata(HawkConfig config) throws BackingStoreException {
		IEclipsePreferences preferences = HManager.getPreferences();

		String xml = preferences.get("config", null);

		if (xml != null) {
			XStream stream = new XStream(new DomDriver());
			stream.processAnnotations(HawksConfig.class);
			stream.processAnnotations(HawkConfig.class);
			stream.setClassLoader(HawksConfig.class.getClassLoader());
			HawksConfig hc = (HawksConfig) stream.fromXML(xml);
			hc.removeLoc(config);
			xml = stream.toXML(hc);
			preferences.put("config", xml);
			preferences.flush();
		} else {
			getConsole().printerrln("removeHawkFromMetadata tried to load preferences but it could not.");
		}
	}

	public boolean start(HManager manager) {
		try {
			final HawkProperties hp = loadIndexerMetadata();

			if (hawkFactory.instancesCreateGraph()) {
				// create the indexer with relevant database
				IGraphDatabase db = manager.createBackendGraph(hawk);
				db.run(new File(this.getFolder()), getConsole());
				hawk.getModelIndexer().setDB(db, false);
			}

			hawk.getModelIndexer().init(hp.getMinDelay(), hp.getMaxDelay());
		} catch (Exception e) {
			getConsole().printerrln(e);
		}

		boolean running = hawk.getModelIndexer().isRunning();

		return running;
	}

	public void stop(ShutdownRequestType requestType) {
		try {
			hawk.getModelIndexer().shutdown(requestType);
		} catch (Exception e) {
			getConsole().printerrln(e);
		}
	}

	public void sync() throws Exception {
		hawk.getModelIndexer().requestImmediateSync();
	}

	@Override
	public String toString() {
		String ret = "";
		try {
			ret = getName() + " [" + this.getFolder() + "] ";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public List<String> validateExpression(String derivationlanguage, String derivationlogic) {
		return hawk.getModelIndexer().validateExpression(derivationlanguage, derivationlogic);
	}

	private HawkProperties loadIndexerMetadata() throws Exception {
		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);
		stream.setClassLoader(HawkProperties.class.getClassLoader());
		String path = hawk.getModelIndexer().getParentFolder() + File.separator + "properties.xml";

		HawkProperties hp = (HawkProperties) stream.fromXML(new File(path));
		hawk.setDatabaseType(hp.getDbType());
		for (String[] s : hp.getMonitoredVCS()) {
			loadVCS(s[0], s[1], s.length > 2 ? Boolean.parseBoolean(s[2]) : false);
		}

		return hp;
	}

	public boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName) {
		return hawk.getModelIndexer().removeDerivedAttribute(metamodelUri, typeName, attributeName);
	}

	public boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename) {
		return hawk.getModelIndexer().removeIndexedAttribute(metamodelUri, typename, attributename);
	}

	public void removeRepository(IVcsManager manager) throws Exception {
		try {
			hawk.getModelIndexer().removeVCSManager(manager);
		} catch (Exception e) {
			getConsole().printerrln(e);
		}
	}

	public IModelIndexer getIndexer() {
		return hawk.getModelIndexer();
	}

	/**
	 * Should throw an {@link IllegalArgumentException} if the configuration for
	 * the polling is not valid (base or max <= 0 or base > max).
	 */
	public void configurePolling(int base, int max) {
		hawk.getModelIndexer().setPolling(base, max);
	}

	public void removeMetamodels(String[] selectedMetamodels) {
		try {
			hawk.getModelIndexer().removeMetamodels(selectedMetamodels);
		} catch (Exception e) {
			getConsole().printerrln(e);
		}
	}

	@Override
	public void state(HawkState state) {
		setStatus(state);
	}

	@Override
	public void info(String s) {
		setInfo(s);
	}

	@Override
	public void error(String s) {
		setInfo(s);
	}

	@Override
	public void removed() {
		// nothing to do when the state listener has been removed
	}

	public boolean removeIndexedAttributes(String[] selected) {

		boolean allSuccess = true;

		for (String s : selected) {
			String[] ss = s.split("##");
			if (ss.length == 3)
				allSuccess = allSuccess && removeIndexedAttribute(ss[0], ss[1], ss[2]);
			else {
				setInfo("internal error in removeIndexedAttributes: " + Arrays.toString(ss));
				allSuccess = false;
			}
		}
		return allSuccess;

	}

	public boolean removeDerivedAttributes(String[] selected) {
		boolean allSuccess = true;

		for (String s : selected) {
			String[] ss = s.split("##");
			if (ss.length == 3)
				allSuccess = allSuccess && removeDerivedAttribute(ss[0], ss[1], ss[2]);
			else {
				setInfo("internal error in removeIndexedAttributes: " + Arrays.toString(ss));
				allSuccess = false;
			}
		}
		return allSuccess;

	}

}
