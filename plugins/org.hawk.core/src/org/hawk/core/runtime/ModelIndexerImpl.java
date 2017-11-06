/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - extract import to interface
 ******************************************************************************/
package org.hawk.core.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
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
import org.hawk.core.IFileImporter;
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
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.FileOperations;
import org.hawk.core.util.HawkProperties;
import org.hawk.core.util.IndexedAttributeParameters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ModelIndexerImpl implements IModelIndexer {

	public static class DefaultFileImporter implements IFileImporter {

		private Map<String, File> cachedImports = new HashMap<>();
		private IVcsManager vcs;
		private File tempDir;

		public DefaultFileImporter(IVcsManager vcs, File tempDir) {
			this.vcs = vcs;
			this.tempDir = tempDir;
		}

		@Override
		public File importFile(String commitPath) {
			/*
			 * We cache the results as some files may get imported repeatedly
			 * (e.g. mmversion.dat for Modelio).
			 */
			if (!cachedImports.containsKey(commitPath)) {
				String[] commitPathSplit = commitPath.split("/");

				// Resolves the commit path as a relative path from the
				// temporary directory
				File destination = tempDir;
				if (commitPathSplit.length > 1) {
					for (String pathComponent : commitPathSplit) {
						destination = new File(destination, pathComponent);
					}
					destination.getParentFile().mkdirs();
				} else {
					destination = new File(destination, commitPath);
				}

				File result = vcs.importFiles(commitPath, destination);
				cachedImports.put(commitPath, result);
				return result;
			} else {
				return cachedImports.get(commitPath);
			}
		}
	}

	private static final int FILECOUNT_PROGRESS_THRESHOLD = 100;

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
	public static final int DEFAULT_MINDELAY = 5000;
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
	public ModelIndexerImpl(String name, File parentfolder, ICredentialsStore credStore, IConsole c) throws Exception {
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
					if (!m.isFrozen()) {
						if (m.isActive()) {
							allSync = internalSynchronise(allSync, m);
						} else {
							console.printerrln("Warning, monitor is inactive, synchronisation failed!");
							allSync = false;
						}
					} else {
						console.printerrln("Monitor is frozen, skipping it.");
						// frozen do nothing

					}
				}
			}

			synctime = (System.currentTimeMillis() - start);

			return allSync;
		} finally {
			stateListener.info("Performing optional post-sync operations.");
			stateListener.state(HawkState.RUNNING);
			listener.synchroniseEnd();
		}
	}

	private boolean internalSynchronise(boolean allSync, IVcsManager m) throws Exception {
		try {
			currReposTopRevisions.put(m.getLocation(), m.getCurrentRevision());
		} catch (Exception e1) {
			// if
			// (e1.getMessage().contains("Authentication"))
			// {syserr(Eclipse_VCS_NoSQL_UI_view.indexers.remove(this)+"");
			// syserr("Incorrect authentication to version
			// control, removing this indexer, please add it
			// again with the correct credentials");}
			// else
			e1.printStackTrace();
			allSync = false;
		}

		if (!currReposTopRevisions.get(m.getLocation()).equals(currLocalTopRevisions.get(m.getLocation()))) {

			boolean success = true;
			latestUpdateFoundChanges = true;

			final Set<VcsCommitItem> deleteditems = new HashSet<VcsCommitItem>();
			final Set<VcsCommitItem> interestingfiles = new HashSet<VcsCommitItem>();
			inspectChanges(m, deleteditems, interestingfiles);
			deletedFiles = deletedFiles + deleteditems.size();
			interestingFiles = interestingFiles + interestingfiles.size();

			final String monitorTempDir = graph.getTempDir();
			File temp = new File(monitorTempDir);
			temp.mkdir();
			
			// for each registered updater
			for (IModelUpdater u : getUpdaters()) {
				success = internalSynchronise(success, m, u, deleteditems, interestingfiles, monitorTempDir);
			}

			// delete temporary files
			if (!FileOperations.deleteFiles(new File(monitorTempDir), true))
				console.printerrln("error in deleting temporary local vcs files");

			if (success) {
				currLocalTopRevisions.put(m.getLocation(),
						currReposTopRevisions.get(m.getLocation()));
			} else {
				allSync = false;
				currLocalTopRevisions.put(m.getLocation(), "-3");
			}
		}

		return allSync;
	}

	private boolean internalSynchronise(boolean success, final IVcsManager m, final IModelUpdater u,
			final Set<VcsCommitItem> deletedItems, final Set<VcsCommitItem> interestingfiles,
			final String monitorTempDir) {

		// enters transaction mode!
		Set<VcsCommitItem> currReposChangedItems = u.compareWithLocalFiles(interestingfiles);

		// metadata about synchronise
		final int totalFiles = currReposChangedItems.size();
		currchangeditems = currchangeditems + totalFiles;

		// create temp files with changed repos files
		final Map<String, File> pathToImported = new HashMap<>();
		final IFileImporter importer = new DefaultFileImporter(m, new File(monitorTempDir));
		importFiles(importer, currReposChangedItems, pathToImported);

		// delete all removed files
		success = deleteRemovedModels(success, u, deletedItems);

		stateListener.info("Updating models to the new version...");

		// prepare for mass inserts if needed
		graph.enterBatchMode();

		final boolean fileCountProgress = totalFiles > FILECOUNT_PROGRESS_THRESHOLD;
		final long millisSinceStart = System.currentTimeMillis();
		int totalProcessedFiles = 0, filesProcessedSinceLastPrint = 0;
		long millisSinceLastPrint = millisSinceStart;

		for (VcsCommitItem v : currReposChangedItems) {
			try {
				// Place before the actual update so we print the 0/X message as well
				if (fileCountProgress && (totalProcessedFiles == 0 && filesProcessedSinceLastPrint == 0 || filesProcessedSinceLastPrint == FILECOUNT_PROGRESS_THRESHOLD)) {
					totalProcessedFiles += filesProcessedSinceLastPrint;

					final long millisPrint = System.currentTimeMillis();
					stateListener.info(String.format(
							"Processed %d/%d files in repo %s (%s sec, %s sec total)",
							totalProcessedFiles, totalFiles, m.getLocation(),
							(millisPrint - millisSinceLastPrint) / 1000,
							(millisPrint - millisSinceStart) / 1000));

					filesProcessedSinceLastPrint = 0;
					millisSinceLastPrint = millisPrint;
				}

				IHawkModelResource r = null;
				if (u.caresAboutResources()) {
					final File file = pathToImported.get(v.getPath());
					if (file == null || !file.exists()) {
						console.printerrln("warning, cannot find file: " + file + ", ignoring changes");
					} else {
						IModelResourceFactory mrf = getModelParserFromFilename(
								file.getName().toLowerCase());
						if (mrf.canParse(file))
							r = mrf.parse(importer, file);
					}
				}
				success = u.updateStore(v, r) && success;

				if (r != null) {
					if (!isSyncMetricsEnabled) {
						r.unload();
					} else {
						fileToResourceMap.put(v, r);
					}
					loadedResources++;
				}

				filesProcessedSinceLastPrint++;

			} catch (Exception e) {
				console.printerrln("updater: " + u + "failed to update store");
				console.printerrln(e);
				success = false;
			}
		}

		// Print the final message
		if (fileCountProgress) {
			totalProcessedFiles += filesProcessedSinceLastPrint;
			final long millisPrint = System.currentTimeMillis();
			stateListener.info(String.format(
					"Processed %d/%d files in repo %s (%s sec, %s sec total)",
					totalProcessedFiles, totalFiles, m.getLocation(),
					(millisPrint - millisSinceLastPrint) / 1000,
					(millisPrint - millisSinceStart) / 1000));
		}

		stateListener.info("Updating proxies...");

		// update proxies
		u.updateProxies();

		// leave batch mode
		graph.exitBatchMode();
		
		stateListener.info("Updated proxies.");
		return success;
	}

	private boolean deleteRemovedModels(boolean success, IModelUpdater u, final Set<VcsCommitItem> deleteditems) {
		stateListener.info("Deleting models removed from repository...");
		try {
			listener.changeStart();
			for (VcsCommitItem c : deleteditems) {
				success = success && u.deleteAll(c);
			}
		} catch (Exception e) {
			success = false;
			System.err.println("error in deleting removed files from store:");
			e.printStackTrace();
			listener.changeFailure();
		} finally {
			listener.changeSuccess();
		}
		return success;
	}

	private void importFiles(IFileImporter importer, final Set<VcsCommitItem> changedItems, final Map<String, File> pathToImported) {
		for (VcsCommitItem s : changedItems) {
			final String commitPath = s.getPath();

			if (VERBOSE) {
				console.println("-->" + commitPath + " HAS CHANGED (" + s.getChangeType()
						+ "), PROPAGATING CHANGES");
			}

			if (!pathToImported.containsKey(commitPath)) {
				final File imported = importer.importFile(commitPath);
				pathToImported.put(commitPath, imported);
			}
		}
	}

	private void inspectChanges(IVcsManager m, Set<VcsCommitItem> deleteditems, Set<VcsCommitItem> interestingfiles)
			throws Exception {
		Collection<VcsCommitItem> files = m.getDelta(currLocalTopRevisions.get(m.getLocation()));

		stateListener.info("Calculating relevant changed model files...");

		for (VcsCommitItem r : files) {
			for (String p : getKnownModelParserTypes()) {
				IModelResourceFactory parser = getModelParser(p);
				for (String ext : parser.getModelExtensions()) {
					if (r.getPath().toLowerCase().endsWith(ext)) {
						interestingfiles.add(r);
					}
				}
			}
		}

		Iterator<VcsCommitItem> it = interestingfiles.iterator();

		while (it.hasNext()) {
			VcsCommitItem c = it.next();

			if (c.getChangeType().equals(VcsChangeType.DELETED)) {

				if (VERBOSE)
					console.println("-->" + c.getPath() + " HAS CHANGED (" + c.getChangeType()
							+ "), PROPAGATING CHANGES");

				deleteditems.add(c);
				it.remove();
			}
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
			graph = null;
		}
		if (credStore != null) {
			credStore.shutdown();
		}
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

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IGraphNode epackage : graph.getMetamodelIndex().query("id", "*")) {
				final String s = epackage.getProperty(IModelIndexer.METAMODEL_RESOURCE_PROPERTY) + "";
				final String ep = epackage.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "";
				final String type = epackage.getProperty(IModelIndexer.METAMODEL_TYPE_PROPERTY) + "";

				final IMetaModelResourceFactory p = getMetaModelParser(type);
				if (p != null) {
					p.parseFromString("resource_from_epackage_" + ep, s);
				} else {
					console.printerrln("cannot register metamodel in graph, named: " + ep + ", with type: " + type
							+ ", as no relevant parser is registered");
				}

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
	public void removeMetamodels(String... mm) throws Exception {
		stateListener.state(HawkState.UPDATING);
		stateListener.info("Removing metamodels...");
		for (String s : metamodelupdater.removeMetamodels(this, mm))
			resetRepositoy(s);
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Removed metamodels.");
	}

	@Override
	/**
	 * Removes the IVcsManager from Hawk, including any contained models
	 * indexed.
	 */
	public void removeVCS(IVcsManager vcs) throws Exception {
		stateListener.state(HawkState.UPDATING);
		stateListener.info("Removing vcs...");
		//

		monitors.remove(vcs);
		currLocalTopRevisions.remove(vcs.getLocation());
		currReposTopRevisions.remove(vcs.getLocation());

		try {
			saveIndexer();
		} catch (Exception e) {
			System.err.println("removeVCS tried to saveIndexer but failed");
			e.printStackTrace();
		}

		for (IModelUpdater u : updaters)
			try {
				u.deleteAll(vcs);
			} catch (Exception e) {
				System.err.println("removeVCS tried to delete index contents but failed");
				e.printStackTrace();
			}

		//
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Removed vcs.");
	}

	private void resetRepositoy(String s) {
		System.err.println("reseting local top revision of repository: " + s
				+ "\n(as elements in it were removed or new metamodels were added to Hawk)");
		currLocalTopRevisions.put(s, "-3");
	}

	@Override
	public void registerMetamodels(File... f) throws Exception {
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
			String parent = genericWorkspaceFile.getAbsolutePath().replaceAll("\\\\", "/");

			// change to workspace directory or a generic one on release
			filechoser.setCurrentDirectory(
					new File(new File(parent).getParentFile().getAbsolutePath().replaceAll("\\\\", "/")
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

					if (previousParserType != null && !previousParserType.equals(parserType)) {
						console.printerrln(
								"cannot add heterogeneous metamodels concurrently, plase add one metamodel type at a time");
						set.clear();
						break;
					} else {
						if (metamodelResource != null) {
							console.println("Adding metamodels in: " + mm + " to store");
							set.add(metamodelResource);
							previousParserType = parserType;
						}
					}
				}

			} // if metamodels added successfully
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
			System.err.println("metamodel updater alredy registered, cannot have more than one");
		}

	}

	public void saveIndexer() throws Exception {
		stateListener.info("Saving Hawk metadata...");
		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);

		HashSet<String[]> set = new HashSet<String[]>();
		for (IVcsManager s : getRunningVCSManagers()) {
			String[] meta = new String[] { s.getLocation(), s.getType(), s.isFrozen() + "" };
			console.println("adding: " + meta[0] + ":" + meta[1] + ":" + meta[2]);
			set.add(meta);
		}
		HawkProperties hp = new HawkProperties(graph.getType(), set, minDelay, maxDelay);

		Files.createDirectories(getParentFolder().toPath());
		String out = stream.toXML(hp);
		try (BufferedWriter b = new BufferedWriter(
				new FileWriter(getParentFolder() + File.separator + "properties.xml"))) {
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
		console.println("inserting static metamodels of registered metamodel factories to graph:");
		for (String factoryNames : metamodelparsers.keySet()) {
			IMetaModelResourceFactory f = metamodelparsers.get(factoryNames);
			console.println(f.getType());
			metamodelupdater.insertMetamodels(f.getStaticMetamodels(), this);
		}
		console.println("inserting static metamodels complete");

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
				console.println("same revision, incrementing check timer: " + olddelay / 1000 + " -> "
						+ currentDelay / 1000 + " (max: " + maxDelay / 1000 + ")");

			} else {

				// t.stop();
				console.println("different revisions, resetting check timer and propagating changes!");
				currentDelay = minDelay;

			}

			updateTimer.schedule(new RunUpdateTask(), currentDelay);
		}

		final long time = (System.currentTimeMillis() - start);
		stateListener.info("Updated Hawk instance " + getName() + " " + (synchronised ? "(success)." : "(failure).") + " " + time / 1000 + "s"
				+ time % 1000 + "ms");
	}

	@Override
	public File getParentFolder() {
		return parentfolder;
	}

	@Override
	public void addDerivedAttribute(String metamodeluri, String typename, String attributename, String attributetype,
			boolean isMany, boolean isOrdered, boolean isUnique, String derivationlanguage, String derivationlogic) {

		stateListener
				.info(String.format("Adding derived attribute %s::%s::%s...", metamodeluri, typename, attributename));

		final boolean success = metamodelupdater.addDerivedAttribute(metamodeluri, typename, attributename, attributetype, isMany,
				isOrdered, isUnique, derivationlanguage, derivationlogic, this);
		if (success) {
			for (IModelUpdater u : getUpdaters())
				u.updateDerivedAttribute(metamodeluri, typename, attributename, attributetype, isMany, isOrdered,
						isUnique, derivationlanguage, derivationlogic);
		}

		cachedDerivedAttributes = null;
		stateListener.info(String.format("Added derived attribute %s::%s::%s.", metamodeluri, typename, attributename));
	}

	/**
	 * We cache the list of derived attributes to avoid switching back and forth between
	 * batch and transactional mode when inserting large numbers of files (since we check
	 * if there are any derived attrs before we insert the derived attribute listener).
	 *
	 * Every method that changes the derived attributes in some form should set this to
	 * null, so it is recomputed in the next call to {@link #getDerivedAttributes()}.
	 */
	private Collection<String> cachedDerivedAttributeNames = null;
	private Collection<IndexedAttributeParameters> cachedDerivedAttributes = null;

	@Override
	public Collection<String> getDerivedAttributeNames() {
		if (cachedDerivedAttributeNames == null) {
			cachedDerivedAttributeNames = getExtraAttributeNames(IS_DERIVED);
		}
		return cachedDerivedAttributeNames;
	}

	@Override
	public Collection<IndexedAttributeParameters> getDerivedAttributes() {
		if (cachedDerivedAttributes == null) {
			cachedDerivedAttributes  = getExtraAttributes(IS_DERIVED);
		}
		return cachedDerivedAttributes;
	}

	@Override
	public boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName) {
		stateListener
				.info(String.format("Removing derived attribute %s::%s::%s...", metamodelUri, typeName, attributeName));

		final boolean removed = metamodelupdater.removeDerivedAttribute(metamodelUri, typeName, attributeName, this);
		cachedDerivedAttributes = null;

		stateListener.info(
				removed ? String.format("Removed derived attribute %s::%s::%s.", metamodelUri, typeName, attributeName)
						: String.format("Derived attribute %s::%s::%s did not exist so nothing happened.", metamodelUri,
								typeName, attributeName));

		return removed;
	}

	@Override
	public void addIndexedAttribute(String metamodeluri, String typename, String attributename) {

		stateListener
				.info(String.format("Adding indexed attribute %s::%s::%s...", metamodeluri, typename, attributename));

		if (metamodelupdater.addIndexedAttribute(metamodeluri, typename, attributename, this))
			for (IModelUpdater u : getUpdaters())
				u.updateIndexedAttribute(metamodeluri, typename, attributename);

		stateListener.info(String.format("Added indexed attribute %s::%s::%s.", metamodeluri, typename, attributename));
	}

	@Override
	public Collection<String> getIndexedAttributeNames() {
		return getExtraAttributeNames(IS_INDEXED);
	}

	@Override
	public Collection<IndexedAttributeParameters> getIndexedAttributes() {
		return  getExtraAttributes(IS_INDEXED);
	}

	@Override
	public boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename) {
	
		stateListener
				.info(String.format("Removing indexed attribute %s::%s::%s...", metamodelUri, typename, attributename));
	
		boolean removed = metamodelupdater.removeIndexedAttribute(metamodelUri, typename, attributename, this);
	
		stateListener.info(
				removed ? String.format("Removed indexed attribute %s::%s::%s.", metamodelUri, typename, attributename)
						: String.format("Indexed attribute %s::%s::%s did not exist so nothing happened.", metamodelUri,
								typename, attributename));
	
		return removed;
	
	}

	/**
	 * Returns {@link #IS_DERIVED} if the attribute is derived or
	 * {@link #IS_INDEXED} if not.
	 */
	private boolean isDerivedAttribute(IGraphNode typenode, final String attrName) {
		return ((String[]) typenode.getProperty(attrName))[0].equals("d");
	}

	/**
	 * Lists all the Hawk-specific attributes available. If
	 * <code>isDerived</code> is {@link #IS_DERIVED}, it will list all the
	 * derived attributes. If it is {@link #IS_INDEXED}, it will list all the
	 * indexed attributes.
	 */
	private Collection<String> getExtraAttributeNames(final boolean isDerived) {
		Set<String> ret = new HashSet<String>();

		try (IGraphTransaction t = graph.beginTransaction()) {
			ret = getAttributeIndexNames();
			
			Iterator<String> it = ret.iterator();
			while (it.hasNext()) {

				String s = it.next();
				String[] split = s.split("##"); 

				final String mmURI = split[0];
				final String typeName = split[1];
				final String attrName = split[2];

				IGraphNode typenode = getTypeNode(mmURI, typeName);

				if (isDerivedAttribute(typenode, attrName) != isDerived) {
					it.remove();
				}
			}

			t.success();
		} catch (Exception e) {
			System.err.println("error in getExtraAttributeNames");
			e.printStackTrace();
		}
		return ret;
	}
	


	private Collection<IndexedAttributeParameters> getExtraAttributes(final boolean isDerived) {

		Set<IndexedAttributeParameters> paramsSet = new HashSet<IndexedAttributeParameters>();

		try (IGraphTransaction t = graph.beginTransaction()) {

			Set<String> ret = getAttributeIndexNames();

			Iterator<String> it = ret.iterator();
			while (it.hasNext()) {

				String s = it.next();
				String[] split = s.split("##"); 
				
				final String mmURI = split[0];
				final String typeName = split[1];
				final String attrName = split[2];
				
				IGraphNode typenode = getTypeNode(mmURI, typeName);

				if (isDerivedAttribute(typenode, attrName) == isDerived) {
					String[] metadata = (String[]) typenode.getProperty(attrName);
					IndexedAttributeParameters params = getAttributeParametersfromMetadata(mmURI, typeName ,attrName, metadata);
					paramsSet.add(params);
				}
			}

			t.success();
		} catch (Exception e) {
			System.err.println("error in getExtraAttributes");
			e.printStackTrace();
		}
		return paramsSet;
	}

	private IGraphNode getTypeNode(String mmURI, String typeName) {
		
		IGraphNode epackagenode = graph.getMetamodelIndex().get("id", mmURI).iterator().next();
		IGraphNode typenode = null;

		for (IGraphEdge e : epackagenode.getIncomingWithType("epackage")) {
			IGraphNode temp = e.getStartNode();
			if (temp.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typeName)) {
				if (typenode == null) {
					typenode = temp;
				} else {
					System.err.println("error in getExtraAttributes, typenode had more than 1 type found");
				}
			}
		}

		return typenode;
	}

	private Set<String> getAttributeIndexNames() {
		Set<String> ret = new HashSet<String>();

		ret.addAll(graph.getNodeIndexNames());
		Iterator<String> it = ret.iterator();
		while (it.hasNext()) {
			String s = it.next();
			if (!s.matches("(.*)##(.*)##(.*)"))
				it.remove();
		}
		
		return ret;
	}

	private IndexedAttributeParameters getAttributeParametersfromMetadata(String mmURI, String typeName, String attrName, String[] metadata) {
		IndexedAttributeParameters params;

		if (metadata[0].equals("d")) {
			params = new DerivedAttributeParameters(mmURI, typeName,  attrName, metadata[4],
					metadata[1].equals("t"), 
					metadata[2].equals("t"), 
					metadata[3].equals("t"),
					metadata[5], metadata[6]);
		} else {
			params = new IndexedAttributeParameters(mmURI, typeName, attrName);
		}

		return params;
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
	public List<String> validateExpression(String derivationlanguage, String derivationlogic) {

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
	public CompositeGraphChangeListener getCompositeGraphChangeListener() {
		return listener;
	}

	@Override
	public void setSyncMetricsEnabled(Boolean enable) {
		isSyncMetricsEnabled = enable;
	}

	public Map<VcsCommitItem, IHawkModelResource> getFileToResourceMap() {
		if (!isSyncMetricsEnabled)
			System.err.println("WARNING: isSyncMetricsEnabled == false, this method will return an empty Map");
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
		// TODO we should honor the derivation language info that we already ask
		// the user for
		if (getKnownQueryLanguages().keySet().contains("org.hawk.epsilon.emc.EOLQueryEngine"))
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
	public CompositeStateListener getCompositeStateListener() {
		return stateListener;
	}

	@Override
	public void setPolling(int base, int max) {
		minDelay = base;
		maxDelay = max;
	}

	@Override
	public void waitFor(HawkState targetState) throws InterruptedException {
		waitFor(targetState, 0);
	}

	@Override
	public void waitFor(HawkState targetState, long timeoutMillis) throws InterruptedException {
		synchronized (stateListener) {
			final long end = System.currentTimeMillis() + timeoutMillis;
			for (HawkState s = stateListener.getCurrentState(); s != targetState; s = stateListener.getCurrentState()) {
				if (s == HawkState.STOPPED) {
					throw new IllegalStateException("The selected Hawk is stopped");
				}

				if (timeoutMillis == 0) {
					stateListener.wait();
				} else {
					final long remaining = end - System.currentTimeMillis();
					if (remaining > 0) {
						// Wait for the remaining time
						stateListener.wait(remaining);
					} else {
						// Exit the loop due to timeout
						break;
					}
				}
			}
		}
	}

	@Override
	public void scheduleTask(TimerTask task, long delayMillis) {
		// TODO See if this solves the concurrency issues, otherwise backtrack
		updateTimer.schedule(task, delayMillis);
	}

	
}
