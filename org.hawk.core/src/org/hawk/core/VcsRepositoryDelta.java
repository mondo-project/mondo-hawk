/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Nikolas Matragkas, James Williams, Dimitris Kolovos - initial API and implementation
 *     Konstantinos Barmpis - adaption for use in Hawk
 ******************************************************************************/
package org.hawk.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VcsRepositoryDelta implements Serializable {

	private static final long serialVersionUID = 1L;
	transient protected VcsRepository repository;
	protected List<VcsCommit> commits = new ArrayList<VcsCommit>();
	protected String latestRevision;

	public VcsRepository getRepository() {
		return repository;
	}

	public void setRepository(VcsRepository repository) {
		this.repository = repository;
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
	public List<VcsCommitItem> getCompactedCommitItems() {
		List<VcsCommitItem> commitItems = new ArrayList<VcsCommitItem>();

		for (VcsCommit commit : commits) {
			for (VcsCommitItem item : commit.getItems()) {
				switch (item.getChangeType()) {
				case ADDED:
					removeCommitItemByPath(commitItems, item.getPath());
					commitItems.add(item);
					break;
				case DELETED:
					removeCommitItemByPath(commitItems, item.getPath());
					commitItems.add(item);
					break;
				case UPDATED:
					removeCommitItemByPath(commitItems, item.getPath());
					commitItems.add(item);
					break;
				case REPLACED:
					removeCommitItemByPath(commitItems, item.getPath());
					commitItems.add(item);
					break;
				case UNKNOWN:
					// ignore
					System.err.println("Found unknnown commit kind: "
							+ item.getChangeType());
					break;
				}
			}
		}

		return commitItems;
	}

	// TODO: (VcsRepositoryDelta developer comment) Is the path enough?
	protected void removeCommitItemByPath(List<VcsCommitItem> items, String path) {
		Iterator<VcsCommitItem> it = items.iterator();
		while (it.hasNext()) {
			VcsCommitItem item = it.next();
			if (item.getPath().equals(path))
				it.remove();
		}
	}

}
