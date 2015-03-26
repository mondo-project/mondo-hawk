package org.hawk.ui2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.hawk.core.IAbstractConsole;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.runtime.util.SecurityManager;

public class HModel {

	private static String apw = "admin";
	private IModelIndexer index;
	private String dbid;
	private boolean running;
	private List<String> allowedPlugins;
	private List<String> registeredMetamodels;

	public List<String> getAllowedPlugins() {
		return allowedPlugins;
	}

	public ArrayList<String> getRegisteredMetamodels() {
		// return registeredMetamodels;
		return new ArrayList<String>(index.getKnownMMUris()// EPackage.Registry.INSTANCE.keySet()
		);
	}

	private HModel(IModelIndexer e, String dbid, boolean r) {
		index = e;
		this.dbid = dbid;
		allowedPlugins = new ArrayList<String>();
		running = r;
		registeredMetamodels = new ArrayList<String>();
	}

	private static IAbstractConsole myConsole;

	public static HModel createFromFolder(String indexerName,
			String folderName, String dbid) {

		if (myConsole == null)
			myConsole = new EclipseLogConsole(); //new HConsole("Hawk Console V2");
		IModelIndexer m = null;

		try {
			m = new ModelIndexerImpl(indexerName, new File(folderName),
					myConsole);

			for (IConfigurationElement mmparse : HManager.getMmps())
				m.addMetaModelResourceFactory((IMetaModelResourceFactory) mmparse
						.createExecutableExtension("MetaModelParser"));

			for (IConfigurationElement mparse : HManager.getMps())
				m.addModelResourceFactory((IModelResourceFactory) mparse
						.createExecutableExtension("ModelParser"));

			for (IConfigurationElement ql : HManager.getLanguages())
				m.addQueryEngine((IQueryEngine) ql
						.createExecutableExtension("query_language"));

			for (IConfigurationElement updater : HManager.getUps())
				m.addModelUpdater((IModelUpdater) updater
						.createExecutableExtension("ModelUpdater"));

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = HManager.getMetaModelUpdater();
			m.setMetaModelUpdater(metaModelUpdater);

		} catch (Exception e) {
			System.err
					.println("Exception in trying to add create Indexer from folder:");
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.err.println("Adding of indexer aborted, please try again");
		}

		HModel hm = new HModel(m, dbid, false);
		return hm;

	}

	private void loadIndexerMetadata(String indexname) {

		File config = new File(getFolder() + File.separator + ".metadata_"
				+ indexname);

		if (config.exists() && config.isFile() && config.canRead()) {
			try {

				BufferedReader r = new BufferedReader(new FileReader(config));
				String indexer = r.readLine();

				String[] parse = indexer.split("\t");

				if (parse.length > 3) {

					String monitormetadata = parse[3];
					for (String vcs : monitormetadata.split(":;:")) {

						String[] vcsmd = vcs.split(";:;");

						addencryptedVCS(vcsmd[0], vcsmd[1], vcsmd[2], vcsmd[3]);

					}

					// myConsole.println("indexer '" + parse[0] + "' loaded");

				}

				r.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		else {
			System.err.println(config.exists() + " " + config.isFile() + " "
					+ config.canRead());
		}

	}

	// private void saveConfig() {
	// saveLocalFolderConfig();
	// }

	// private void saveLocalFolderConfig() {
	// Properties p = createPropertyList("folder",
	// (List<String>) this.getLocalLocations());
	// saveConfig(index.getParentFolder().getAbsolutePath(), ".localfolder", p);
	// }

	// private void loadLocalFolderConfig() {
	// for (String folder : getPropertyList(
	// loadConfig(this.getFolder(), ".localfolder"), "folder")) {
	// this.addLocal(folder);
	// }
	// }

	// private static Properties loadConfig(String folderName, String
	// configName) {
	// File config = new File(folderName + File.separator + configName);
	// Properties p = new Properties();
	// if (config.exists() && config.isFile() && config.canRead()) {
	// try {
	// FileInputStream fis = new FileInputStream(
	// config.getAbsolutePath());
	// p.load(fis);
	// fis.close();
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// }
	// }
	// return p;
	// }

	// private static void saveConfig(String folderName, String configName,
	// Properties p) {
	// File config = new File(folderName + File.separator + configName);
	//
	// if (config.exists() && config.isFile() && config.canWrite()) {
	// config.delete();
	// }
	//
	// if (!config.exists()) {
	// try {
	// FileOutputStream fos = new FileOutputStream(
	// config.getAbsolutePath());
	// p.store(fos, "");
	// fos.close();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }

	// private static List<String> getPropertyList(Properties properties,
	// String name) {
	// List<String> result = new ArrayList<String>();
	// for (Entry<Object, Object> entry : properties.entrySet()) {
	// if (((String) entry.getKey()).matches("^" + Pattern.quote(name)
	// + "\\.\\d+$")) {
	// result.add((String) entry.getValue());
	// }
	// }
	// return result;
	// }

	// private static Properties createPropertyList(String key, List<String>
	// values) {
	// Properties p = new Properties();
	// for (int i = 0; i < values.size(); i++) {
	// p.setProperty(key + "." + i, values.get(i));
	// }
	// return p;
	// }

	public static HModel create(String indexerName, String folderName,
			String dbid, List<String> plugins) {

		if (myConsole == null)
			myConsole = new EclipseLogConsole(); //new HConsole("Hawk Console V2");

		IModelIndexer m;
		IGraphDatabase db = null;
		try {
			
			//TODO: Do something with the indexer name
			
			m = new ModelIndexerImpl(indexerName, new File(folderName),
					myConsole);

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
				m.addMetaModelResourceFactory(f);
				System.out.println(f.getHumanReadableName());
			}
			System.out.println("adding model resource factories:");
			for (IConfigurationElement mparse : HManager.getMps()) {
				IModelResourceFactory f = (IModelResourceFactory) mparse
						.createExecutableExtension("ModelParser");
				m.addModelResourceFactory(f);
				System.out.println(f.getHumanReadableName());
			}
			System.out.println("adding query engines:");
			for (IConfigurationElement ql : HManager.getLanguages()) {
				IQueryEngine q = (IQueryEngine) ql
						.createExecutableExtension("query_language");
				m.addQueryEngine(q);
				System.out.println(q.getType());
			}
			System.out.println("adding model updaters:");
			for (IConfigurationElement updater : HManager.getUps()) {
				IModelUpdater u = (IModelUpdater) updater
						.createExecutableExtension("ModelUpdater");
				m.addModelUpdater(u);
				System.out.println(u.getName());
			}
			System.out.println("setting up hawk's back-end store:\n" + dbid);
			db = HManager.createGraph(dbid);
			db.run(indexerName, new File(folderName), myConsole);
			m.setDB(db);

			// hard coded metamodel updater?
			IMetaModelUpdater metaModelUpdater = HManager.getMetaModelUpdater();
			System.out.println("setting up hawk's metamodel updater:\n"
					+ metaModelUpdater.getName());
			m.setMetaModelUpdater(metaModelUpdater);

			m.init(apw.toCharArray());
			HModel hm = new HModel(m, dbid, true);

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

	public String getName() {
		return index.getName();
	}

	public String getFolder() {
		return index.getParentFolder().toString();
	}

	private String getDBID() {
		return dbid;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		try {
			// create the indexer with relevant database
			IGraphDatabase db = HManager.createGraph(this.getDBID());
			db.run(this.getName(), new File(this.getFolder()), myConsole);
			index.setDB(db);

			this.loadIndexerMetadata(index.getName());

			index.init(apw.toCharArray());
			running = true;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			// this.saveConfig();
			String metaData = index.getParentFolder().getAbsolutePath()
					+ File.separator + ".metadata_" + index.getName();
			index.shutdown(new File(metaData), false);

			running = false;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void delete() {

		File f = index.getParentFolder();
		while (this.isRunning()) {
			try {
				index.shutdown(null, true);
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

	public String toString() {
		return this.getName()
				+ (this.isRunning() ? " (running) " : " (stopped) ") + " ["
				+ this.getFolder() + "] ";
	}

	// public String query(String query) {
	// return query(query, "org.hawk.epsilon.emc.GraphEpsilonModel");
	// }

	public Set<String> getKnownQueryLanguages() {

		return index.getKnownQueryLanguages().keySet();

	}

	public String contextFullQuery(String query, String ql,
			Map<String, String> context) {
		IQueryEngine q = index.getKnownQueryLanguages().get(ql);

		Object ret = q.contextfullQuery(index.getGraph(), query, context);

		return ret != null ? ret.toString() : "null";
	}

	public String contextFullQuery(File query, String ql,
			Map<String, String> context) {
		IQueryEngine q = index.getKnownQueryLanguages().get(ql);

		Object ret = q.contextfullQuery(index.getGraph(), query, context);

		return ret != null ? ret.toString() : "null";
	}

	public String query(String query, String ql) {
		IQueryEngine q = index.getKnownQueryLanguages().get(ql);

		Object ret = q.contextlessQuery(index.getGraph(), query);

		return ret != null ? ret.toString() : "null";
	}

	public String query(File query, String ql) {
		IQueryEngine q = index.getKnownQueryLanguages().get(ql);

		Object ret = q.contextlessQuery(index.getGraph(), query);

		return ret != null ? ret.toString() : "null";
	}

	public boolean registerMeta(File f) {
		if (registeredMetamodels.contains(f.getAbsolutePath()))
			return true;
		try {
			index.registerMetamodel(f);
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
				mo.run(loc, SecurityManager.decrypt(user, apw.toCharArray()),
						SecurityManager.decrypt(pass, apw.toCharArray()), myConsole);
				index.addVCSManager(mo);
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
				index.addVCSManager(mo);
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
		for (IVcsManager o : index.getRunningVCSManagers()) {
			locations.add(o.getLocation());
		}
		return locations;
	}

	public Collection<String> getLocalLocations() {

		List<String> locations = new ArrayList<String>();
		;
		for (IVcsManager o : index.getRunningVCSManagers()) {
			if (o.getType().contains("localfolder"))
				locations.add(o.getLocation());
		}
		return locations;
	}

	public IGraphDatabase getGraph() {
		return index.getGraph();
	}

	public void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, Boolean isMany,
			Boolean isOrdered, Boolean isUnique, String derivationlanguage,
			String derivationlogic) {
		index.addDerivedAttribute(metamodeluri, typename, attributename,
				attributetype, isMany, isOrdered, isUnique, derivationlanguage,
				derivationlogic);
	}

	public void addIndexedAttribute(String metamodeluri, String typename,
			String attributename) {
		index.addIndexedAttribute(metamodeluri, typename, attributename);
	}

	public Collection<String> getDerivedAttributes() {
		return index.getDerivedAttributes();
	}

	public Collection<String> getIndexedAttributes() {
		return index.getIndexedAttributes();
	}

	public Collection<String> getIndexes() {
		return index.getIndexes();
	}

	public List<String> validateExpression(String derivationlanguage,
			String derivationlogic) {
		return index.validateExpression(derivationlanguage, derivationlogic);
	}

}
