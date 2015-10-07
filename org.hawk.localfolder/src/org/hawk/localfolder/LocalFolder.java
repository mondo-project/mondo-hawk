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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepository;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.util.FileOperations;

public class LocalFolder implements IVcsManager {

	private final class LastModifiedFileVisitor implements FileVisitor<Path> {
		public boolean hasChanged = false;
		boolean alter;

		public LastModifiedFileVisitor(boolean alter) {
			this.alter = alter;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			long currentlatest = Files.getLastModifiedTime(dir).toMillis();
			final Long lastDate = recordedModifiedDates.get(dir);
			if (lastDate == null || !lastDate.equals(currentlatest)) {
				if (alter)
					recordedModifiedDates.put(dir, currentlatest);
				hasChanged = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			long currentlatest = Files.getLastModifiedTime(file).toMillis();
			final Long lastDate = recordedModifiedDates.get(file);
			if (lastDate == null || !lastDate.equals(currentlatest)) {
				if (alter)
					recordedModifiedDates.put(file, currentlatest);
				hasChanged = true;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc)
				throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	private IAbstractConsole console;
	private Path rootLocation;
	private Set<File> previousFiles = new HashSet<>();
	private LocalFolderRepository repository;

	private long currentRevision = 0;

	private Map<Path, Long> recordedModifiedDates = new HashMap<Path, Long>();

	public LocalFolder() {

	}

	@Override
	public void run(String vcsloc, String un, String pw, IAbstractConsole c)
			throws Exception {

		console = c;

		// Accept both regular paths and file:// URIs
		Path path;
		try {
			path = Paths.get(new URI(vcsloc));
		} catch (URISyntaxException | IllegalArgumentException ex) {
			path = Paths.get(vcsloc);
		}

		rootLocation = path.toFile().getCanonicalFile().toPath();
		String repositoryURI = rootLocation.toUri().toString();
		if (!repositoryURI.endsWith("/")) {
			repositoryURI += "/";
		}
		repository = new LocalFolderRepository(URLDecoder.decode(
				repositoryURI.replace("+", "%2B"), "UTF-8"));
	}

	@Override
	public String getCurrentRevision(VcsRepository r) {
		// XXX as there is no implementation of top-level versions on a local
		// folder, every time this method is called a different value is
		// returned, hence the check is delegated to changes in each individual
		// file in the folder
		return getCurrentRevision();
	}

	private String getCurrentRevision(boolean alter) {
		// as there is no implementation of top-level versions on a local
		// folder, every time this method is called a different value is
		// returned, if something has changed in the folder (or subfolders)

		try {
			final LastModifiedFileVisitor visitor = new LastModifiedFileVisitor(
					alter);
			Files.walkFileTree(rootLocation, visitor);
			long ret = visitor.hasChanged ? (currentRevision + 1)
					: currentRevision;
			if (alter)
				currentRevision = ret;
			//System.err.println(ret + " | " + alter);
			return ret + "";
		} catch (IOException ex) {
			ex.printStackTrace();
			return "0";
		}
	}

	@Override
	public String getCurrentRevision() {
		return getCurrentRevision(false);
	}

	@Override
	public void importFiles(String path, File temp) {
		try {
			FileOperations.copyFile(rootLocation.resolve(path).toFile(), temp);
		} catch (Exception e) {
			console.printerrln(e);
		}
	}

	public static void main(String[] a) throws Exception {

		File f = new File("C:/testfolder");

		System.out.println("Folder: " + f.getPath() + "\nContains:\n");

		for (VcsCommitItem r : new LocalFolder().getDelta("0"))
			System.out.println(r.getPath());

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
		return repository.getUrl();
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Local Folder Monitor";
	}

	@Override
	public String getUsername() {
		return "na";
	}

	@Override
	public String getPassword() {
		return "na";
	}

	@Override
	public void setCredentials(String username, String password) {
		// ignore
	}

	@Override
	public String getFirstRevision(VcsRepository repository) throws Exception {
		return "0";
	}

	@Override
	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision) throws Exception {
		return getDelta(repository, startRevision, getCurrentRevision(false));
	}

	@Override
	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision, String endRevision) throws Exception {

		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setRepository(repository);

		Set<File> files = new HashSet<>();
		addAllFiles(rootLocation.toFile(), files);
		previousFiles.removeAll(files);

		for (File f : previousFiles) {
			VcsCommit commit = new VcsCommit();
			commit.setAuthor("i am a local folder driver - no authors recorded");
			commit.setDelta(delta);
			commit.setJavaDate(null);
			commit.setMessage("i am a local folder driver - no messages recorded");
			commit.setRevision(f.lastModified() + "");
			delta.getCommits().add(commit);

			VcsCommitItem c = new VcsCommitItem();
			c.setChangeType(VcsChangeType.DELETED);
			c.setCommit(commit);

			Path path = f.toPath();

			c.setPath(makeRelative(repository.getUrl(), URLDecoder.decode(path
					.toUri().toString().replace("+", "%2B"), "UTF-8")));

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
				final long latestModified = Files.getLastModifiedTime(filePath)
						.toMillis();

				final Long lastDate = recordedModifiedDates.get(filePath);
				if (lastDate != null && lastDate.equals(latestModified)) {
					continue;
				}

				VcsCommit commit = new VcsCommit();
				commit.setAuthor("i am a local folder driver - no authors recorded");
				commit.setDelta(delta);
				commit.setJavaDate(null);
				commit.setMessage("i am a local folder driver - no messages recorded");
				commit.setRevision(f.lastModified() + "");
				delta.getCommits().add(commit);

				VcsCommitItem c = new VcsCommitItem();
				c.setChangeType(VcsChangeType.UPDATED);
				c.setCommit(commit);

				c.setPath(makeRelative(
						repository.getUrl(),
						URLDecoder.decode(f.toPath().toUri().toString()
								.replace("+", "%2B"), "UTF-8")));

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
	public List<VcsCommitItem> getDelta(String string) throws Exception {
		return getDelta(repository, string).getCompactedCommitItems();
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

}
