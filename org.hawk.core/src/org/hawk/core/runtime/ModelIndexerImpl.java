/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IStateListener;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.util.FileOperations;
import org.hawk.core.util.HawkProperties;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ModelIndexerImpl implements IModelIndexer {

	// validation metrics and data
	private boolean isSyncMetricsEnabled = false;
	private Map<VcsCommitItem, IHawkModelResource> fileToResourceMap = new HashMap<>();
	private int deletedFiles;
	private int interestingFiles;
	private int currchangeditems;
	private int loadedResources;
	private long synctime;

	private boolean latestUpdateFoundChanges = false;

	private final class RunUpdateTask extends TimerTask {
		@Override
		public void run() {
			runUpdateTask();
		}
	}

	public static final String ID = "org.hawk.core.ModelIndexer";

	private static final boolean IS_DERIVED = true;
	private static final boolean IS_INDEXED = false;

	private String name;

	private List<IVcsManager> monitors = new ArrayList<>();
	private IGraphDatabase graph = null;
	private IMetaModelUpdater metamodelupdater = null;
	private Map<String, String> currLocalTopRevisions = new HashMap<>();
	private Map<String, String> currReposTopRevisions = new HashMap<>();

	private Map<String, IModelResourceFactory> modelparsers = new HashMap<>();
	private Map<String, IMetaModelResourceFactory> metamodelparsers = new HashMap<>();
	private Map<String, IQueryEngine> knownQueryLanguages = new HashMap<>();
	private List<IModelUpdater> updaters = new LinkedList<>();

	private IConsole console;

	public static final int DEFAULT_MAXDELAY = 1000 * 512;
	public static final int DEFAULT_MINDELAY = 1000;
	private int maxDelay = DEFAULT_MAXDELAY;
	private int minDelay = DEFAULT_MINDELAY;
	private int currentDelay = minDelay;
	private Timer updateTimer = null;

	public boolean permanentDelete = false;

	private File parentfolder = null;
	private boolean running = false;

	private final ICredentialsStore credStore;
	private final CompositeGraphChangeListener listener = new CompositeGraphChangeListener();
	private final CompositeStateListener stateListener = new CompositeStateListener();

	/**
	 * Creates an indexer with a <code>name</code>, with its contents saved in
	 * <code>parentfolder</code> and printing to console <code>c</code>.
	 */
	public ModelIndexerImpl(String name, File parentfolder,
			ICredentialsStore credStore, IConsole c) throws Exception {
		this.name = name;
		this.console = c;
		this.credStore = credStore;
		this.parentfolder = parentfolder;
	}

	@Override
	public void requestImmediateSync() {
		if (running) {
			updateTimer.schedule(new RunUpdateTask(), 0);
		}
	}

	private boolean internalSynchronise() throws Exception {
		listener.synchroniseStart();
		stateListener.state(HawkState.UPDATING);

		try {
			// System.err.println(currLocalTopRevisions);
			// System.err.println(currReposTopRevisions);

			long start = System.currentTimeMillis();
			boolean allSync = true;
			fileToResourceMap = new HashMap<>();
			deletedFiles = 0;
			interestingFiles = 0;
			currchangeditems = 0;
			loadedResources = 0;
			synctime = 0;

			latestUpdateFoundChanges = false;

			if (monitors.size() > 0) {

				for (IVcsManager m : monitors) {

					if (m.isActive()) {

						try {
							currReposTopRevisions.put(m.getLocation(),
									m.getCurrentRevision());
						} catch (Exception e1) {
							// if (e1.getMessage().contains("Authentication"))
							// {syserr(Eclipse_VCS_NoSQL_UI_view.indexers.remove(this)+"");
							// syserr("Incorrect authentication to version
							// control, removing this indexer, please add it
							// again with the correct credentials");}
							// else
							e1.printStackTrace();
							allSync = false;
						}

						if (!currReposTopRevisions.get(m.getLocation()).equals(
								currLocalTopRevisions.get(m.getLocation()))) {

							boolean success = true;

							latestUpdateFoundChanges = true;

							String monitorTempDir = graph.getTempDir();

							File temp = new File(
							// context.getBundle().getLocation()
									monitorTempDir);
							temp.mkdir();

							Set<VcsCommitItem> deleteditems = new HashSet<VcsCommitItem>();

							// limit to "interesting" files
							List<VcsCommitItem> files = m
									.getDelta(currLocalTopRevisions.get(m
											.getLocation()));

							// System.err.println(files);

							HashSet<VcsCommitItem> interestingfiles = new HashSet<VcsCommitItem>();

							for (VcsCommitItem r : files) {
								for (String p : getKnownModelParserTypes()) {
									IModelResourceFactory parser = getModelParser(p);
									for (String ext : parser
											.getModelExtensions()) {
										if (r.getPath().toLowerCase()
												.endsWith(ext)) {
											interestingfiles.add(r);
										}
									}
								}
							}

							Iterator<VcsCommitItem> it = interestingfiles
									.iterator();

							while (it.hasNext()) {
								VcsCommitItem c = it.next();

								if (c.getChangeType().equals(
										VcsChangeType.DELETED)) {

									if (VERBOSE)
										console.println("-->" + c.getPath()
												+ " HAS CHANGED ("
												+ c.getChangeType()
												+ "), PROPAGATING CHANGES");

									deleteditems.add(c);
									it.remove();
								}
							}

							// metadata about synchronise
							deletedFiles = deletedFiles + deleteditems.size();
							interestingFiles = interestingFiles
									+ interestingfiles.size();

							// for each registered updater
							for (IModelUpdater u : getUpdaters()) {

								// enters transaction mode!
								Set<VcsCommitItem> currreposchangeditems = u
										.compareWithLocalFiles(interestingfiles);

								// metadata about synchronise
								currchangeditems = currchangeditems
										+ currreposchangeditems.size();

								// create temp files with changed repos files
								for (VcsCommitItem s : currreposchangeditems) {

									String commitPath = s.getPath();

									if (VERBOSE)
										console.println("-->" + commitPath
												+ " HAS CHANGED ("
												+ s.getChangeType()
												+ "), PROPAGATING CHANGES");

									String[] commitPathSplit = commitPath
											.split("/");

									if (commitPathSplit.length > 1) {
										String path = monitorTempDir;
										for (int ii = 0; ii < commitPathSplit.length - 1; ii++) {

											File dir = new File(path + "/"
													+ commitPathSplit[ii]);
											dir.mkdir();
											path = path + "/"
													+ commitPathSplit[ii];

										}
										temp = new File(
												path
														+ "/"
														+ commitPathSplit[commitPathSplit.length - 1]);
									} else
										temp = new File(monitorTempDir + "/"
												+ commitPath);

									if (!temp.exists())
										m.importFiles(commitPath, temp);

								}

								// delete all removed files

								try {
									listener.changeStart();
									for (VcsCommitItem c : deleteditems) {
										success = success && u.deleteAll(c);
									}
								} catch (Exception e) {
									success = false;
									System.err
											.println("error in deleting removed files from store:");
									e.printStackTrace();
									listener.changeFailure();
								} finally {
									listener.changeSuccess();
								}

								// prepare for mass inserts if needed
								graph.enterBatchMode();

								for (VcsCommitItem v : currreposchangeditems) {
									// if(v.getPath().equals("W4 BPMN+ Composer"
									// +" V.9.0/B.2.0-roundtrip.bpmn")){
									// if
									// (v.getPath().equals("A - Fixed Digrams"
									// +" with Variations of Attributes/eclipse"
									// +" BPMN2 Modeler 0.2.6/A.3.0-export.bpmn"))
									// {
									try {
										IHawkModelResource r = null;

										if (u.caresAboutResources()) {

											File file = new File(
													graph.getTempDir() + "/"
															+ v.getPath());

											if (!file.exists()) {
												console.printerrln("warning, cannot find file: "
														+ file
														+ ", ignoring changes");
											} else {
												r = getModelParserFromFilename(
														file.getName()
																.toLowerCase())
														.parse(file);
											}

										}

										success = u.updateStore(v, r)
												&& success;

										if (r != null) {
											if (!isSyncMetricsEnabled)
												r.unload();
											else {
												fileToResourceMap.put(v, r);

											}
											loadedResources++;
										}

									} catch (Exception e) {
										console.printerrln("updater: " + u
												+ "failed to update store");
										console.printerrln(e);
										success = false;
									}
								}
								// }

								// update proxies
								u.updateProxies();

							}

							// delete temporary files
							if (!FileOperations.deleteFiles(new File(
									monitorTempDir), true))
								console.printerrln("error in deleting temporary local vcs files");

							if (success) {
								currLocalTopRevisions.put(m.getLocation(),
										currReposTopRevisions.get(m
												.getLocation()));
							} else {
								allSync = false;
								currLocalTopRevisions
										.put(m.getLocation(), "-3");
							}
						}

					} else {
						console.printerrln("Warning, monitor is inactive, synchronisation failed!");
						allSync = false;
					}
				}
			}

			synctime = (System.currentTimeMillis() - start);

			return allSync;
		} finally {
			listener.synchroniseEnd();
			stateListener.state(HawkState.RUNNING);
		}
	}

	private List<IModelUpdater> getUpdaters() {
		return updaters;
	}

	@Override
	public void shutdown(ShutdownRequestType type) throws Exception {
		// The type is ignored for local instances of Hawk: they
		// must always be safely shut down, to preserve consistency.
		stateListener.info("Hawk shutting down...");
		preShutdown();

		if (graph != null) {
			graph.shutdown();
			credStore.shutdown();
		}
		graph = null;
		stateListener.info("Hawk shut down.");
	}

	@Override
	public void delete() throws Exception {
		preShutdown();

		if (graph != null) {
			graph.delete();
		}
		graph = null;
	}

	private void preShutdown() {
		try {
			saveIndexer();
		} catch (Exception e) {
			System.err.println("shutdown tried to saveIndexer but failed");
			e.printStackTrace();
		}

		updateTimer.cancel();

		for (IVcsManager monitor : monitors)
			monitor.shutdown();
		monitors = new ArrayList<>();
		running = false;
		stateListener.state(HawkState.STOPPED);
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

	@Override
	public String getId() {

		return "ModelIndexer | " + " | " + this;

	}

	public void logFullStore() throws Exception {

		graph.logFull();

	}

	private void registerMetamodelFiles() throws Exception {

		stateListener.info("Registering metamodels...");

		String s = null;
		String ep = null;
		String type = null;

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IGraphNode epackage : graph.getMetamodelIndex().query("id",
					"*")) {

				s = epackage.getProperty(
						IModelIndexer.METAMODEL_RESOURCE_PROPERTY).toString();
				ep = epackage.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.toString();
				type = epackage.getProperty(
						IModelIndexer.METAMODEL_TYPE_PROPERTY).toString();

				IMetaModelResourceFactory p = getMetaModelParser(type);

				if (p != null)
					// registeredMetamodels.put(p.getType(),
					p.parseFromString("resource_from_epackage_" + ep, s)
					// )
					;
				else
					console.printerrln("cannot register metamodel in graph, named: "
							+ ep
							+ ", with type: "
							+ type
							+ ", as no relevant parser is registered");

			}

			t.success();
		}
		stateListener.info("Registered metamodels.");
	}

	// @Override
	// public void removeMetamodel(File metamodel) throws Exception {
	//
	// File[] mm = { metamodel };
	//
	// removeMetamodel(mm);
	//
	// }

	@Override
	/**
	 * removes metamodel with uri mm and all containing models, keeping
	 * cross-file references as proxies
	 */
	public void removeMetamodels(String[] mm) throws Exception {
		stateListener.info("Removing metamodels...");
		for (String s : metamodelupdater.removeMetamodels(this, mm))
			resetRepositoy(s);
		stateListener.info("Removed metamodels.");
	}

	private void resetRepositoy(String s) {
		System.err
				.println("reseting local top revision of repository: "
						+ s
						+ "\n(as elements in it were removed or new metamodels were added to Hawk)");
		currLocalTopRevisions.put(s, "-3");
	}

	@Override
	public void registerMetamodel(File f) throws Exception {

		File[] ret = { f };

		registerMetamodel(ret);

	}

	@Override
	public void registerMetamodel(File[] f) throws Exception {
		stateListener.info("Adding metamodel(s)...");
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

				parser = getMetaModelParserFromFilename(mm.getPath());

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
						break;
					} else {
						if (metamodelResource != null) {
							System.out.println("Adding metamodels in: " + mm
									+ " to store");
							set.add(metamodelResource);
							previousParserType = parserType;
						}
					}
				}

			}// if metamodels added successfully
			if (metamodelupdater.insertMetamodels(set, this)) {
				// reset repositories as models may be parsable in them
				for (IVcsManager s : monitors) {
					resetRepositoy(s.getLocation());
				}
			}
		}
		stateListener.info("Added metamodel(s).");
	}

	@Override
	public IConsole getConsole() {
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

	public IMetaModelResourceFactory getMetaModelParserFromFilename(String name) {
		if (name == null)
			console.printerrln("null extension given to getMetaModelParserFromFilename(extension), returning null");
		else
			for (String p : metamodelparsers.keySet()) {
				IMetaModelResourceFactory parser = metamodelparsers.get(p);
				for (String ext : parser.getMetaModelExtensions()) {
					if (name.endsWith(ext)) {
						return parser;
					}
				}
			}
		return null;
	}

	public IModelResourceFactory getModelParserFromFilename(String name) {
		if (name == null)
			console.printerrln("null extension given to getModelParserFromFilename(extension), returning null");
		else
			for (String p : modelparsers.keySet()) {
				IModelResourceFactory parser = modelparsers.get(p);
				for (String ext : parser.getModelExtensions()) {
					if (name.endsWith(ext)) {
						return parser;
					}
				}
			}
		return null;
	}

	public Map<String, IQueryEngine> getKnownQueryLanguages() {
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
					.println("metamodel updater alredy registered, cannot have more than one");
		}

	}

	public void saveIndexer() throws Exception {
		stateListener.info("Saving Hawk metadata...");
		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);

		HashSet<String[]> set = new HashSet<String[]>();
		for (IVcsManager s : getRunningVCSManagers()) {
			String[] meta = new String[] { s.getLocation(), s.getType() };
			System.out.println("adding: " + meta[0] + ":" + meta[1]);
			set.add(meta);
		}
		HawkProperties hp = new HawkProperties(graph.getType(), set, minDelay,
				maxDelay);

		String out = stream.toXML(hp);
		try (BufferedWriter b = new BufferedWriter(new FileWriter(
				getParentFolder() + File.separator + "properties.xml"))) {
			b.write(out);
			b.flush();
		}
		stateListener.info("Saved Hawk metadata.");
	}

	@Override
	public void addVCSManager(IVcsManager vcs, boolean persist) {
		monitors.add(vcs);
		currLocalTopRevisions.put(vcs.getLocation(), "-3");
		currReposTopRevisions.put(vcs.getLocation(), "-4");

		try {
			if (persist)
				saveIndexer();
		} catch (Exception e) {
			System.err.println("addVCSManager tried to saveIndexer but failed");
			e.printStackTrace();
		}

		requestImmediateSync();
	}

	@Override
	public void setDB(IGraphDatabase db, boolean persist) {
		graph = db;
		try {
			if (persist)
				saveIndexer();
		} catch (Exception e) {
			System.err.println("setDB tried to saveIndexer but failed");
			e.printStackTrace();
		}
	}

	@Override
	public void init(int minDelay, int maxDelay) throws Exception {
		stateListener.info("Initializing Hawk...");
		this.maxDelay = maxDelay;
		this.minDelay = minDelay;
		this.currentDelay = minDelay;

		// register all static metamodels to graph
		System.out
				.println("inserting static metamodels of registered metamodel factories to graph:");
		for (String factoryNames : metamodelparsers.keySet()) {
			IMetaModelResourceFactory f = metamodelparsers.get(factoryNames);
			System.out.println(f.getType());
			metamodelupdater.insertMetamodels(f.getStaticMetamodels(), this);
		}
		System.out.println("inserting static metamodels complete");

		// register all metamodels in graph to their factories
		registerMetamodelFiles();

		// begin scheduled updates from vcs
		updateTimer = new Timer("t", false);
		updateTimer.schedule(new RunUpdateTask(), 0);

		running = true;
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Initialized Hawk.");
	}

	private void runUpdateTask() {
		stateListener.info("Updating Hawk...");
		long start = System.currentTimeMillis();

		boolean synchronised = true;
		console.println("updating indexer: ");

		try {
			synchronised = internalSynchronise();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!synchronised) {
			console.println("SYNCHRONISATION ERROR");
			stateListener.error("Update FAILED!");
		}

		// Timer only enabled if the delay is non-zero
		if (maxDelay > 0) {
			if (!latestUpdateFoundChanges) {
				int olddelay = currentDelay;
				currentDelay = currentDelay * 2;
				if (currentDelay > maxDelay)
					currentDelay = maxDelay;
				console.println("same revision, incrementing check timer: "
						+ olddelay / 1000 + " -> " + currentDelay / 1000
						+ " (max: " + maxDelay / 1000 + ")");

			} else {

				// t.stop();
				console.println("different revisions, resetting check timer and propagating changes!");
				currentDelay = minDelay;

			}

			updateTimer.schedule(new RunUpdateTask(), currentDelay);
		}

		final long time = (System.currentTimeMillis() - start);
		stateListener.info("Updated Hawk "
				+ (synchronised ? "(success)." : "(failure).") + " " + time
				/ 1000 + "s" + time % 1000 + "ms");
	}

	@Override
	public File getParentFolder() {
		return parentfolder;
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
	public Collection<String> getDerivedAttributes() {
		return getExtraAttributes(IS_DERIVED);
	}

	@Override
	public Collection<String> getIndexedAttributes() {
		return getExtraAttributes(IS_INDEXED);
	}

	/**
	 * Returns {@link #IS_DERIVED} if the attribute is derived or
	 * {@link #IS_INDEXED} if not.
	 */
	private boolean isDerivedAttribute(IGraphNode typenode,
			final String attrName) {
		return ((String[]) typenode.getProperty(attrName))[0].equals("d");
	}

	/**
	 * Lists all the Hawk-specific attributes available. If
	 * <code>isDerived</code> is {@link #IS_DERIVED}, it will list all the
	 * derived attributes. If it is {@link #IS_INDEXED}, it will list all the
	 * indexed attributes.
	 */
	private Collection<String> getExtraAttributes(final boolean isDerived) {
		Set<String> ret = new HashSet<String>();

		try (IGraphTransaction t = graph.beginTransaction()) {
			ret.addAll(graph.getNodeIndexNames());

			Iterator<String> it = ret.iterator();
			while (it.hasNext()) {
				String s = it.next();
				if (!s.matches("(.*)##(.*)##(.*)"))
					it.remove();
			}

			it = ret.iterator();
			while (it.hasNext()) {
				String s = it.next();
				String[] split = s.split("##");
				final String mmURI = split[0];

				IGraphNode epackagenode = graph.getMetamodelIndex()
						.get("id", mmURI).iterator().next();

				IGraphNode typenode = null;
				for (IGraphEdge e : epackagenode
						.getIncomingWithType("epackage")) {
					IGraphNode temp = e.getStartNode();
					final String typeName = split[1];
					if (temp.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
							.equals(typeName)) {
						if (typenode == null) {
							typenode = temp;
						} else {
							System.err
									.println("error in getExtraAttributes, typenode had more than 1 type found");
						}
					}
				}

				final String attrName = split[2];
				if (isDerivedAttribute(typenode, attrName) != isDerived) {
					it.remove();
				}
			}

			t.success();
		} catch (Exception e) {
			System.err.println("error in getExtraAttributes");
			System.err.println(e.getCause());
		}
		return ret;
	}

	@Override
	public Collection<String> getIndexes() {

		HashSet<String> ret = new HashSet<String>();

		try (IGraphTransaction t = graph.beginTransaction()) {

			ret.addAll(graph.getNodeIndexNames());

			t.success();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;

	}

	@Override
	public List<String> validateExpression(String derivationlanguage,
			String derivationlogic) {

		IQueryEngine q = knownQueryLanguages.get(derivationlanguage);

		return q.validate(derivationlogic);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean addGraphChangeListener(IGraphChangeListener changeListener) {
		return listener.add(changeListener);
	}

	@Override
	public boolean removeGraphChangeListener(IGraphChangeListener changeListener) {
		return listener.remove(changeListener);
	}

	@Override
	public IGraphChangeListener getCompositeGraphChangeListener() {
		return listener;
	}

	@Override
	public void setSyncMetricsEnabled(Boolean enable) {
		isSyncMetricsEnabled = enable;
	}

	public Map<VcsCommitItem, IHawkModelResource> getFileToResourceMap() {
		if (!isSyncMetricsEnabled)
			System.err
					.println("WARNING: isSyncMetricsEnabled == false, this method will return an empty Map");
		return fileToResourceMap;
	}

	public int getDeletedFiles() {
		// works even if metrics disabled
		return deletedFiles;
	}

	public int getInterestingFiles() {
		// works even if metrics disabled
		return interestingFiles;
	}

	public int getCurrChangedItems() {
		// works even if metrics disabled
		return currchangeditems;
	}

	public int getLoadedResources() {
		// works even if metrics disabled
		return loadedResources;
	}

	public long getLatestSynctime() {
		// works even if metrics disabled
		return synctime;
	}

	@Override
	public ICredentialsStore getCredentialsStore() {
		return credStore;
	}

	@Override
	public String getDerivedAttributeExecutionEngine() {
		// TODO we should honor the derivation language info that we already ask the user for
		if (getKnownQueryLanguages().keySet().contains(
				"org.hawk.epsilon.emc.EOLQueryEngine"))
			return "org.hawk.epsilon.emc.EOLQueryEngine";
		else
			return "error: default derived attribute engine (EOLQueryEngine) not added to Hawk. Derived attributes cannot be calculated!";
	}

	@Override
	public boolean addStateListener(IStateListener listener) {
		final boolean ret = stateListener.add(listener);
		return ret;
	}

	@Override
	public boolean removeStateListener(IStateListener listener) {
		return stateListener.remove(listener);
	}

	@Override
	public IStateListener getCompositeStateListener() {
		return stateListener;
	}
}
