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
package org.hawk.localfolder;

import java.util.Collection;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommitItem;

public abstract class FileBasedLocation implements IVcsManager {

	protected static final String FIRST_REV = "0";

	protected IConsole console;
	protected String repositoryURL;
	protected String currentRevision = FIRST_REV;
	private boolean isFrozen = false;

	@Override
	public String getLocation() {
		return repositoryURL;
	}

	@Override
	public void setCredentials(String username, String password, ICredentialsStore credStore) {
		// ignore
	}

	protected String makeRelative(String base, String extension) {
		if (!extension.startsWith(base)) {
			return extension;
		}
		return extension.substring(base.length());
	}

	@Override
	public boolean isAuthSupported() {
		return false;
	}

	@Override
	public boolean isPathLocationAccepted() {
		return true;
	}

	@Override
	public boolean isURLLocationAccepted() {
		return true;
	}

	@Override
	public String getRepositoryPath(String rawPath) {
		final String emfUriPrefix = getLocation().replaceFirst("file:///", "file:/");
		if (rawPath.startsWith(emfUriPrefix)) {
			return rawPath.substring(emfUriPrefix.length());
		}
		return rawPath;
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public boolean isFrozen() {
		return isFrozen;
	}

	@Override
	public void setFrozen(boolean f) {
		isFrozen = f;
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public Collection<VcsCommitItem> getDelta(String endRevision) throws Exception {
		return getDelta(FIRST_REV, endRevision).getCompactedCommitItems();
	}

	@Override
	public String getFirstRevision() throws Exception {
		return FIRST_REV;
	}

	@Override
	public String getCurrentRevision() {
		return getCurrentRevision(false);
	}

	protected abstract String getCurrentRevision(boolean alter);

	@Override
	public void run() {
		/* nothing */
	}

}