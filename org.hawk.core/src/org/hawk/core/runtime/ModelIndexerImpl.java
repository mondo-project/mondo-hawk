/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.util.SecurityManager;
import org.hawk.core.runtime.util.TimerManager;
import org.hawk.core.util.DefaultConsole;
import org.hawk.core.util.FileOperations;

public class ModelIndexerImpl implements IModelIndexer {

	private String name;

	public static final String ID = "org.hawk.core.ModelIndexer";

	private ArrayList<IVcsManager> monitors = new ArrayList<>();
	private IGraphDatabase graph = null;
	private IMetaModelUpdater metamodelupdater = null;
	private ArrayList<String> currLocalTopRevisions = new ArrayList<>();
	private ArrayList<String> currReposTopRevisions = new ArrayList<>();

	private HashMap<String, IModelResourceFactory> modelparsers = new HashMap<>();
	private HashMap<String, IMetaModelResourceFactory> metamodelparsers = new HashMap<>();
	private HashMap<String, IQueryEngine> knownQueryLanguages = new HashMap<>();
	private LinkedList<IModelUpdater> updaters = new LinkedList<>();

	private IAbstractConsole console;

	// limited for testing (usual cap: 512)
	private int maxdelay = 1000 * 4;

	private Timer t = null;
	private Timer t2 = null;

	public boolean permanentDelete = false;
	public int currentdelay = 1000;
	public int leftoverdelay = 0;

	public char[] adminPw = null;
	private File parentfolder = null;

	private final boolean runSchedule = true;

	private HashSet<IGraphChangeDescriptor> metamodelchanges = new HashSet<>();

	/**
	 * 
	 * Creates an indexer with a name, with its contents saved in parentfolder
	 * and printing to console c.
	 * 
	 * @param name
	 * @param parentfolder
	 * @param c
	 * @throws Exception
	 */
	public ModelIndexerImpl(String name, File parentfolder, IAbstractConsole c)
			throws Exception {

		this.name = name;
		console = c;
		this.parentfolder = parentfolder;
		// registerMetamodelFiles();
	}

	/**
	 * 
	 * Creates an indexer with a name, with its contents saved in parentfolder
	 * and printing to the default console -- see ModelIndexerImpl(String name,
	 * File parentfolder, IAbstractConsole c) to print to a specific console or
	 * other output mechanism.
	 * 
	 * @param name
	 * @param parentfolder
	 * @throws Exception
	 */
	public ModelIndexerImpl(String name, File parentfolder) throws Exception {

		this.name = name;
		console = new DefaultConsole();
		this.parentfolder = parentfolder;
		// registerMetamodelFiles();
	}

	@Override
	public boolean synchronise() throws Exception {

		// System.err.println(currLocalTopRevisions);
		// System.err.println(currReposTopRevisions);

		boolean allSync = true;

		if (monitors.size() > 0) {

			for (int i = 0; i < monitors.size(); i++) {

				IVcsManager m = monitors.get(i);

				if (m.isActive()) {

					try {
						currReposTopRevisions.set(i, m.getCurrentRevision());
					} catch (Exception e1) {
						// if (e1.getMessage().contains("Authentication"))
						// {syserr(Eclipse_VCS_NoSQL_UI_view.indexers.remove(this)+"");
						// syserr("Incorrect authentication to version control, removing this indexer, please add it again with the correct credentials");}
						// else
						e1.printStackTrace();
						allSync = false;
					}

					if (!currReposTopRevisions.get(i).equals(
							currLocalTopRevisions.get(i))) {

						String monitorTempDir = graph.getTempDir();

						File temp = new File(
						// context.getBundle().getLocation()
								monitorTempDir);
						temp.mkdir();

						Set<VcsCommitItem> currreposchangeditems = new HashSet<VcsCommitItem>();
						Set<VcsCommitItem> deleteditems = new HashSet<VcsCommitItem>();

						// limit to "interesting" files
						List<VcsCommitItem> files = m
								.getDelta(currLocalTopRevisions.get(i));

						// System.err.println(files);

						HashSet<VcsCommitItem> interestingfiles = new HashSet<VcsCommitItem>();

						for (VcsCommitItem r : files) {
							String[] split = r.getPath().split("\\.");
							String extension = split[split.length - 1];
							for (String p : getKnownModelParserTypes()) {
								IModelResourceFactory parser = getModelParser(p);
								if (parser.getModelExtensions().contains(
										extension))
									interestingfiles.add(r);
							}
						}

						Iterator<VcsCommitItem> it = interestingfiles
								.iterator();

						if (it.hasNext()) {

							VcsCommitItem c = it.next();

							if (c.getChangeType().equals(VcsChangeType.DELETED)) {

								console.printerrln("-->" + c.getPath()
										+ " HAS CHANGED (" + c.getChangeType()
										+ "), PROPAGATING CHANGES");

								deleteditems.add(c);
								interestingfiles.remove(c);
							}
						}

						currreposchangeditems = graph
								.compareWithLocalFiles(interestingfiles);

						// create temp files with changed repos files
						for (VcsCommitItem s : currreposchangeditems) {

							console.printerrln("-->" + s.getPath()
									+ " HAS CHANGED (" + s.getChangeType()
									+ "), PROPAGATING CHANGES");

							String[] a = s.getPath().replaceAll("\\\\", "/")
									.split("/");

							if (a.length > 1) {
								String path = monitorTempDir;
								for (int ii = 0; ii < a.length - 1; ii++) {

									File dir = new File(path + "/" + a[ii]);
									dir.mkdir();
									path = path + "/" + a[ii];

								}
								temp = new File(path.replaceAll("\\\\", "/")
										+ "/" + a[a.length - 1]);
							} else
								temp = new File(monitorTempDir.replaceAll(
										"\\\\", "/")
										+ "/"
										+ s.getPath().replaceAll("\\\\", "/"));

							m.importFiles(s.getPath(), temp);

						}
						// sysout.println(currrepositems);

						// int[] upd = { 0, 0, 0 };

						boolean careAboutResources = false;

						for (IModelUpdater u : getUpdaters())
							if (u.caresAboutResources()) {
								careAboutResources = true;
								break;
							}

						// changedfiles / resourcessd
						HashMap<VcsCommitItem, IHawkModelResource> map = new HashMap<>();
						for (VcsCommitItem f : currreposchangeditems)
							map.put(f, null);

						if (careAboutResources) {

							for (VcsCommitItem f : currreposchangeditems) {

								String[] split = f.getPath().split("\\.");
								String extension = split[split.length - 1];

								// ModelParser parser =
								// getParserWithMetamodelExtension(extension);

								File file = new File(graph.getTempDir() + "/"
										+ f.getPath());

								if (!file.exists()) {
									console.printerrln("warning, cannot find file: "
											+ file + ", ignoring changes");
								} else {
									IHawkModelResource r = getModelParserWithModelExtension(
											extension).parse(file);
									map.put(f, r);
								}

							}
						}

						// FIXMEdone delete all removed files
						graph.exitBatchMode();

						try {
							for (VcsCommitItem c : deleteditems) {
								for (IModelUpdater u : updaters)
									u.deleteAll(c.getPath());
							}
						} catch (Exception e) {
							System.err
									.println("error in deleting removed files from store:");
							e.printStackTrace();
						}

						LinkedList<IGraphChangeDescriptor> changes = new LinkedList<>();

						if (metamodelchanges.size() > 0) {
							changes.addAll(metamodelchanges);
							metamodelchanges.clear();
						}

						for (IModelUpdater u : getUpdaters()) {
							try {
								if (currreposchangeditems.size() > 0)
									changes.add(u.updateStore(map));
							} catch (Exception e) {
								console.printerrln("updater: " + u
										+ "failed to update store");
								e.printStackTrace();

							}
						}

						// FIXME manage changes (propagate to mondix / derived
						// updaters etc)

						// changes.dosomething -- currently logs all changes to
						// files for debugging

						// temp = new File(monitorTempDir);
						// File log = new File(temp.getParent() + "/log.txt");
						// int count = 0;
						// while (log.exists()) {
						// count++;
						// log = new File(temp.getParent() + "/log" + count
						// + ".txt");
						// }
						//
						// PrintWriter w = new PrintWriter(new FileWriter(log,
						// true));
						//
						// for (IGraphChangeDescriptor change : changes) {
						//
						// w.println("---------" + change.getName()
						// + "---------");
						//
						// for (IGraphChange c : change.getChanges()) {
						//
						// w.println(c.toString());
						//
						// }
						//
						// w.println("---------" + "---------" + "---------");
						//
						// }
						//
						// w.flush();
						// w.close();

						//

						//

						boolean success = true;

						for (IGraphChangeDescriptor change : changes) {
							success = success && !change.getErrorState();
							if (change.getUnresolvedReferences() > 0
									|| change.getUnresolvedDerivedProperties() > 0) {
								console.printerrln("update left: "
										+ change.getUnresolvedReferences()
										+ " unresolved references and "
										+ change.getUnresolvedDerivedProperties()
										+ " unresolved DERIVED properties, please ensure your new models are consistent");
								// System.exit(1);
							}
						}
						// delete temporary files
						// TODO remove if you want non temp local files (for
						// example using git)
						if (!FileOperations.deleteFiles(
								new File(monitorTempDir), true))
							console.printerrln("ERROR IN DELETING TEMPORARY LOCAL VCS FILES");

						if (success)
							currLocalTopRevisions.set(i,
									currReposTopRevisions.get(i));
						else
							allSync = false;

					}

				} else {
					console.printerrln("Warning, monitor is inactive, synchronisation failed!");
					allSync = false;
				}
			}
		}
		return allSync;
	}

	private LinkedList<IModelUpdater> getUpdaters() {
		return updaters;
	}

	@Override
	public void shutdown(File f, boolean delete) throws Exception {

		if (f != null)
			saveIndexer(f);

		for (Timer t : TimerManager.timers)
			t.cancel();
		TimerManager.timers = new HashSet<>();

		for (IVcsManager monitor : monitors)
			monitor.shutdown();
		monitors = new ArrayList<>();

		if (graph != null)
			graph.shutdown(delete);
		graph = null;

	}

	@Override
	public void shutdown() throws Exception {

		for (Timer t : TimerManager.timers)
			t.cancel();
		TimerManager.timers = new HashSet<>();

		for (IVcsManager monitor : monitors)
			monitor.shutdown();
		monitors = new ArrayList<>();

		graph = null;

	}

	public String getCurrLocalTopRevision(int i) {
		return currLocalTopRevisions.get(i);
	}

	public String getCurrReposTopRevision(int i) {
		return currReposTopRevisions.get(i);
	}

	@Override
	public IGraphDatabase getGraph() {
		return graph;
	}

	@Override
	public Set<String> getKnownMMUris() {

		return graph.getKnownMMUris();

	}

	@Override
	public Set<IVcsManager> getRunningVCSManagers() {

		Set<IVcsManager> ret = new HashSet<>();

		for (IVcsManager m : monitors)
			ret.add(m);

		return ret;

	}

	private void setCurrLocalTopRevision(int i, String local) {
		currLocalTopRevisions.set(i, local);

	}

	@Override
	public String getId() {

		return "ModelIndexer | " + " | " + name;

	}

	public void runEOL() {

		try {
			JFrame fileChoserWindow = null;
			File query = null;

			fileChoserWindow = new JFrame();

			JFileChooser filechoser = new JFileChooser();
			filechoser.setDialogTitle("Chose EOL File to run:");
			File genericWorkspaceFile = new File("");
			String parent = genericWorkspaceFile.getAbsolutePath().replaceAll(
					"\\\\", "/");

			// change to workspace directory or a generic one on release
			filechoser
					.setCurrentDirectory(new File(
							new File(parent).getParentFile().getAbsolutePath()
									.replaceAll("\\\\", "/")
									+ "workspace/org.hawk.epsilon/src/org/hawk/epsilon/query"));

			if (filechoser.showDialog(fileChoserWindow, "Select File") == JFileChooser.APPROVE_OPTION)
				query = filechoser.getSelectedFile();
			else {
				System.err.println("Chosing of EOL file canceled");
			}

			fileChoserWindow.dispose();

			if (query != null) {

				// System.err.println(getKnownQueryLanguages().keySet());

				IQueryEngine q = knownQueryLanguages
						.get("org.hawk.epsilon.emc.GraphEpsilonModel");
				//
				//
				Object ret = q.contextlessQuery(graph, query);

				console.println(ret.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void logFullStore() throws Exception {

		graph.logFull();

	}

	private void registerMetamodelFiles() throws Exception {

		String s = null;
		String ep = null;
		String type = null;

		// HashMap<String, IHawkMetaModelResource> registeredMetamodels = new
		// HashMap<>();

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IGraphNode epackage : graph.getMetamodelIndex().query("id",
					"*")) {

				s = epackage.getProperty("resource").toString();
				ep = epackage.getProperty("id").toString();
				type = epackage.getProperty("type").toString();

				IMetaModelResourceFactory p = getMetaModelParser(type);

				if (p != null)
					// registeredMetamodels.put(p.getType(),
					p.parseFromString("resource_from_epackage_" + ep, s)
					// )
					;
				else
					console.printerrln("WARNING: cannot register metamodel in graph, named: "
							+ ep
							+ ", with type: "
							+ type
							+ ", as no relevant parser is registered");

			}

			t.success();
		}

		// manager.addToRegisteredMetamodels(registeredMetamodels);

	}

	@Override
	public void removeMetamodel(File metamodel) throws Exception {

		File[] mm = { metamodel };

		removeMetamodel(mm);

	}

	@Override
	public void removeMetamodel(File[] mm) throws Exception {

		HashSet<IHawkMetaModelResource> set = new HashSet<>();

		IMetaModelResourceFactory parser = null;
		String previousParserType = null;

		File[] metamodel = null;

		if (mm != null) {
			metamodel = mm;
		} else {
			//
		}

		for (File f : metamodel) {

			String[] split = f.getPath().split("\\.");
			String extension = split[split.length - 1];

			parser = getMetaModelParserWithMetamodelExtension(extension);

			if (parser == null) {
				console.printerrln("metamodel de-regstration failed, no relevant factory found");
			} else {

				String parserType = parser.getType();

				IHawkMetaModelResource metamodelResource = parser.parse(f);
				// modelResourceSet.getResource(
				// URI.createFileURI(mm.getAbsolutePath()), true);
				// metamodelResource.setURI(URI.createURI("resource_from_file_"
				// + mm.getCanonicalPath()));

				if (previousParserType != null
						&& !previousParserType.equals(parserType)) {
					System.err
							.println("cannot remove heterogeneous metamodels concurrently, plase add one metamodel type at a time");
					set.clear();
				} else {
					System.out
							.println("REMOVING metamodels in: "
									+ f
									+ " from store -- as well as any models depending on them");
					set.add(metamodelResource);
					previousParserType = parserType;
				}
			}

		}

		metamodelchanges.add(metamodelupdater.removeMetamodels(set, this));

	}

	@Override
	public void registerMetamodel(File f) throws Exception {

		File[] ret = { f };

		registerMetamodel(ret);

	}

	@Override
	public void registerMetamodel(File[] f) throws Exception {

		File[] metamodel = null;

		if (f != null) {
			metamodel = f;
		} else {
			JFrame fileChoserWindow = null;

			fileChoserWindow = new JFrame();

			JFileChooser filechoser = new JFileChooser();
			filechoser.setDialogTitle("Chose metamodel File to register:");
			filechoser.setMultiSelectionEnabled(true);
			File genericWorkspaceFile = new File("");
			String parent = genericWorkspaceFile.getAbsolutePath().replaceAll(
					"\\\\", "/");

			// change to workspace directory or a generic one on release
			filechoser
					.setCurrentDirectory(new File(
							new File(parent).getParentFile().getAbsolutePath()
									.replaceAll("\\\\", "/")
									+ "workspace/org.hawk.emf/src/org/hawk/emf/metamodel/examples/single"));

			if (filechoser.showDialog(fileChoserWindow, "Select File") == JFileChooser.APPROVE_OPTION)
				metamodel = filechoser.getSelectedFiles();
			else {
				System.err.println("Chosing of Metamodel file canceled");
			}

			fileChoserWindow.dispose();
		}

		if (metamodel != null) {

			HashSet<IHawkMetaModelResource> set = new HashSet<IHawkMetaModelResource>();

			IMetaModelResourceFactory parser = null;
			String previousParserType = null;

			for (File mm : metamodel) {

				String[] split = mm.getPath().split("\\.");
				String extension = split[split.length - 1];

				parser = getMetaModelParserWithMetamodelExtension(extension);

				if (parser == null) {
					console.printerrln("metamodel regstration failed, no relevant factory found");
				} else {

					String parserType = parser.getType();

					IHawkMetaModelResource metamodelResource = parser.parse(mm);
					// modelResourceSet.getResource(
					// URI.createFileURI(mm.getAbsolutePath()), true);
					// metamodelResource.setURI(URI.createURI("resource_from_file_"
					// + mm.getCanonicalPath()));

					if (previousParserType != null
							&& !previousParserType.equals(parserType)) {
						System.err
								.println("cannot add heterogeneous metamodels concurrently, plase add one metamodel type at a time");
						set.clear();
					} else {
						System.out.println("Adding metamodels in: " + mm
								+ " to store");
						set.add(metamodelResource);
						previousParserType = parserType;
					}
				}

				metamodelchanges.add(metamodelupdater.insertMetamodels(set,
						this));

			}
		}
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public IAbstractConsole getConsole() {
		return console;
	}

	public void addModelResourceFactory(IModelResourceFactory p) {
		modelparsers.put(p.getType(), p);
	}

	public void addMetaModelResourceFactory(IMetaModelResourceFactory p) {
		metamodelparsers.put(p.getType(), p);
	}

	public void addQueryEngine(IQueryEngine q) {
		knownQueryLanguages.put(q.getType(), q);
	}

	public IMetaModelResourceFactory getMetaModelParser(String type) {
		if (type == null)
			console.printerrln("null type given to getMetaModelParser(type), returning null");
		else
			return metamodelparsers.get(type);

		return null;
	}

	public IModelResourceFactory getModelParser(String type) {
		if (type == null)
			console.printerrln("null type given to getModelParser(type), returning null");
		else
			return modelparsers.get(type);

		return null;
	}

	public IMetaModelResourceFactory getMetaModelParserWithMetamodelExtension(
			String extension) {
		if (extension == null)
			console.printerrln("null extension given to getMetaModelParserWithMetamodelExtension(extension), returning null");
		else
			for (String p : metamodelparsers.keySet()) {
				IMetaModelResourceFactory parser = metamodelparsers.get(p);
				if (parser.getMetaModelExtensions().contains(extension))
					return parser;
			}
		return null;
	}

	public IModelResourceFactory getModelParserWithModelExtension(
			String extension) {
		if (extension == null)
			console.printerrln("null extension given to getParserWithModelExtension(extension), returning null");
		else
			for (String p : modelparsers.keySet()) {
				IModelResourceFactory parser = modelparsers.get(p);
				if (parser.getModelExtensions().contains(extension))
					return parser;
			}
		return null;
	}

	public HashMap<String, IQueryEngine> getKnownQueryLanguages() {
		return knownQueryLanguages;
	}

	public Set<String> getKnownModelParserTypes() {
		return modelparsers.keySet();
	}

	public void addModelUpdater(IModelUpdater u) {
		try {
			u.run(console, this);
			updaters.add(u);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void setMetaModelUpdater(IMetaModelUpdater u) {
		if (metamodelupdater == null) {
			u.run(// console
			);
			metamodelupdater = u;
		} else {
			System.err
					.println("metamodel updater alredy rgistered, cannot have more than one");
		}

	}

	private void saveIndexer(File f) {

		// save indexers
		try {

			File metadata = f;

			if (metadata == null) {
				metadata = new File(parentfolder.getAbsolutePath().replaceAll(
						"\\\\", "/")
						+ "/.metadata_" + name);
				metadata.delete();
			}
			// System.err.println(metadata.getAbsolutePath());

			if (!metadata.exists())
				metadata.createNewFile();

			System.out.println("saving metadata of indexer: " + name
					+ " to file: " + metadata);

			if (adminPw != null) {

				if (getGraph() != null) {

					FileWriter r = new FileWriter(metadata, true);

					r.append(name + "\t" + getGraph().getName() + "\t"
							+ getGraph().getType() + "\t");

					for (int i = 0; i < monitors.size(); i++) {

						IVcsManager m = monitors.get(i);

						r.append(m.getLocation() + ";:;" + m.getType() + ";:;");

						if (m.getUn() != null && m.getPw() != null) {

							r.append(SecurityManager.encrypt(m.getUn(), adminPw)
									+ ""
									+ SecurityManager.encrypt(m.getPw(),
											adminPw));

						} else {
							r.append("?" + ";:;" + "?");
						}

						if (!(i == (monitors.size() - 1)))
							r.append(":;:");

					}

					r.append("\r\n");

					r.flush();
					r.close();
				}
				System.out
						.println("indexer metadata saved successfuly to file: "
								+ metadata.getAbsolutePath());
			} else {
				System.err.println("null adminpw");
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err
					.println("error in saving metadata file, indexers not saved and need to be manually added on next Hawk startup (index data still saved on disk)");
		}

	}

	@Override
	public void addVCSManager(IVcsManager vcs) {

		monitors.add(vcs);
		currLocalTopRevisions.add("-3");
		currReposTopRevisions.add("-4");

	}

	@Override
	public void setDB(IGraphDatabase db) {
		graph = db;
	}

	@Override
	public void init() throws Exception {

		char[] init = new char[5];
		init[0] = 'a';
		init[1] = 'd';
		init[2] = 'm';
		init[3] = 'i';
		init[4] = 'n';

		init(init);

	}

	@Override
	public void init(char[] apw) throws Exception {

		// System.err.println("warning: automatic loading of persisted indexes on startup is only supported through the eclipse UI view extension");

		adminPw = apw;

		// register all metamodels in graph to their factories
		registerMetamodelFiles();

		// register all static metamodels to graph
		System.out
				.println("inserting static metamodels of registered metamodel factories to graph:");
		for (String factoryNames : metamodelparsers.keySet()) {
			IMetaModelResourceFactory f = metamodelparsers.get(factoryNames);
			System.out.println(f.getType());

			metamodelchanges.add(metamodelupdater.insertMetamodels(
					f.getStaticMetamodels(), this));

		}
		System.out.println("inserting static metamodels complete");

		// begin scheduled updates from vcs
		if (runSchedule) {

			t = TimerManager.createNewTimer("t", false);
			t.schedule(new TimerTask() {

				@Override
				public void run() {
					runtask();
				}
			}, 0);

			t2 = TimerManager.createNewTimer("t2", false);

			TimerTask task2 = new TimerTask() {

				@Override
				public void run() {
					leftoverdelay = leftoverdelay - 1000;
					// updateTimers(true);
				}
			};

			t2.scheduleAtFixedRate(task2, 2000, 1000);
		}
	}

	private void runtask() {

		long start = System.currentTimeMillis();
		System.err.println("------------------ starting update task");

		boolean allSame = true;

		console.println("updating indexer: " + getName());

		boolean synchronised = false;

		try {
			synchronised = synchronise();
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < monitors.size(); i++) {

			// IVCSMonitor m = monitors.get(i);

			if (getCurrReposTopRevision(i).equals(getCurrLocalTopRevision(i))) {

				//

			} else {

				if (!synchronised) {

					System.out.println("remote top: "
							+ getCurrReposTopRevision(i));
					System.out.println("local top: "
							+ getCurrLocalTopRevision(i));

					console.println("SYNCHRONISATION ERROR");

				} else
					setCurrLocalTopRevision(i, getCurrReposTopRevision(i));

				allSame = false;

			}

			// t2.restart();
		}

		if (allSame) {

			// t.stop();
			int olddelay = currentdelay;
			currentdelay = currentdelay * 2;
			if (currentdelay > maxdelay)
				currentdelay = maxdelay;
			console.println("same revision, incrementing check timer: "
					+ olddelay / 1000 + " -> " + currentdelay / 1000
					+ " (max: " + maxdelay / 1000 + ")");

		} else {

			// t.stop();
			console.println("different revisions, reseting check timer and propagating changes!");
			currentdelay = 1000;

		}

		t.schedule(new TimerTask() {

			@Override
			public void run() {
				runtask();
			}
		}, currentdelay);
		// updateTimers(false);
		leftoverdelay = currentdelay;

		long time = (System.currentTimeMillis() - start);
		System.err.println("------------------ update task took: " + time
				/ 1000 + "s" + time % 1000 + "ms");
	}

	@Override
	public File getParentFolder() {
		return parentfolder;
	}

	@Override
	public void resetScheduler() {

		currentdelay = 1000;
		leftoverdelay = 1000;

		t.cancel();
		t = TimerManager.createNewTimer("t", true);
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				runtask();
			}
		}, currentdelay);

	}

	@Override
	public void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic) {

		if (metamodelupdater.addDerivedAttribute(metamodeluri, typename,
				attributename, attributetype, isMany, isOrdered, isUnique,
				derivationlanguage, derivationlogic, this))
			for (IModelUpdater u : getUpdaters())
				u.updateDerivedAttribute(metamodeluri, typename, attributename,
						attributetype, isMany, isOrdered, isUnique,
						derivationlanguage, derivationlogic);

	}

	@Override
	public void addIndexedAttribute(String metamodeluri, String typename,
			String attributename) {

		if (metamodelupdater.addIndexedAttribute(metamodeluri, typename,
				attributename, this))
			for (IModelUpdater u : getUpdaters())
				u.updateIndexedAttribute(metamodeluri, typename, attributename);

	}

	@Override
	public Object query(File query, String queryLangID) throws Exception {
		String code = "";
		try {
			BufferedReader r = new BufferedReader(new FileReader(query));
			String line;
			while ((line = r.readLine()) != null)
				code = code + "\r\n" + line;
			r.close();
		} catch (Exception e) {
			System.err.println("error reading query file:");
			e.printStackTrace();
		}
		return query(code, queryLangID);
	}

	@Override
	public Object query(String query, String queryLangID) throws Exception {

		IQueryEngine q = knownQueryLanguages.get(queryLangID);

		if (q == null)
			throw new Exception("Unknown query engine: " + queryLangID);

		return q.contextlessQuery(graph, query);
	}

}