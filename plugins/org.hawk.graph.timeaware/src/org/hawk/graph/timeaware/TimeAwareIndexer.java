/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.timeaware;

import java.io.File;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.runtime.BaseModelIndexer;
import org.hawk.graph.timeaware.VCSManagerIndex.RepositoryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes all revisions of the models in the repository. Requires using
 * backends and locations that can handle full history (currently Greycat +
 * SVN).
 */
public class TimeAwareIndexer extends BaseModelIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareIndexer.class);

	public TimeAwareIndexer(String name, File parentFolder, ICredentialsStore credStore, IConsole c) {
		super(name, parentFolder, credStore, c);
	}

	@Override
	protected void resetRepository(String repoURL) {
		// nothing to do - we keep track of repository information in the graph
	}

	@Override
	protected boolean synchronise(IVcsManager vcsManager) {
		if (!(graph instanceof ITimeAwareGraphDatabase)) {
			LOGGER.error("This indexer requires a time-aware backend, aborting");
			return false;
		}
		ITimeAwareGraphDatabase taGraph = (ITimeAwareGraphDatabase)graph;

		String lastRev;
		try {
			lastRev = getLastIndexedRevision(vcsManager);
		} catch (Exception e) {
			LOGGER.error("Could not fetch the last indexed revision", e);
			return false;
		}

		boolean success = true;
		try {
			final String currentRevision = vcsManager.getCurrentRevision();
			if (!currentRevision.equals(lastRev)) {
				latestUpdateFoundChanges = true;

				VcsRepositoryDelta delta = vcsManager.getDelta(lastRev, currentRevision);
				for (VcsCommit commit : delta.getCommits()) {
					taGraph.setTime(commit.getJavaDate().toInstant().getEpochSecond());

					/*
					 * TODO: allow for fixing unresolved proxies in previous versions? Might make
					 * sense if we forgot to add a metamodel in a previous version.
					 */
					success = success && synchroniseFiles(commit.getRevision(), vcsManager, commit.getItems());
					console.println(String.format("Index revision %s (timepoint %d) of %s",
							commit.getRevision(), commit.getJavaDate().toInstant().getEpochSecond(), commit.getDelta().getManager().getLocation()));

					taGraph.setTime(0);
					setLastIndexedRevision(vcsManager, commit.getRevision());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to synchronise repository " + vcsManager.getLocation(), e);
			return false;
		} finally {
			taGraph.setTime(0);
		}

		return success;
	}

	protected String getLastIndexedRevision(IVcsManager vcsManager) throws Exception {
		String lastRev;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			VCSManagerIndex vcsIndex = new VCSManagerIndex(graph);
			RepositoryNode repoNode = vcsIndex.getOrCreateRepositoryNode(vcsManager.getLocation());
			lastRev = repoNode.getLastRevision();
			tx.success();
		}
		return lastRev;
	}

	protected void setLastIndexedRevision(IVcsManager vcsManager, String lastRev) throws Exception {
		try (IGraphTransaction tx = graph.beginTransaction()) {
			VCSManagerIndex vcsIndex = new VCSManagerIndex(graph);
			RepositoryNode repoNode = vcsIndex.getOrCreateRepositoryNode(vcsManager.getLocation());
			repoNode.setLastRevision(lastRev);
			tx.success();
		}
	}
	
}
