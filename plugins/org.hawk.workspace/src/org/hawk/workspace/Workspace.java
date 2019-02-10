/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
package org.hawk.workspace;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository manager for an Eclipse workspace. This one uses
 * {@link IFile#getModificationStamp()} to find out if a file changed: it is
 * inexpensive to compute, but it changes if you ask for a full build, even if
 * the file has not changed. If all your work is done from Eclipse editors, the
 * {@link LocalHistoryWorkspace} component will trigger updates less often.
 */
public class Workspace implements IVcsManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(Workspace.class);

	private final class WorkspaceDeltaVisitor implements IResourceDeltaVisitor {
		private boolean anyChanges = false;

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			final boolean isFile = delta.getResource() instanceof IFile;
			if (isFile) {
				anyChanges = true;
				return true;
			}
			return !isFile;
		}
	}

	private class WorkspaceListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_CLOSE:
			case IResourceChangeEvent.PRE_DELETE:
				/*
				 * No delta in these events - but we know a project is being closed/deleted, so
				 * we can react to it.
				 */
				pendingChanges = true;
				break;
			case IResourceChangeEvent.POST_CHANGE:
				try {
					IResourceDelta delta = event.getDelta();
					final WorkspaceDeltaVisitor visitor = new WorkspaceDeltaVisitor();
					delta.accept(visitor);

					if (!pendingChanges && visitor.anyChanges) {
						pendingChanges = true;
					}
				} catch (Exception e) {
					console.printerrln(e);
				}
				break;
			}
		}
	}

	private long revision;
	private boolean pendingChanges = false;
	private IConsole console;
	private WorkspaceListener listener;

	/* Needed to emulate the usual URLs within a workspace when concatenated with the file path. */
	public static final String REPOSITORY_URL = "platform:/resource";

	private Set<IFile> previousFiles = new HashSet<>();
	private Map<IFile, Long> recordedStamps = new HashMap<>();
	private boolean isFrozen = false;

	@Override
	public String getFirstRevision() throws Exception {
		return "0";
	}

	@Override
	public VcsRepositoryDelta getDelta(String sStartRevision, String endRevision) throws Exception {
		final long startMillis = System.currentTimeMillis();
		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setManager(this);

		final Set<IFile> files = getAllFiles();
		previousFiles.removeAll(files);
		for (IFile f : previousFiles) {
			addIFile(delta, f, VcsChangeType.DELETED);
			recordedStamps.remove(f);
		}
		previousFiles.clear();

		for (IFile f : files) {
			previousFiles.add(f);

			final long latestRev = getModificationTimestamp(f);

			final Long lastRev = recordedStamps.get(f);
			if (lastRev == null || lastRev < latestRev) {
				recordedStamps.put(f, latestRev);
				addIFile(delta, f, lastRev == null ? VcsChangeType.ADDED : VcsChangeType.UPDATED);
			}
		}

		if (pendingChanges) {
			revision++;
			pendingChanges = false;
		}
		delta.setManager(this);

		if (LOGGER.isInfoEnabled()) {
			final long endMillis = System.currentTimeMillis();
			LOGGER.info("getDelta() over workspace - {} ms", endMillis - startMillis);
		}

		return delta;
	}

	protected long getModificationTimestamp(IFile f) throws CoreException {
		return f.getModificationStamp();
	}

	private Set<IFile> getAllFiles() {
		try {
			final Set<IFile> allFiles = new HashSet<>();
			ResourcesPlugin.getWorkspace().getRoot().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						allFiles.add((IFile) resource);
					}
					return true;
				}

			});
			return allFiles;
		} catch (CoreException e) {
			console.printerrln(e);
			return Collections.emptySet();
		}
	}

	private void addIFile(VcsRepositoryDelta delta, IFile f, final VcsChangeType changeType) {
		VcsCommit commit = new VcsCommit();
		commit.setAuthor("i am a workspace driver - no authors recorded");
		commit.setDelta(delta);
		commit.setJavaDate(new Date());
		commit.setMessage("i am a workspace driver - no messages recorded");
		commit.setRevision(f.getModificationStamp() + "");
		delta.getCommits().add(commit);

		VcsCommitItem c = new VcsCommitItem();
		c.setChangeType(changeType);
		c.setCommit(commit);

		String encodedPath;
		try {
			encodedPath = URLEncoder.encode(
				f.getFullPath().toString(),
				StandardCharsets.UTF_8.toString()
			).replaceAll("%2F", "/").replaceAll("[+]", "%20");

			c.setPath(encodedPath);
			commit.getItems().add(c);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public File importFile(String revision, String uriEncodedPath, File temp) {
		String decodedPath;
		try {
			decodedPath = URLDecoder.decode(uriEncodedPath, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(), e);

			// Fall back to not decoding
			decodedPath = uriEncodedPath;
		}

		// Access directly the file (no need for copying - helps with links between workspace files)
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(decodedPath));
		return file.getRawLocation().toFile();
	}

	@Override
	public boolean isActive() {
		return listener != null;
	}

	@Override
	public void init(String vcsloc, IModelIndexer indexer) throws Exception {
		this.console = indexer.getConsole();
		this.listener = new WorkspaceListener();
	}

	@Override
	public void run() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
	}

	@Override
	public void shutdown() {
		if (listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
			listener = null;
		}
	}

	@Override
	public String getLocation() {
		return REPOSITORY_URL;
	}

	@Override
	public void setCredentials(String username, String password, ICredentialsStore credStore) {
		// ignore
	}

	@Override
	public String getHumanReadableName() {
		return "Workspace Driver - Workspace Timestamp Based";
	}

	@Override
	public String getCurrentRevision() throws Exception {
		if (pendingChanges) {
			return (revision + 1) + "";
		} else {
			return revision + "";
		}
	}

	@Override
	public Collection<VcsCommitItem> getDelta(String string) throws Exception {
		return getDelta(string, getCurrentRevision()).getCompactedCommitItems();
	}

	@Override
	public boolean isAuthSupported() {
		return false;
	}

	@Override
	public boolean isPathLocationAccepted() {
		return false;
	}

	@Override
	public boolean isURLLocationAccepted() {
		// platform:/resource is a URL
		return true;
	}

	@Override
	public String getRepositoryPath(String rawPath) {
		if (rawPath.startsWith(REPOSITORY_URL)) {
			return rawPath.substring(REPOSITORY_URL.length());
		} else if (rawPath.startsWith("pathmap://")) {
			// we'd need the ResourceSet that the IHawkObject belongs to in order to undo the mapping
			return rawPath;
		}

		try {
			final URI uri = new URI(rawPath);
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
			if (files.length > 0) {
				String filePath = files[0].getFullPath().toString();
				if (uri.getFragment() == null) {
					return filePath;
				} else {
					return filePath + "#" + uri.getFragment();
				}
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Could not find file " + rawPath + " in the workspace");
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
}
