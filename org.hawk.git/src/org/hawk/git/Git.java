/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - use Java 7 Path instead of File+string processing
 ******************************************************************************/
package org.hawk.git;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;

public class Git implements IVcsManager {

	private static final String FIRST_REV = "0";

	private final class LastModifiedFileVisitor implements FileVisitor<Path> {

		public boolean hasChanged = false;
		boolean alter;

		public LastModifiedFileVisitor(boolean alter) {
			this.alter = alter;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			final File f = dir.toFile();
			final String currentlatest = getRevisionFromFileMetadata(f);
			final String lastRev = recordedModifiedDates.get(dir);
			if (lastRev == null || !lastRev.equals(currentlatest)) {
				if (alter)
					recordedModifiedDates.put(dir, currentlatest);
				hasChanged = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// skip .git folder to avoid overhead
			if (dir.getFileName().equals(GITMETADATAFOLDERNAME)) {
				// System.err.println("Git monitor (preVisitDirectory): skipping
				// .git folder");
				return FileVisitResult.SKIP_SUBTREE;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			final File f = file.toFile();
			final String currentlatest = getRevisionFromFileMetadata(f);
			final String lastRev = recordedModifiedDates.get(file);
			if (lastRev == null || !lastRev.equals(currentlatest)) {
				if (alter)
					recordedModifiedDates.put(file, currentlatest);
				hasChanged = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	private static final String GITMETADATAFOLDERNAME = ".git";

	private IConsole console;
	private Path rootLocation;
	private Set<File> previousFiles = new HashSet<>();

	private long currentRevision = 0;
	private Map<Path, String> recordedModifiedDates = new HashMap<>();
	private String repositoryURL;

	private boolean isFrozen = false;

	public Git() {

	}

	@Override
	public void init(String vcsloc, IModelIndexer indexer) throws Exception {

		console = indexer.getConsole();

		// Accept both regular paths and file:// URIs
		Path path;
		try {
			path = Paths.get(new URI(vcsloc));
		} catch (URISyntaxException | IllegalArgumentException ex) {
			ex.printStackTrace();
			path = Paths.get(vcsloc);
		}

		final File canonicalFile = path.toFile().getCanonicalFile();
		rootLocation = canonicalFile.toPath();
		String repositoryURI = rootLocation.toUri().toString();

		// If the file doesn't exist, it might be because this is a local folder in
		// a remote server - try to preserve the provided vcsloc as is. Otherwise,
		// if the server and the client use different operating systems we could end
		// up with an unusable URL in the server.
		if (canonicalFile.exists()) {
			repositoryURL = repositoryURI;
		} else {
			repositoryURL = vcsloc;
		}

		// dont decode it to ensure consistency with other managers
		// URLDecoder.decode(repositoryURI.replace("+", "%2B"), "UTF-8");
	}

	@Override
	public void run() { /* nothing */
	}

	private String getCurrentRevision(boolean alter) {

		try {
			final LastModifiedFileVisitor visitor = new LastModifiedFileVisitor(alter);
			Files.walkFileTree(rootLocation, visitor);
			long ret = visitor.hasChanged ? (currentRevision + 1) : currentRevision;
			if (alter)
				currentRevision = ret;
			// System.err.println(ret + " | " + alter);
			return ret + "";
		} catch (IOException ex) {
			ex.printStackTrace();
			return FIRST_REV;
		}
	}

	@Override
	public String getCurrentRevision() {
		return getCurrentRevision(false);
	}

	@Override
	public File importFiles(String p, File temp) {
		try {
			final String path = URLDecoder.decode(p.replace("+", "%2B"), "UTF-8");
			final Path resolvedPath = rootLocation.resolve(path.startsWith("/") ? path.replaceFirst("/", "") : path);
			return resolvedPath.toFile();
		} catch (Exception e) {
			console.printerrln(e);
			return null;
		}
	}

	@Override
	public boolean isActive() {
		return rootLocation == null ? false : rootLocation.toFile().exists();
	}

	@Override
	public void shutdown() {
		rootLocation = null;
	}

	@Override
	public String getLocation() {
		return repositoryURL;
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Git Monitor";
	}

	@Override
	public void setCredentials(String username, String password, ICredentialsStore credStore) {
		// ignore
	}

	@Override
	public String getFirstRevision() throws Exception {
		return FIRST_REV;
	}

	@Override
	public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {

		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setManager(this);

		Set<File> files = new HashSet<>();
		addAllFiles(rootLocation.toFile(), files);
		previousFiles.removeAll(files);

		for (File f : previousFiles) {
			VcsCommit commit = new VcsCommit();
			commit.setAuthor("i am a local git repo driver - no authors recorded");
			commit.setDelta(delta);
			commit.setJavaDate(null);
			commit.setMessage("i am a local git repo driver - no messages recorded");
			final String currentlatest = getRevisionFromFileMetadata(f);
			commit.setRevision(currentlatest);
			delta.getCommits().add(commit);

			VcsCommitItem c = new VcsCommitItem();
			c.setChangeType(VcsChangeType.DELETED);
			c.setCommit(commit);

			Path path = f.toPath();

			String relativepath = makeRelative(repositoryURL,
					// dont decode it to ensure consistency with other managers
					// URLDecoder.decode(
					f.toPath().toUri().toString()
			// .replace("+", "%2B"),
			// "UTF-8")
			);

			c.setPath(relativepath.startsWith("/") ? relativepath : "/" + relativepath);

			// c.setPath(rootLocation.relativize(Paths.get(f.getPath())).toString());
			commit.getItems().add(c);

			recordedModifiedDates.remove(path);

		}

		previousFiles.clear();

		if (files != null && files.size() > 0) {
			// long newLastModifiedRepository = lastModifiedRepository;
			for (File f : files) {
				previousFiles.add(f);
				Path filePath = f.toPath();
				final String latestRev = getRevisionFromFileMetadata(f);
				final String lastDate = recordedModifiedDates.get(filePath);
				if (lastDate != null && lastDate.equals(latestRev)) {
					if ((currentRevision + "").equals(startRevision))
						continue;
				}

				VcsCommit commit = new VcsCommit();
				commit.setAuthor("i am a local git repo driver - no authors recorded");
				commit.setDelta(delta);
				commit.setJavaDate(null);
				commit.setMessage("i am a local git repo driver - no messages recorded");
				commit.setRevision(latestRev);
				delta.getCommits().add(commit);

				VcsCommitItem c = new VcsCommitItem();
				c.setChangeType(VcsChangeType.UPDATED);
				c.setCommit(commit);

				String relativepath = makeRelative(repositoryURL,
						// URLDecoder.decode(
						f.toPath().toUri().toString()
				// .replace("+", "%2B"), "UTF-8")
				);

				c.setPath(relativepath.startsWith("/") ? relativepath : ("/" + relativepath));

				// c.setPath(rootLocation.relativize(Paths.get(f.getPath())).toString());
				commit.getItems().add(c);
			}

		}
		delta.setLatestRevision(getCurrentRevision(true));

		return delta;
	}

	private String makeRelative(String base, String extension) {

		// System.err.println(">>"+base);
		// System.err.println("<>"+extension);

		if (!extension.startsWith(base))
			return extension;

		String ret = extension.substring(base.length());

		return ret;

	}

	private void addAllFiles(File dir, Set<File> ret) {
		// skip .git folder to avoid overhead
		if (dir.getName().equals(GITMETADATAFOLDERNAME)) {
			// System.err.println("Git monitor (addAllFiles): skipping .git
			// folder");
			return;
		}
		File[] files = dir.listFiles();
		if (files == null) {
			// couldn't list files in that directory
			console.printerrln("Could not list the entries of " + dir);
			return;
		}
		for (File file : files) {
			if (!file.isDirectory()) {
				ret.add(file);
			} else {
				addAllFiles(file, ret);
			}
		}
	}

	@Override
	public Collection<VcsCommitItem> getDelta(String endRevision) throws Exception {
		return getDelta(FIRST_REV, endRevision).getCompactedCommitItems();
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

	private String getRevisionFromFileMetadata(final File f) {
		return f.lastModified() + "-" + f.length();
	}

	@Override
	public Set<String> getPrefixesToBeStripped() {
		return Collections.emptySet();
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
