package org.hawk.ui2.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;
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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HModel {

	private IHawk hawk;
	private boolean running;
	private List<String> allowedPlugins;
	private List<String> registeredMetamodels;

	public List<String> getAllowedPlugins() {
		return allowedPlugins;
	}

	public ArrayList<String> getRegisteredMetamodels() {
		// return registeredMetamodels;
		return new ArrayList<String>(hawk.getModelIndexer().getKnownMMUris()// EPackage.Registry.INSTANCE.keySet()
		);
	}

	private HModel(File storageFolder, String databaseType, boolean r)
			throws Exception {
		// System.err.println(storageFolder.getPath());
		String[] path = storageFolder.getCanonicalPath().split(
				Pattern.quote(File.separator));

		String name = path[path.length - 1];

		hawk = new LocalHawk(name, storageFolder, myConsole);
		hawk.setDbtype(databaseType);
		allowedPlugins = new ArrayList<String>();
		running = r;
		registeredMetamodels = new ArrayList<String>();

	}

	public HModel(String folder, boolean b) throws Exception {
		// System.err.println(storageFolder.getPath());
		String[] path = folder.split(Pattern.quote(File.separator));

		String name = path[path.length - 1];

		hawk = new LocalHawk(name, new File(folder), myConsole);
		// hawk.setDbtype(databaseType);
		allowedPlugins = new ArrayList<String>();
		running = b;
		registeredMetamodels = new ArrayList<String>();
	}

	private static IAbstractConsole myConsole;

	public static HModel createFromFolder(String folder) throws Exception {

		if (myConsole == null)
			myConsole = new EclipseLogConsole(); // new
													// HConsole("Hawk Console V2");

		HModel hm = new HModel(folder, false);

		hm.loadIndexerMetadata();

		try {

			for (IConfigurationElement mmparse : HManager.getMmps())
				hm.hawk.getModelIndexer().addMetaModelResourceFactory(
						(IMetaModelResourceFactory) mmparse
								.createExecutableExtension("MetaModelParser"));

			for (IConfigurationElement mparse : HManager.getMps())
				hm.hawk.getModelIndexer().addModelResourceFactory(
						(IModelResourceFactory) mparse
								.createExecutableExtension("ModelParser"));

			for (IConfigurationElement ql : HManager.getLanguages())
				hm.hawk.getModelIndexer().addQueryEngine(
						(IQueryEngine) ql
								.createExecutableExtension("query_language"));

			for (IConfigurationElement updater : HManager.getUps())
				hm.hawk.getModelIndexer().addModelUpdater(
						(IModelUpdater) updater
								.createExecutableExtension("ModelUpdater"));

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = HManager.getMetaModelUpdater();
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

	public static HModel create(File folder, String dbType, List<String> plugins)
			throws Exception {

		if (myConsole == null)
			myConsole = new EclipseLogConsole(); // new
													// HConsole("Hawk Console V2");

		HModel hm = new HModel(folder, dbType, true);

		IGraphDatabase db = null;
		try {

			// create the indexer with relevant database
			System.out.println("Creating Hawk indexer...");

			// set up plugins
			// first get all of type (static callto HawkOSGIConfigManager)
			// check each one has the an ID that was selected
			// create VCS
			// call m.add
			System.out.println("adding metamodel resource factories:");
			for (IConfigurationElement mmparse : HManager.getMmps()) {
				IMetaModelResourceFactory f = (IMetaModelResourceFactory) mmparse
						.createExecutableExtension("MetaModelParser");
				hm.hawk.getModelIndexer().addMetaModelResourceFactory(f);
				System.out.println(f.getHumanReadableName());
			}
			System.out.println("adding model resource factories:");
			for (IConfigurationElement mparse : HManager.getMps()) {
				IModelResourceFactory f = (IModelResourceFactory) mparse
						.createExecutableExtension("ModelParser");
				hm.hawk.getModelIndexer().addModelResourceFactory(f);
				System.out.println(f.getHumanReadableName());
			}
			System.out.println("adding query engines:");
			for (IConfigurationElement ql : HManager.getLanguages()) {
				IQueryEngine q = (IQueryEngine) ql
						.createExecutableExtension("query_language");
				hm.hawk.getModelIndexer().addQueryEngine(q);
				System.out.println(q.getType());
			}
			System.out.println("adding model updaters:");
			for (IConfigurationElement updater : HManager.getUps()) {
				IModelUpdater u = (IModelUpdater) updater
						.createExecutableExtension("ModelUpdater");
				hm.hawk.getModelIndexer().addModelUpdater(u);
				System.out.println(u.getName());
			}
			System.out.println("setting up hawk's back-end store:");
			db = HManager.createGraph(hm.hawk);
			db.run(folder, myConsole);
			hm.hawk.getModelIndexer().setDB(db);

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = HManager.getMetaModelUpdater();
			System.out.println("setting up hawk's metamodel updater:\n"
					+ metaModelUpdater.getName());
			hm.hawk.getModelIndexer().setMetaModelUpdater(metaModelUpdater);

			hm.hawk.init();

			HManager.addHawk(hm);
			// System.err.println("indexer added");
			System.out.println("Created Hawk indexer!");
			return hm;

		} catch (Exception e) {
			System.err.println("Exception in trying to add new Indexer:");
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.err
					.println("Adding of indexer aborted, please try again.\nShutting down and removing back-end (if it was created)");
			try {
				db.shutdown(true);
			} catch (Exception e2) {
			}
			System.err.println("aborting finished.");
		}

		return null;
	}

	public String getFolder() {
		return hawk.getModelIndexer().getParentFolder().toString();
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		try {
			// create the indexer with relevant database
			IGraphDatabase db = HManager.createGraph(hawk);
			db.run(new File(this.getFolder()), myConsole);
			hawk.getModelIndexer().setDB(db);

			loadIndexerMetadata();

			hawk.init();
			running = true;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadIndexerMetadata() {

		// ...

		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);
		stream.setClassLoader(HawkProperties.class.getClassLoader());
		// stream.createObjectInputStream(in)

		String path = hawk.getModelIndexer().getParentFolder() + File.separator
				+ "properties.xml";

		// System.out.println(path);

		HawkProperties hp = (HawkProperties) stream.fromXML(new File(path));

		// System.out.println(stream.toXML(hp));

		hawk.setDbtype(hp.getDbType());

		for (String[] s : hp.getMonitoredVCS()) {

			addencryptedVCS(s[0], s[1], s[2], s[3]);

		}

	}

	public void stop() {
		try {

			hawk.getModelIndexer().shutdown(false);

			running = false;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void delete() {

		File f = hawk.getModelIndexer().getParentFolder();
		while (this.isRunning()) {
			try {
				hawk.getModelIndexer().shutdown(true);
				running = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (int i = 0; i < 5 || deleteDir(f); i++) {

		}

		if (f.exists())
			System.err.println("failed to delete directory: " + f);

	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	@Override
	public String toString() {
		String ret = "";
		try {
			ret = getFolder()
					+ (this.isRunning() ? " (running) " : " (stopped) ") + " ["
					+ this.getFolder() + "] ";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	// public String query(String query) {
	// return query(query, "org.hawk.epsilon.emc.GraphEpsilonModel");
	// }

	public Set<String> getKnownQueryLanguages() {

		return hawk.getModelIndexer().getKnownQueryLanguages().keySet();

	}

	public String contextFullQuery(String query, String ql,
			Map<String, String> context) {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);

		Object ret = q.contextfullQuery(hawk.getModelIndexer().getGraph(),
				query, context);

		return ret != null ? ret.toString() : "null";
	}

	public String contextFullQuery(File query, String ql,
			Map<String, String> context) {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);

		Object ret = q.contextfullQuery(hawk.getModelIndexer().getGraph(),
				query, context);

		return ret != null ? ret.toString() : "null";
	}

	public String query(String query, String ql) {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);

		Object ret = q.contextlessQuery(hawk.getModelIndexer().getGraph(),
				query);

		return ret != null ? ret.toString() : "null";
	}

	public String query(File query, String ql) {
		IQueryEngine q = hawk.getModelIndexer().getKnownQueryLanguages()
				.get(ql);

		Object ret = q.contextlessQuery(hawk.getModelIndexer().getGraph(),
				query);

		return ret != null ? ret.toString() : "null";
	}

	public boolean registerMeta(File f) {
		if (registeredMetamodels.contains(f.getAbsolutePath()))
			return true;
		try {
			hawk.getModelIndexer().registerMetamodel(f);
		} catch (Exception e) {
			return false;
		}
		this.registeredMetamodels.add(f.getAbsolutePath());
		return true;
	}

	public Collection<String> getVCSTypeNames() {
		return HManager.getVCSTypes();
	}

	public void addencryptedVCS(String loc, String type, String user,
			String pass) {
		try {
			if (!this.getLocations().contains(loc)) {
				IVcsManager mo = HManager.createVCSManager(type);
				mo.run(loc, hawk.decrypt(user), hawk.decrypt(pass), myConsole);
				hawk.getModelIndexer().addVCSManager(mo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addVCS(String loc, String type, String user, String pass) {
		try {
			if (!this.getLocations().contains(loc)) {
				IVcsManager mo = HManager.createVCSManager(type);
				mo.run(loc, user, pass, myConsole);
				hawk.getModelIndexer().addVCSManager(mo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// public void addLocal(String loc) {
	// try {
	// if (!this.getLocations().contains(loc)) {
	// IVcsManager mo = HManager
	// .createVCSManager("org.hawk.localfolder.LocalFolder");
	// mo.run(loc, "", "", myConsole);
	// index.addVCSManager(mo);
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public Collection<String> getLocations() {

		List<String> locations = new ArrayList<String>();
		;
		for (IVcsManager o : hawk.getModelIndexer().getRunningVCSManagers()) {
			locations.add(o.getLocation());
		}
		return locations;
	}

	public Collection<String> getLocalLocations() {

		List<String> locations = new ArrayList<String>();
		;
		for (IVcsManager o : hawk.getModelIndexer().getRunningVCSManagers()) {
			if (o.getType().contains("localfolder"))
				locations.add(o.getLocation());
		}
		return locations;
	}

	public IGraphDatabase getGraph() {
		return hawk.getModelIndexer().getGraph();
	}

	public void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, Boolean isMany,
			Boolean isOrdered, Boolean isUnique, String derivationlanguage,
			String derivationlogic) {
		hawk.getModelIndexer().addDerivedAttribute(metamodeluri, typename,
				attributename, attributetype, isMany, isOrdered, isUnique,
				derivationlanguage, derivationlogic);
	}

	public void addIndexedAttribute(String metamodeluri, String typename,
			String attributename) {
		hawk.getModelIndexer().addIndexedAttribute(metamodeluri, typename,
				attributename);
	}

	public Collection<String> getDerivedAttributes() {
		return hawk.getModelIndexer().getDerivedAttributes();
	}

	public Collection<String> getIndexedAttributes() {
		return hawk.getModelIndexer().getIndexedAttributes();
	}

	public Collection<String> getIndexes() {
		return hawk.getModelIndexer().getIndexes();
	}

	public List<String> validateExpression(String derivationlanguage,
			String derivationlogic) {
		return hawk.getModelIndexer().validateExpression(derivationlanguage,
				derivationlogic);
	}

	public String getName() {
		return hawk.getModelIndexer().getName();
	}

}
