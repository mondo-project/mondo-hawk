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
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
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

	private IAbstractConsole console;
	private Path rootLocation;
	private Set<File> previousFiles = new HashSet<>();
	private LocalFolderRepository repository;

	private static int version = 0;

	// Maximum 'last modified' time observed within the folder since the last time we checked
	private long lastModifiedRepository = 0;

	public LocalFolder() {

	}

	@Override
	public void run(String vcsloc, String un, String pw, IAbstractConsole c)
			throws Exception {
		console = c;
		Path path = Paths.get(vcsloc);
		rootLocation = path.toRealPath();
		repository = new LocalFolderRepository(URLDecoder.decode(rootLocation
				.toUri().toString().replace("+", "%2B"), "UTF-8")
		// rootLocation.toUri().toString()
		);
	}

	@Override
	public String getCurrentRevision(VcsRepository r) {
		// XXX as there is no implementation of top-level versions on a local
		// folder, every time this method is called a different value is
		// returned, hence the check is delegated to changes in each individual
		// file in the folder
		version++;
		return version + "";
	}

	@Override
	public String getCurrentRevision() {
		// XXX as there is no implementation of top-level versions on a local
		// folder, every time this method is called a different value is
		// returned, hence the check is delegated to changes in each individual
		// file in the folder
		version++;
		return version + "";
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
		return getDelta(repository, startRevision, getCurrentRevision());
	}

	@Override
	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision, String endRevision) throws Exception {

		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setLatestRevision(getCurrentRevision());
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

			c.setPath(makeRelative(repository.getUrl(), URLDecoder.decode(f
					.toPath().toUri().toString().replace("+", "%2B"), "UTF-8")));

			// c.setPath(rootLocation.relativize(Paths.get(f.getPath())).toString());
			commit.getItems().add(c);
		}

		previousFiles.clear();

		if (files != null && files.size() > 0) {
			long newLastModifiedRepository = lastModifiedRepository;
			for (File f : files) {
				previousFiles.add(f);
				final long lastModified = Files.getLastModifiedTime(f.toPath()).toMillis();
				if (lastModifiedRepository > lastModified) {
					continue;
				}
				newLastModifiedRepository = Math.max(newLastModifiedRepository, lastModified);

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
			lastModifiedRepository = newLastModifiedRepository;
		}

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
		return false;
	}

}
