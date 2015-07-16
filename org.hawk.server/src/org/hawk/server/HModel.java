/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - move to servlet project, use explicit HManager instances
 ******************************************************************************/
package org.hawk.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.hawk.core.IAbstractConsole;
import org.hawk.core.IHawk;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.LocalHawk;
import org.hawk.core.util.HawkConfig;
import org.hawk.core.util.HawkProperties;
import org.hawk.core.util.HawksConfig;
import org.osgi.service.prefs.BackingStoreException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HModel {

	private static IAbstractConsole CONSOLE = new SLF4JConsole();

	private static IAbstractConsole getConsole() {
		if (CONSOLE == null)
			CONSOLE = new SLF4JConsole();
		return CONSOLE;
	}

	public static HModel create(String name, File folder, String dbType,
			HManager manager) throws Exception {
		HModel hm = new HModel(manager, name, folder, dbType, true);

		IGraphDatabase db = null;
		final IAbstractConsole console = getConsole();
		try {
			// create the indexer with relevant database
			console.println("Creating Hawk indexer...");

			// set up plugins
			// first get all of type (static callto HawkOSGIConfigManager)
			// check each one has the an ID that was selected
			// create VCS
			// call m.add
			console.println("adding metamodel resource factories:");
			for (IConfigurationElement mmparse : manager.getMmps()) {
				IMetaModelResourceFactory f = (IMetaModelResourceFactory) mmparse
						.createExecutableExtension("MetaModelParser");
				hm.hawk.getModelIndexer().addMetaModelResourceFactory(f);
				console.println(f.getHumanReadableName());
			}
			console.println("adding model resource factories:");
			for (IConfigurationElement mparse : manager.getMps()) {
				IModelResourceFactory f = (IModelResourceFactory) mparse
						.createExecutableExtension("ModelParser");
				hm.hawk.getModelIndexer().addModelResourceFactory(f);
				console.println(f.getHumanReadableName());
			}
			console.println("adding query engines:");
			for (IConfigurationElement ql : manager.getLanguages()) {
				IQueryEngine q = (IQueryEngine) ql
						.createExecutableExtension("query_language");
				hm.hawk.getModelIndexer().addQueryEngine(q);
				console.println(q.getType());
			}
			console.println("adding model updaters:");
			for (IConfigurationElement updater : manager.getUps()) {
				IModelUpdater u = (IModelUpdater) updater
						.createExecutableExtension("ModelUpdater");
				hm.hawk.getModelIndexer().addModelUpdater(u);
				console.println(u.getName());
			}
			console.println("setting up hawk's back-end store:");
			db = manager.createGraph(hm.hawk);
			db.run(folder, console);
			hm.hawk.getModelIndexer().setDB(db);

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = manager.getMetaModelUpdater();
			console.println("setting up hawk's metamodel updater:\n"
					+ metaModelUpdater.getName());
			hm.hawk.getModelIndexer().setMetaModelUpdater(metaModelUpdater);

			hm.hawk.init();

			manager.addHawk(hm);
			manager.saveHawkToMetadata(hm);
			console.println("Created Hawk indexer!");
			return hm;

		} catch (Exception e) {
			console.printerrln(e.getMessage());
			console.printerrln("Adding of indexer aborted, please try again.\nShutting down and removing back-end (if it was created)");
			try {
				db.shutdown(true);
			} catch (Exception e2) {
				throw e2;
			}
			console.printerrln("aborting finished.");
			throw e;
		}
	}

	public static HModel createFromFolder(HawkConfig s, HManager manager)
			throws Exception {
		HModel hm = new HModel(manager, s, false);

		try {
			for (IConfigurationElement mmparse : manager.getMmps())
				hm.hawk.getModelIndexer().addMetaModelResourceFactory(
						(IMetaModelResourceFactory) mmparse
								.createExecutableExtension("MetaModelParser"));

			for (IConfigurationElement mparse : manager.getMps())
				hm.hawk.getModelIndexer().addModelResourceFactory(
						(IModelResourceFactory) mparse
								.createExecutableExtension("ModelParser"));

			for (IConfigurationElement ql : manager.getLanguages())
				hm.hawk.getModelIndexer().addQueryEngine(
						(IQueryEngine) ql
								.createExecutableExtension("query_language"));

			for (IConfigurationElement updater : manager.getUps())
				hm.hawk.getModelIndexer().addModelUpdater(
						(IModelUpdater) updater
								.createExecutableExtension("ModelUpdater"));

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = manager.getMetaModelUpdater();
			hm.hawk.getModelIndexer().setMetaModelUpdater(metaModelUpdater);

		} catch (Exception e) {
			System.err
					.println("Exception in trying to add create Indexer from folder:");
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.err.println("Adding of indexer aborted, please try again");
		}

		return hm;

	}

	private List<String> allowedPlugins;
	private IHawk hawk;
	private HManager manager;
	private boolean running;

	public HModel(HManager manager, HawkConfig config, boolean isRunning)
			throws Exception {
		this(manager, config.getName(), new File(config.getLoc()), null,
				isRunning);
	}

	private HModel(HManager manager, String name, File storageFolder,
			String databaseType, boolean isRunning) throws Exception {
		this.manager = manager;
		this.hawk = new LocalHawk(name, storageFolder, getConsole());
		if (databaseType != null) {
			this.hawk.setDbtype(databaseType);
		}
		this.allowedPlugins = new ArrayList<String>();
		this.running = isRunning;
	}

	public void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, Boolean isMany,
			Boolean isOrdered, Boolean isUnique, String derivationlanguage,
			String derivationlogic) throws Exception {
		hawk.getModelIndexer().addDerivedAttribute(metamodeluri, typename,
				attributename, attributetype, isMany, isOrdered, isUnique,
				derivationlanguage, derivationlogic);
	}

	public void addEncryptedVCS(String loc, String type, String user,
			String pass) throws Exception {
		if (!this.getLocations().contains(loc)) {
			IVcsManager mo = manager.createVCSManager(type);
			mo.run(loc, hawk.decrypt(user), hawk.decrypt(pass), getConsole());
			hawk.getModelIndexer().addVCSManager(mo);
		}
	}

	public void addIndexedAttribute(String metamodeluri, String typename,
			String attributename) throws Exception {
		hawk.getModelIndexer().addIndexedAttribute(metamodeluri, typename,
				attributename);
	}

	public void addVCS(String loc, String type, String user, String pass) {
		try {
			if (!this.getLocations().contains(loc)) {
				IVcsManager mo = manager.createVCSManager(type);
				mo.run(loc, user, pass, getConsole());
				hawk.getModelIndexer().addVCSManager(mo);
			}
		} catch (Exception e) {
			getConsole().printerrln(e.getMessage());
		}
	}

	/**
	 * Performs a context-aware query and returns its result. The result must be
	 * a Double, a String, an Integer, a ModelElement, the null reference or an
	 * Iterable of these things.
	 * 
	 * @throws NoSuchElementException
	 *             Unknown query language.
	 */
	public Object contextFullQuery(File query, String ql,
			Map<String, String> context) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);
		if (q == null) {
			throw new NoSuchElementException();
		}

		return q.contextfullQuery(hawk.getModelIndexer().getGraph(), query,
				context);
	}

	/**
	 * Performs a context-aware query and returns its result. For the result
	 * types, see {@link #contextFullQuery(File, String, Map)}.
	 * 
	 * @throws NoSuchElementException
	 *             Unknown query language.
	 */
	public Object contextFullQuery(String query, String ql,
			Map<String, String> context) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);
		if (q == null) {
			throw new NoSuchElementException();
		}

		return q.contextfullQuery(hawk.getModelIndexer().getGraph(), query,
				context);
	}

	public void delete() throws BackingStoreException {
		removeHawkFromMetadata(new HawkConfig(getName(), getFolder()));

		File f = hawk.getModelIndexer().getParentFolder();
		while (this.isRunning()) {
			try {
				// XXX shutting down hawk does not delete the storage.
				hawk.getModelIndexer().shutdown(false);
				running = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (f.exists())
			System.err.println("failed to delete directory: " + f);
	}

	public boolean exists() {
		return hawk != null && hawk.exists();
	}

	public List<String> getAllowedPlugins() {
		return allowedPlugins;
	}

	public Collection<String> getDerivedAttributes() {
		return hawk.getModelIndexer().getDerivedAttributes();
	}

	public String getFolder() {
		return hawk.getModelIndexer().getParentFolder().toString();
	}

	public IGraphDatabase getGraph() {
		return hawk.getModelIndexer().getGraph();
	}

	public Collection<String> getIndexedAttributes() {
		return hawk.getModelIndexer().getIndexedAttributes();
	}

	public Collection<String> getIndexes() {
		return hawk.getModelIndexer().getIndexes();
	}

	public Set<String> getKnownQueryLanguages() {
		return hawk.getModelIndexer().getKnownQueryLanguages().keySet();
	}

	public Collection<String> getLocalLocations() {
		List<String> locations = new ArrayList<String>();
		for (IVcsManager o : hawk.getModelIndexer().getRunningVCSManagers()) {
			if (o.getType().contains("localfolder"))
				locations.add(o.getLocation());
		}
		return locations;
	}

	public Collection<String> getLocations() {
		List<String> locations = new ArrayList<String>();
		for (IVcsManager o : hawk.getModelIndexer().getRunningVCSManagers()) {
			locations.add(o.getLocation());
		}
		return locations;
	}

	public String getName() {
		return hawk.getModelIndexer().getName();
	}

	public ArrayList<String> getRegisteredMetamodels() {
		return new ArrayList<String>(hawk.getModelIndexer().getKnownMMUris());
	}

	public Collection<String> getVCSTypeNames() {
		return manager.getVCSTypes();
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * For the result types, see {@link #contextFullQuery(File, String, Map)}.
	 */
	public Object query(File query, String ql) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);

		return q.contextlessQuery(hawk.getModelIndexer().getGraph(), query);
	}

	/**
	 * For the result types, see {@link #contextFullQuery(File, String, Map)}.
	 */
	public Object query(String query, String ql) throws Exception {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);
		return q.contextlessQuery(hawk.getModelIndexer().getGraph(), query);
	}

	public boolean registerMeta(File... f) {
		try {
			hawk.getModelIndexer().registerMetamodel(f);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void removeHawkFromMetadata(HawkConfig config)
			throws BackingStoreException {
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
			getConsole()
					.printerrln(
							"removeHawkFromMetadata tried to load preferences but it could not.");
		}
	}

	public boolean start(HManager manager, char[] apw) {
		try {
			hawk.getModelIndexer().setAdminPassword(apw);
			loadIndexerMetadata();
		} catch (Exception e) {
			getConsole().printerrln(e.getMessage());
		}

		try {
			// create the indexer with relevant database
			IGraphDatabase db = manager.createGraph(hawk);
			db.run(new File(this.getFolder()), getConsole());
			hawk.getModelIndexer().setDB(db);
			hawk.init();

			running = true;
		} catch (Exception e) {
			getConsole().printerrln(e.getMessage());
		}

		return running;
	}

	public void stop() {
		try {
			hawk.getModelIndexer().shutdown(false);
			running = false;
		} catch (Exception e) {
			getConsole().printerrln(e.getMessage());
		}
	}

	@Override
	public String toString() {
		String ret = "";
		try {
			ret = getName()
					+ (this.isRunning() ? " (running) " : " (stopped) ") + " ["
					+ this.getFolder() + "] ";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public List<String> validateExpression(String derivationlanguage,
			String derivationlogic) {
		return hawk.getModelIndexer().validateExpression(derivationlanguage,
				derivationlogic);
	}

	private void loadIndexerMetadata() throws Exception {
		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);
		stream.setClassLoader(HawkProperties.class.getClassLoader());
		String path = hawk.getModelIndexer().getParentFolder() + File.separator
				+ "properties.xml";

		HawkProperties hp = (HawkProperties) stream.fromXML(new File(path));
		hawk.setDbtype(hp.getDbType());
		for (String[] s : hp.getMonitoredVCS()) {
			addEncryptedVCS(s[0], s[1], s[2], s[3]);
		}
	}

	public void removeDerivedAttribute(String metamodelUri, String typeName,
			String attributeName) {
		// TODO Auto-generated method stub

	}

	public void removeIndexedAttribute(String metamodelUri, String typename,
			String attributename) {
		// TODO Auto-generated method stub

	}

	public Iterable<ModelElement> resolveProxies(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeRepository(String uri) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * Should throw an {@link IllegalArgumentException} if the configuration for
	 * the polling is not valid (base or max <= 0 or base > max).
	 */
	public void configurePolling(int base, int max) {
		// TODO Auto-generated method stub
	}

}
