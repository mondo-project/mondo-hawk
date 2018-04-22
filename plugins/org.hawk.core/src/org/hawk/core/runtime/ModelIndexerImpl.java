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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.util.FileOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes only the latest revision into the graph.
 */
public class ModelIndexerImpl extends BaseModelIndexer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelIndexerImpl.class);

	private final Map<String, String> currLocalTopRevisions = new HashMap<>();
	private final Map<String, String> currReposTopRevisions = new HashMap<>();

	public ModelIndexerImpl(String name, File parentfolder, ICredentialsStore credStore, IConsole c) {
		super(name, parentfolder, credStore, c);
	}

	@Override
	public void addVCSManager(IVcsManager vcs, boolean persist) {
		monitors.add(vcs);
		currLocalTopRevisions.put(vcs.getLocation(), "-3");
		currReposTopRevisions.put(vcs.getLocation(), "-4");
	
		try {
			if (persist) {
				saveIndexer();
			}
		} catch (Exception e) {
			LOGGER.error("addVCSManager tried to saveIndexer but failed", e);
		}
	
		requestImmediateSync();
	}

	@Override
	public void removeVCSManager(IVcsManager vcs) throws Exception {
		stateListener.state(HawkState.UPDATING);
		stateListener.info("Removing vcs...");
	
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

	@Override
	protected void resetRepository(String repoURL) {
		System.err.println("reseting local top revision of repository: " + repoURL
				+ "\n(as elements in it were removed or new metamodels were added to Hawk)");
		currLocalTopRevisions.put(repoURL, "-3");
	}

	@Override
	protected boolean synchronise(IVcsManager vcsManager) throws Exception {
		boolean success = true;
	
		String currentRevision = currReposTopRevisions.get(vcsManager.getLocation());
		try {
			// Try to fetch the current revision from the VCS, if not, keep the latest seen
			// revision
			currentRevision = vcsManager.getCurrentRevision();
			currReposTopRevisions.put(vcsManager.getLocation(), currentRevision);
		} catch (Exception e) {
			console.printerrln(e);
			success = false;
		}
	
		if (!currentRevision.equals(currLocalTopRevisions.get(vcsManager.getLocation()))) {
			latestUpdateFoundChanges = true;
	
			final Set<VcsCommitItem> deleteditems = new HashSet<VcsCommitItem>();
			final Set<VcsCommitItem> interestingfiles = new HashSet<VcsCommitItem>();
			inspectChanges(vcsManager, deleteditems, interestingfiles);
			deletedFiles = deletedFiles + deleteditems.size();
			interestingFiles = interestingFiles + interestingfiles.size();
	
			final String monitorTempDir = graph.getTempDir();
			File temp = new File(monitorTempDir);
			temp.mkdir();
	
			// for each registered updater
			boolean updatersOK = true;
			for (IModelUpdater updater : getUpdaters()) {
				updatersOK = updatersOK && internalSynchronise(currentRevision, vcsManager, updater, deleteditems, interestingfiles, monitorTempDir);
			}
	
			// delete temporary files
			if (!FileOperations.deleteFiles(new File(monitorTempDir), true))
				console.printerrln("error in deleting temporary local vcs files");
	
			if (updatersOK) {
				currLocalTopRevisions.put(vcsManager.getLocation(),
						currReposTopRevisions.get(vcsManager.getLocation()));
			} else {
				success = false;
				currLocalTopRevisions.put(vcsManager.getLocation(), "-3");
			}
		}
	
		return success;
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

	private void inspectChanges(IVcsManager m, Set<VcsCommitItem> deleteditems, Set<VcsCommitItem> interestingfiles) throws Exception {
		Collection<VcsCommitItem> files = m.getDelta(currLocalTopRevisions.get(m.getLocation()));
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

}
