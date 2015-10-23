/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.workspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

/**
 * Repository manager for an Eclipse workspace.
 */
public class Workspace implements IVcsManager {

	private final class WorkspaceDeltaVisitor implements IResourceDeltaVisitor {
		boolean anyChanges = false;

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (!(delta.getResource() instanceof IFile)) {
				// not a file: not interested
				return true;
			}
			anyChanges = true;
			return false;
		}
	}

	private class WorkspaceListener implements IResourceChangeListener {
		private IModelIndexer indexer;

		public WorkspaceListener(IModelIndexer indexer) {
			this.indexer = indexer;
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				IResourceDelta delta = event.getDelta();
				try {
					final WorkspaceDeltaVisitor visitor = new WorkspaceDeltaVisitor();
					delta.accept(visitor);
					if (!pendingChanges && visitor.anyChanges) {
						pendingChanges = true;
						indexer.requestImmediateSync();
					}
				} catch (Exception e) {
					console.printerrln(e);
				}
			}
		}
	}

	private long revision;
	private boolean pendingChanges = false;
	private IConsole console;
	private WorkspaceListener listener;
	private String repositoryURL;
	private Set<IFile> previousFiles = new HashSet<>();
	private Map<IFile, Long> recordedStamps = new HashMap<>();

	@Override
	public String getFirstRevision() throws Exception {
		return "0";
	}

	@Override
	public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {
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
			final Long latestRev = f.getModificationStamp();
			final Long lastRev = recordedStamps.get(f);
			if (lastRev != null && lastRev.equals(latestRev)) {
				if ((revision + "").equals(startRevision))
					continue;
			}
			recordedStamps.put(f, latestRev);
			addIFile(delta, f, VcsChangeType.UPDATED);
		}

		if (pendingChanges) {
			revision++;
			pendingChanges = false;
		}
		delta.setManager(this);
		delta.setLatestRevision(revision + "");

		return delta;
	}

	private Set<IFile> getAllFiles() {
		try {
			final Set<IFile> allFiles = new HashSet<>();
			ResourcesPlugin.getWorkspace().getRoot().accept(new IResourceVisitor(){
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						allFiles.add((IFile)resource);
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
		commit.setJavaDate(null);
		commit.setMessage("i am a workspace driver - no messages recorded");
		commit.setRevision(f.getModificationStamp() + "");
		delta.getCommits().add(commit);

		VcsCommitItem c = new VcsCommitItem();
		c.setChangeType(changeType);
		c.setCommit(commit);

		c.setPath(f.getFullPath().toString());

		commit.getItems().add(c);
	}

	@Override
	public void importFiles(String path, File temp) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		try {
			try (InputStream is = file.getContents()) {
				Files.copy(is, temp.toPath());
			}
		} catch (IOException | CoreException e) {
			console.printerrln(e);
		}
	}

	@Override
	public boolean isActive() {
		return listener != null;
	}

	@Override
	public void run(String vcsloc, IConsole c, IModelIndexer indexer) throws Exception {
		this.console = c;
		this.listener = new WorkspaceListener(indexer);

		// Needed to emulate the usual URLs within a workspace when concatenated with the file path
		this.repositoryURL = "platform:/resource";

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
		return repositoryURL	;
	}

	@Override
	public void setCredentials(String username, String password, ICredentialsStore credStore) {
		// ignore
	}

	@Override
	public String getType() {
		return Workspace.class.getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Workspace driver";
	}

	@Override
	public String getCurrentRevision() throws Exception {
		if (pendingChanges) {
			return (revision +  1) + "";
		} else {
			return revision + "";
		}
	}

	@Override
	public List<VcsCommitItem> getDelta(String string) throws Exception {
		return getDelta(string, getCurrentRevision()).getCompactedCommitItems();
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
		return false;
	}

	@Override
	public Set<String> getPrefixesToBeStripped() {
		return new HashSet<>(Arrays.asList("platform:/resource"));
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}

}
