/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - adaption for use in Hawk
 ******************************************************************************/
package org.hawk.core;

import java.io.Serializable;

public class VcsCommitItem implements Serializable {

	private static final long serialVersionUID = 1L;

	protected String path;
	protected VcsChangeType changeType;
	protected VcsCommit commit;

	public VcsCommit getCommit() {
		return commit;
	}

	public void setCommit(VcsCommit commit) {
		this.commit = commit;
	}

	public String getPath() {
		return path;
	}

	/**
	 * path from location of repository, always starting with a single /
	 * @param path
	 */
	public void setPath(String path) {
		assert path.startsWith("/") : "Path " + path + " should start with a slash";
		this.path = path;
	}

	public VcsChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(VcsChangeType changeType) {
		this.changeType = changeType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VcsCommitItem) {
			if (!this.path.equals(((VcsCommitItem) obj).getPath())) {
				return false;
			}
			if (!this.changeType.equals(((VcsCommitItem) obj).getChangeType())) {
				return false;
			}
			if (!(this.commit.revision
					.equals(((VcsCommitItem) obj).getCommit().revision) && this.commit.date
					.equals(((VcsCommitItem) obj).getCommit().date))) {
				return false;
			}
			return true;
		}

		return false;
	}

	public String toString() {
		return (this.path + " : " + this.changeType);
	}

}
