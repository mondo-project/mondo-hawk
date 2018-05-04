/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - extract import to interface, cleanup,
 *       use revision in imports, divide into super + subclass
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
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public abstract class BaseModelIndexer implements IModelIndexer {

	private final class RunUpdateTask extends TimerTask {
		@Override
		public void run() {
			runUpdateTask();
		}
	}

	public static class DefaultFileImporter implements IFileImporter {
		private final Map<String, File> cachedImports = new HashMap<>();
		private final IVcsManager vcs;
		private final String revision;
		private final File tempDir;
	
		public DefaultFileImporter(IVcsManager vcs, String revision, File tempDir) {
			this.vcs = vcs;
			this.revision = revision;
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
	
				File result = vcs.importFile(revision, commitPath, destination);
				cachedImports.put(commitPath, result);
				return result;
			} else {
				return cachedImports.get(commitPath);
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseModelIndexer.class);

	private static final int CEILING_DELAY_FAILED_UPDATES = 60_000;

	public static final int FILECOUNT_PROGRESS_THRESHOLD = 100;
	public static final int SHUTDOWN_WAIT_SECONDS = 120;
	public static final int DEFAULT_MAXDELAY = 1000 * 512;
	public static final int DEFAULT_MINDELAY = 5000;

	protected static final boolean IS_DERIVED = true;
	protected static final boolean IS_INDEXED = false;

	protected boolean isSyncMetricsEnabled = false;
	protected Map<VcsCommitItem, IHawkModelResource> fileToResourceMap = new HashMap<>();

	protected int deletedFiles, interestingFiles, currchangeditems, loadedResources;
	private long synctime;

	protected boolean latestUpdateFoundChanges = false;
	protected String name;
	protected File parentfolder = null;
	protected boolean running = false;
	protected final ICredentialsStore credStore;

	protected IGraphDatabase graph = null;
	protected List<IVcsManager> monitors = new ArrayList<>();
	protected List<IModelUpdater> updaters = new LinkedList<>();
	protected IMetaModelUpdater metamodelupdater = null;
	protected Map<String, IModelResourceFactory> modelParsers = new HashMap<>();
	protected Map<String, IMetaModelResourceFactory> metamodelParsers = new HashMap<>();
	protected Map<String, IQueryEngine> knownQueryLanguages = new HashMap<>();
	protected IConsole console;

	private int maxDelay = DEFAULT_MAXDELAY;
	private int minDelay = DEFAULT_MINDELAY;
	private int currentDelay = minDelay;
	private ScheduledExecutorService updateTimer = null;

	private final CompositeGraphChangeListener listener = new CompositeGraphChangeListener();
	protected final CompositeStateListener stateListener = new CompositeStateListener();

	/**
	 * We cache the list of derived attributes to avoid switching back and forth between
	 * batch and transactional mode when inserting large numbers of files (since we check
	 * if there are any derived attrs before we insert the derived attribute listener).
	 *
	 * Every method that changes the derived attributes in some form should set this to
	 * null, so it is recomputed in the next call to {@link #getDerivedAttributes()}.
	 */
	private Collection<IndexedAttributeParameters> cachedDerivedAttributes = null;

	/**
	 * Creates an indexer.
	 * 
	 * @param name
	 *            Name of the new indexer.
	 * @param parentFolder
	 *            Folder where the contents of the index should be stored.
	 * @param credStore
	 *            Credentials store for the VCS usernames and passwords.
	 * @param c
	 *            Console for printing user-facing informational and error messages.
	 *            More detailed information is sent to the SLF4J logs.
	 */
	public BaseModelIndexer(String name, File parentFolder, ICredentialsStore credStore, IConsole c) {
		this.name = name;
		this.console = c;
		this.credStore = credStore;
		this.parentfolder = parentFolder;
	}

	/**
	 * Tells the indexer that this repository may need to be re-indexed.
	 */
	protected abstract void resetRepository(String repoURL);

	/**
	 * Adds a {@link IVcsManager} to be immediately indexed by Hawk. If
	 * <code>persist</code> is <code>true</code>, the new manager should be added to
	 * the Hawk index configuration file.
	 */
	public void addVCSManager(IVcsManager vcs, boolean persist) {
		monitors.add(vcs);

		try {
			if (persist) {
				saveIndexer();
			}
		} catch (Exception e) {
			LOGGER.error("addVCSManager tried to saveIndexer but failed", e);
		}
	
		requestImmediateSync();
	}

	/**
	 * Removes a {@link IVcsManager} from Hawk, including any contained models
	 * indexed.
	 */
	public void removeVCSManager(IVcsManager vcs) throws Exception {
		stateListener.state(HawkState.UPDATING);
		stateListener.info("Removing vcs...");
		monitors.remove(vcs);

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
	
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Removed vcs.");
	}

	/**
	 * Updates the Hawk index based on the current contents of the specified {@link IVcsManager}.
	 * Returns <code>true</code> if successful, <code>false</code> otherwise.
	 */
	protected abstract boolean synchronise(IVcsManager vcsManager) throws Exception;

	@Override
	public void requestImmediateSync() {
		if (running) {
			updateTimer.submit(new RunUpdateTask());
		}
	}

	protected boolean synchronise() throws Exception {
		listener.synchroniseStart();
		stateListener.state(HawkState.UPDATING);
	
		try {
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
							allSync = allSync && synchronise(m);
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


	protected boolean deleteRemovedModels(boolean success, IModelUpdater u, final Set<VcsCommitItem> deleteditems) {
		stateListener.info("Deleting models removed from repository...");

		try {
			listener.changeStart();
			for (VcsCommitItem c : deleteditems) {
				success = success && u.deleteAll(c);
			}
			listener.changeSuccess();
		} catch (Exception e) {
			success = false;
			LOGGER.error("Error while deleting removed files from store", e);
			listener.changeFailure();
		}

		return success;
	}

	protected void importFiles(IFileImporter importer, final Set<VcsCommitItem> changedItems, final Map<String, File> pathToImported) {
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

	protected List<IModelUpdater> getUpdaters() {
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
			LOGGER.error("Error while saving the indexer during shut down", e);
		}
	
		try {
			LOGGER.info("Waiting {}s for all scheduled tasks to complete", SHUTDOWN_WAIT_SECONDS);
			updateTimer.shutdownNow();
			updateTimer.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Wait for scheduled tasks was interrupted", e);
		}

		for (IVcsManager monitor : monitors) {
			monitor.shutdown();
		}

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
		return new HashSet<>(monitors);
	}

	@Override
	public String getId() {
		return getClass().getCanonicalName();
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

	@Override
	public void removeMetamodels(String... mm) throws Exception {
		stateListener.state(HawkState.UPDATING);
		stateListener.info("Removing metamodels...");
		for (String repoURL : metamodelupdater.removeMetamodels(this, mm)) {
			resetRepository(repoURL);
		}
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Removed metamodels.");
	}

	@Override
	public void registerMetamodels(File... metamodel) throws Exception {
		stateListener.info("Adding metamodel(s)...");
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
	
		}

		// if metamodels added successfully
		if (metamodelupdater.insertMetamodels(set, this)) {
			// reset repositories as models may be parsable in them
			for (IVcsManager s : monitors) {
				resetRepository(s.getLocation());
			}
		}
		stateListener.info("Added metamodel(s).");
	}

	@Override
	public IConsole getConsole() {
		return console;
	}

	public void addModelResourceFactory(IModelResourceFactory p) {
		modelParsers.put(p.getType(), p);
	}

	public void addMetaModelResourceFactory(IMetaModelResourceFactory p) {
		metamodelParsers.put(p.getType(), p);
	}

	public void addQueryEngine(IQueryEngine q) {
		knownQueryLanguages.put(q.getType(), q);
	}

	@Override
	public Set<String> getKnownMetaModelParserTypes() {
		return metamodelParsers.keySet();
	}

	@Override
	public IMetaModelResourceFactory getMetaModelParser(String type) {
		if (type == null) {
			console.printerrln("null type given to getMetaModelParser(type), returning null");
		} else {
			return metamodelParsers.get(type);
		}
	
		return null;
	}

	@Override
	public IModelResourceFactory getModelParser(String type) {
		if (type == null)
			console.printerrln("null type given to getModelParser(type), returning null");
		else
			return modelParsers.get(type);
	
		return null;
	}

	public IMetaModelResourceFactory getMetaModelParserFromFilename(String name) {
		if (name == null)
			console.printerrln("null extension given to getMetaModelParserFromFilename(extension), returning null");
		else
			for (String p : metamodelParsers.keySet()) {
				IMetaModelResourceFactory parser = metamodelParsers.get(p);
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
			for (String p : modelParsers.keySet()) {
				IModelResourceFactory parser = modelParsers.get(p);
				for (String ext : parser.getModelExtensions()) {
					if (name.endsWith(ext)) {
						return parser;
					}
				}
			}
		return null;
	}

	@Override
	public Map<String, IQueryEngine> getKnownQueryLanguages() {
		return knownQueryLanguages;
	}

	@Override
	public Set<String> getKnownMetamodelFileExtensions() {
		Set<String> exts = new HashSet<>();
		for (IMetaModelResourceFactory mm : metamodelParsers.values()) {
			exts.addAll(mm.getMetaModelExtensions());
		}
		return exts;
	}

	@Override
	public void addModelUpdater(IModelUpdater u) {
		try {
			u.run(console, this);
			updaters.add(u);
		} catch (Exception e) {
			LOGGER.error("Error while adding model updater", e);
		}
	}

	@Override
	public void setMetaModelUpdater(IMetaModelUpdater u) {
		if (metamodelupdater == null) {
			u.run();
			metamodelupdater = u;
		} else {
			LOGGER.warn("metamodel updater alredy registered, cannot have more than one");
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
	public void setDB(IGraphDatabase db, boolean persist) {
		graph = db;
		try {
			if (persist) {
				saveIndexer();
			}
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
		for (String factoryNames : metamodelParsers.keySet()) {
			IMetaModelResourceFactory f = metamodelParsers.get(factoryNames);
			console.println(f.getType());
			metamodelupdater.insertMetamodels(f.getStaticMetamodels(), this);
		}
		console.println("inserting static metamodels complete");
	
		// register all metamodels in graph to their factories
		registerMetamodelFiles();
	
		// begin scheduled updates from vcs
		updateTimer = new ScheduledThreadPoolExecutor(1);
		updateTimer.submit(new RunUpdateTask());
	
		running = true;
		stateListener.state(HawkState.RUNNING);
		stateListener.info("Initialized Hawk.");
	}

	protected void runUpdateTask() {
		stateListener.info("Updating Hawk...");
		long start = System.currentTimeMillis();
	
		boolean synchronised = true;
		console.println("updating indexer: ");
	
		try {
			synchronised = synchronise();
		} catch (Throwable e) {
			LOGGER.error("Error during synchronisation", e);
		}
	
		if (!synchronised) {
			console.println("SYNCHRONISATION ERROR");
			stateListener.error("Update FAILED!");
		}
	
		// Timer only enabled if the delay is non-zero
		if (maxDelay > 0) {
			if (!latestUpdateFoundChanges) {
				final int oldDelay = currentDelay;
				currentDelay = Math.min(currentDelay * 2, maxDelay);
				console.println(
					String.format("same revision, incrementing check timer: %s s -> %s s (max: %s s)",
						oldDelay / 1000, currentDelay / 1000, maxDelay / 1000));
			} else if (!synchronised) {
				int oldDelay = currentDelay;
				currentDelay = Math.min(currentDelay * 2, Math.min(CEILING_DELAY_FAILED_UPDATES, maxDelay));
				console.println(
					String.format("update failed, incrementing check timer: %s s -> %s s (max: %s s with %s s ceiling)",
						oldDelay / 1000, currentDelay / 1000, maxDelay / 1000, CEILING_DELAY_FAILED_UPDATES / 1000));
			} else {
				console.println("different revisions, resetting check timer and propagating changes!");
				currentDelay = minDelay;
			}
	
			updateTimer.schedule(new RunUpdateTask(), currentDelay, TimeUnit.MILLISECONDS);
		}
	
		final long time = (System.currentTimeMillis() - start);
		stateListener.info(String.format(
			"Updated Hawk instance %s (%s). %d s %d ms",
			getName(), synchronised ? "success" : "failure", time / 1_000, time % 1_000
		));
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

		final boolean success = metamodelupdater.addDerivedAttribute(metamodeluri, typename, attributename,
				attributetype, isMany, isOrdered, isUnique, derivationlanguage, derivationlogic, this);
		if (success) {
			for (IModelUpdater u : getUpdaters())
				u.updateDerivedAttribute(metamodeluri, typename, attributename, attributetype, isMany, isOrdered,
						isUnique, derivationlanguage, derivationlogic);
		}

		cachedDerivedAttributes = null;
		stateListener.info(String.format("Added derived attribute %s::%s::%s.", metamodeluri, typename, attributename));
	}

	@Override
	public Collection<IndexedAttributeParameters> getDerivedAttributes() {
		if (cachedDerivedAttributes == null) {
			cachedDerivedAttributes = getExtraAttributes(IS_DERIVED);
		}
		return cachedDerivedAttributes;
	}

	@Override
	public boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName) {
		stateListener.info(String.format(
			"Removing derived attribute %s::%s::%s...",
			metamodelUri, typeName, attributeName));
	
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
		stateListener.info(String.format(
			"Adding indexed attribute %s::%s::%s...",
			metamodeluri, typename, attributename));
	
		if (metamodelupdater.addIndexedAttribute(metamodeluri, typename, attributename, this))
			for (IModelUpdater u : getUpdaters())
				u.updateIndexedAttribute(metamodeluri, typename, attributename);
	
		stateListener.info(String.format(
			"Added indexed attribute %s::%s::%s.",
			metamodeluri, typename, attributename));
	}

	@Override
	public Collection<IndexedAttributeParameters> getIndexedAttributes() {
		return getExtraAttributes(IS_INDEXED);
	}

	@Override
	public boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename) {
		stateListener.info(String.format(
			"Removing indexed attribute %s::%s::%s...",
			metamodelUri, typename, attributename));
	
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
		final String[] propertyInfo = (String[]) typenode.getProperty(attrName);
		if (propertyInfo == null) {
			LOGGER.warn("Information on derived attribute {} in type node {} is missing", typenode.getId(), attrName);
			return false;
		} else {
			return propertyInfo[0].equals("d");
		}
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
					IndexedAttributeParameters params = getAttributeParametersfromMetadata(mmURI, typeName, attrName, metadata);
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
			params = new DerivedAttributeParameters(mmURI, typeName, attrName, metadata[4],
					metadata[1].equals("t"), metadata[2].equals("t"), metadata[3].equals("t"),
					metadata[5], metadata[6]);
		} else {
			params = new IndexedAttributeParameters(mmURI, typeName, attrName);
		}
	
		return params;
	}

	@Override
	public Collection<String> getIndexes() {
		Set<String> ret = new HashSet<String>();
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
		if (!isSyncMetricsEnabled) {
			LOGGER.warn("isSyncMetricsEnabled == false, this method will return an empty Map");
		}
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
		if (getKnownQueryLanguages().keySet().contains("org.hawk.epsilon.emc.EOLQueryEngine"))
			return "org.hawk.epsilon.emc.EOLQueryEngine";
		else
			return "error: default derived attribute engine (EOLQueryEngine) not added to Hawk. Derived attributes cannot be calculated!";
	}

	@Override
	public boolean addStateListener(IStateListener listener) {
		return stateListener.add(listener);
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
	public <T> ScheduledFuture<T> scheduleTask(Callable<T> task, long delayMillis) {
		return updateTimer.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
	}

	protected void inspectChanges(Iterable<VcsCommitItem> files, Set<VcsCommitItem> deleteditems, Set<VcsCommitItem> interestingfiles) {
		stateListener.info("Calculating relevant changed model files...");
	
		for (VcsCommitItem r : files) {
			for (IModelResourceFactory parser : modelParsers.values()) {
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
				if (VERBOSE) {
					console.println(String.format(
						"--> %s HAS CHANGED (%S), PROPAGATING CHANGES",
						c.getPath(), c.getChangeType()));
				}

				deleteditems.add(c);
				it.remove();
			}
		}
	}

	protected boolean internalSynchronise(final String currentRevision, final IVcsManager m,
			final IModelUpdater u, final Set<VcsCommitItem> deletedItems, final Set<VcsCommitItem> interestingfiles,
			final String monitorTempDir) {
		boolean success = true;

		// enters transaction mode!
		Set<VcsCommitItem> currReposChangedItems = u.compareWithLocalFiles(interestingfiles);

		// metadata about synchronise
		final int totalFiles = currReposChangedItems.size();
		currchangeditems = currchangeditems + totalFiles;

		// create temp files with changed repos files
		final Map<String, File> pathToImported = new HashMap<>();
		final IFileImporter importer = new DefaultFileImporter(m, currentRevision, new File(monitorTempDir));
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
				if (fileCountProgress && (totalProcessedFiles == 0 && filesProcessedSinceLastPrint == 0
						|| filesProcessedSinceLastPrint == FILECOUNT_PROGRESS_THRESHOLD)) {
					totalProcessedFiles += filesProcessedSinceLastPrint;

					final long millisPrint = System.currentTimeMillis();
					stateListener.info(String.format("Processed %d/%d files in repo %s (%s sec, %s sec total)",
							totalProcessedFiles, totalFiles, m.getLocation(),
							(millisPrint - millisSinceLastPrint) / 1000, (millisPrint - millisSinceStart) / 1000));

					filesProcessedSinceLastPrint = 0;
					millisSinceLastPrint = millisPrint;
				}

				IHawkModelResource r = null;
				if (u.caresAboutResources()) {
					final File file = pathToImported.get(v.getPath());
					if (file == null || !file.exists()) {
						console.printerrln("warning, cannot find file: " + file + ", ignoring changes");
					} else {
						IModelResourceFactory mrf = getModelParserFromFilename(file.getName().toLowerCase());
						if (mrf.canParse(file)) {
							r = mrf.parse(importer, file);
						}
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
			stateListener.info(String.format("Processed %d/%d files in repo %s (%s sec, %s sec total)",
					totalProcessedFiles, totalFiles, m.getLocation(), (millisPrint - millisSinceLastPrint) / 1000,
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

	protected boolean synchroniseFiles(String revision, IVcsManager vcsManager, final Collection<VcsCommitItem> files) {
		final Set<VcsCommitItem> deleteditems = new HashSet<VcsCommitItem>();
		final Set<VcsCommitItem> interestingfiles = new HashSet<VcsCommitItem>();
		inspectChanges(files, deleteditems, interestingfiles);
		deletedFiles = deletedFiles + deleteditems.size();
		interestingFiles = interestingFiles + interestingfiles.size();

		final String monitorTempDir = graph.getTempDir();
		File temp = new File(monitorTempDir);
		temp.mkdir();

		// for each registered updater
		boolean updatersOK = true;
		for (IModelUpdater updater : getUpdaters()) {
			updatersOK = updatersOK && internalSynchronise(revision, vcsManager, updater, deleteditems, interestingfiles, monitorTempDir);
		}

		// delete temporary files
		if (!FileOperations.deleteFiles(new File(monitorTempDir), true))
			console.printerrln("error in deleting temporary local vcs files");
		return updatersOK;
	}
	
}