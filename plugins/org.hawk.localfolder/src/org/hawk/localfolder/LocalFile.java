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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;

/**
 * VCS manager that watches over a single file. Created for TTC 2018.
 */
public class LocalFile extends FileBasedLocation {

	private long initialVersion;
	private File monitoredFile;

	@Override
	public void init(String vcsloc, IModelIndexer indexer) throws Exception {
		console = indexer.getConsole();

		// Accept both regular paths and file:// URIs
		Path path;
		try {
			path = Paths.get(new URI(vcsloc));
		} catch (URISyntaxException | IllegalArgumentException ex) {
			path = Paths.get(vcsloc);
		}

		monitoredFile = path.toFile().getCanonicalFile();
		initialVersion = monitoredFile.lastModified();
		String repositoryURI = path.toUri().toString();

		// If the file doesn't exist, it might be because this is a local folder in
		// a remote server - try to preserve the provided vcsloc as is. Otherwise,
		// if the server and the client use different operating systems we could end
		// up with an unusable URL in the server.
		if (monitoredFile.exists()) {
			repositoryURL = repositoryURI;
		} else {
			repositoryURL = vcsloc;
		}
	}

	@Override
	protected String getCurrentRevision(boolean alter) {
		final long lastModified = monitoredFile.lastModified();
		if (alter) {
			lastRevision = lastModified;
		}

		return lastModified == initialVersion ? FIRST_REV : lastModified + "";
	}

	@Override
	public File importFile(String revision, String p, File temp) {
		return monitoredFile;
	}

	@Override
	public boolean isActive() {
		return monitoredFile != null && monitoredFile.exists();
	}

	@Override
	public void shutdown() {
		monitoredFile = null;
	}

	@Override
	public String getHumanReadableName() {
		return "Local File Monitor";
	}

	@Override
	public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {
		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setManager(this);

		VcsCommit commit = new VcsCommit();
		commit.setAuthor("i am a local file driver - no authors recorded");
		commit.setDelta(delta);
		commit.setJavaDate(null);
		commit.setMessage("i am a local file driver - no messages recorded");
		delta.getCommits().add(commit);

		VcsCommitItem c = new VcsCommitItem();
		c.setCommit(commit);
		c.setPath("/" + monitoredFile.getName());

		final long currentTimestamp = monitoredFile.lastModified();
		if (currentTimestamp != lastRevision) {
			if (currentTimestamp == initialVersion) {
				c.setChangeType(VcsChangeType.ADDED);
				commit.setRevision(currentTimestamp + "");
				commit.getItems().add(c);
			} else {
				c.setChangeType(VcsChangeType.UPDATED);
				commit.setRevision(currentTimestamp + "");
				commit.getItems().add(c);
			}
		}

		// Update the latest revision seen
		getCurrentRevision(true);

		return delta;
	}
}
