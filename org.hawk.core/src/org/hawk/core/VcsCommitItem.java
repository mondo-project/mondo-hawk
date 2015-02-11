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

	public void setPath(String path) {
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
