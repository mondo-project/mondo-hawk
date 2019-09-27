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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - use Java 7 Path instead of File+string processing,
 *       use MapDB, refactor into shared part with LocalFile, add file filtering
 ******************************************************************************/
package org.hawk.localfolder;

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
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * VCS manager that watches over the contents of a directory, including its
 * subdirectories.
 */
public class LocalFolder extends FileBasedLocation {

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
			final String lastRev = recordedModifiedDates.get(dir.toString());
			if (lastRev == null || !lastRev.equals(currentlatest)) {
				if (alter)
					recordedModifiedDates.put(dir.toString(), currentlatest);
				hasChanged = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (isFileInteresting(dir.toFile())) {
				return FileVisitResult.CONTINUE;
			} else {
				return FileVisitResult.SKIP_SUBTREE;
			}
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			final File f = file.toFile();
			if (isFileInteresting(f)) {
				final String currentlatest = getRevisionFromFileMetadata(f);
				final String lastRev = recordedModifiedDates.get(file.toString());
				if (lastRev == null || !lastRev.equals(currentlatest)) {
					if (alter) {
						recordedModifiedDates.put(file.toString(), currentlatest);
					}
					hasChanged = true;
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	private Path rootLocation;
	private long currentRevision = 0;
	private Set<File> previousFiles;
	private Map<String, String> recordedModifiedDates;
	private Function<File, Boolean> fileFilter;

	/**
	 * MapDB database: using file-backed Java collections allows us to save memory
	 * when handling folders with a large number of files.
	 */
	private DB db;

	@Override
	public void init(String vcsloc, IModelIndexer indexer) throws Exception {
		final File fMapDB = File.createTempFile("localfolder", "mapdb");
		db = DBMaker.newFileDB(fMapDB).deleteFilesAfterClose().closeOnJvmShutdown().make();
		previousFiles = db.createHashSet("previousFiles").make();
		recordedModifiedDates = db.createHashMap("recordedModifiedDates").make();

		console = indexer.getConsole();

		// Accept both regular paths and file:// URIs
		Path path;
		try {
			path = Paths.get(new URI(vcsloc));
		} catch (URISyntaxException | IllegalArgumentException ex) {
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

	protected String getCurrentRevision(boolean alter) {
		try {
			final LastModifiedFileVisitor visitor = new LastModifiedFileVisitor(alter);
			Files.walkFileTree(rootLocation, visitor);
			long ret = visitor.hasChanged ? (currentRevision + 1) : currentRevision;
			if (alter) {
				currentRevision = ret;
				
			}
			return ret + "";
		} catch (IOException ex) {
			console.printerrln(ex);
			return FIRST_REV;
		}
	}

	@Override
	public File importFile(String revision, String p, File temp) {
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
		if (!db.isClosed()) {
			db.close();
		}
	}

	@Override
	public String getHumanReadableName() {
		return "Local Folder Monitor";
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
			commit.setAuthor("i am a local folder driver - no authors recorded");
			commit.setDelta(delta);
			commit.setJavaDate(new Date());
			commit.setMessage("i am a local folder driver - no messages recorded");
			final String currentlatest = getRevisionFromFileMetadata(f);
			commit.setRevision(currentlatest);
			delta.getCommits().add(commit);

			VcsCommitItem c = new VcsCommitItem();
			c.setChangeType(VcsChangeType.DELETED);
			c.setCommit(commit);

			Path path = f.toPath();

			// Don't decode it to ensure consistency with other managers
			String relativepath = makeRelative(repositoryURL,
					f.toPath().toUri().toString()
			);

			c.setPath(relativepath.startsWith("/") ? relativepath : "/" + relativepath);
			commit.getItems().add(c);
			recordedModifiedDates.remove(path.toString());
		}

		previousFiles.clear();

		if (files != null && files.size() > 0) {
			// long newLastModifiedRepository = lastModifiedRepository;
			for (File f : files) {
				previousFiles.add(f);
				Path filePath = f.toPath();
				final String latestRev = getRevisionFromFileMetadata(f);
				final String lastDate = recordedModifiedDates.get(filePath.toString());
				if (lastDate != null && lastDate.equals(latestRev)) {
					if ((currentRevision + "").equals(startRevision))
						continue;
				}

				VcsCommit commit = new VcsCommit();
				commit.setAuthor("i am a local folder driver - no authors recorded");
				commit.setDelta(delta);
				commit.setJavaDate(new Date());
				commit.setMessage("i am a local folder driver - no messages recorded");
				commit.setRevision(latestRev);
				delta.getCommits().add(commit);

				VcsCommitItem c = new VcsCommitItem();
				c.setChangeType(VcsChangeType.UPDATED);
				c.setCommit(commit);

				String relativepath = makeRelative(repositoryURL, f.toPath().toUri().toString());
				c.setPath(relativepath.startsWith("/") ? relativepath : ("/" + relativepath));
				commit.getItems().add(c);
			}

			// Update the latest revision seen
			getCurrentRevision(true);
		}

		return delta;
	}

	public Function<File, Boolean> getFileFilter() {
		return fileFilter;
	}

	public void setFileFilter(Function<File, Boolean> fileFilter) {
		this.fileFilter = fileFilter;
	}

	protected void addAllFiles(File dir, Set<File> ret) {
		File[] files = dir.listFiles();
		if (files == null) {
			// couldn't list files in that directory
			console.printerrln("Could not list the entries of " + dir);
			return;
		}
		for (File file : files) {
			if (isFileInteresting(file)) {
				if (!file.isDirectory()) {
					ret.add(file);
				} else {
					addAllFiles(file, ret);
				}
			}
		}
	}

	private String getRevisionFromFileMetadata(final File f) {
		return f.lastModified() + "-" + f.length();
	}

	private boolean isFileInteresting(File f) {
		return fileFilter == null || fileFilter.apply(f);
	}
}
