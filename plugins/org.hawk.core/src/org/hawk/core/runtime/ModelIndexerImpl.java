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
import java.util.Map;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommitItem;

/**
 * Indexes only the latest revision into the graph, and does not maintain
 * repository nodes.
 */
public class ModelIndexerImpl extends BaseModelIndexer {
	private final Map<String, String> currLocalTopRevisions = new HashMap<>();
	private final Map<String, String> currReposTopRevisions = new HashMap<>();

	public ModelIndexerImpl(String name, File parentfolder, ICredentialsStore credStore, IConsole c) {
		super(name, parentfolder, credStore, c);
	}

	@Override
	public void addVCSManager(IVcsManager vcs, boolean persist) {
		currLocalTopRevisions.put(vcs.getLocation(), "-3");
		currReposTopRevisions.put(vcs.getLocation(), "-4");

		super.addVCSManager(vcs, persist);
	}

	@Override
	public void removeVCSManager(IVcsManager vcs) throws Exception {
		currLocalTopRevisions.remove(vcs.getLocation());
		currReposTopRevisions.remove(vcs.getLocation());
		super.removeVCSManager(vcs);
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
			final Collection<VcsCommitItem> files = vcsManager.getDelta(currLocalTopRevisions.get(vcsManager.getLocation()));
			latestUpdateFoundChanges = true;
			boolean updatersOK = synchroniseFiles(currentRevision, vcsManager, files);
	
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

}
