/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
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
 * 	   Nikolas Matragkas, James Williams, Dimitris Kolovos - initial API and implementation
 *     Konstantinos Barmpis - adaptation for use in Hawk
 *     Antonio Garcia-Dominguez - speed up commit item compaction
 ******************************************************************************/
package org.hawk.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VcsRepositoryDelta implements Serializable {

	private static final long serialVersionUID = 1L;
	protected IVcsManager vcsManager;
	protected List<VcsCommit> commits = new ArrayList<VcsCommit>();
	protected String latestRevision;

	public IVcsManager getManager() {
		return vcsManager;
	}

	public void setManager(IVcsManager manager) {
		this.vcsManager = manager;
	}

	public List<VcsCommit> getCommits() {
		return commits;
	}

	public String getLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(String latestRevision) {
		this.latestRevision = latestRevision;
	} // TODO (VcsRepositoryDelta developer comment) THIS NEEDS SETTING ON
		// CREATION

	// XXX (Kostas comment) keeping all info from svn, compacted
	public Collection<VcsCommitItem> getCompactedCommitItems() {
		final Map<String, VcsCommitItem> compacted = new HashMap<>();
		for (VcsCommit commit : commits) {
			for (VcsCommitItem item : commit.getItems()) {
				switch (item.getChangeType()) {
				case ADDED:
				case DELETED:
				case UPDATED:
				case REPLACED:
					compacted.put(item.getPath(), item);
					break;
				case UNKNOWN:
					System.err.println("Found unknnown commit kind: " + item.getChangeType());
					break;
				}
			}
		}

		return compacted.values();
	}

	public String toString() {
		return "delta on repo: " + vcsManager.getLocation() + "\n" + commits.toString();

	}

}
