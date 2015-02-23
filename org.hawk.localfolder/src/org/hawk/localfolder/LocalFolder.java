/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.localfolder;

import java.io.File;
import java.util.HashSet;
import java.util.List;

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

	private String type;
	private String hrn;

	private String loc;

	private HashSet<File> cashedFiles = new HashSet<>();

	private static int version = 0;

	public LocalFolder() {

	}

	@Override
	public void run(String vcsloc, String un, String pw, IAbstractConsole c)
			throws Exception {

		type = "org.hawk.localfolder.LocalFolder";
		hrn = "Local Folder Monitor";

		console = c;
		loc = vcsloc;

	}

	public void sysout(String s) {

		// System.out.println(s);

		console.println(s);

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
			// System.err.println(path);
			// System.err.println(temp);
			FileOperations.copyFile(new File(loc + "/" + path), temp);

		} catch (Exception e) {
			e.printStackTrace();
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
		// System.err.println(loc);
		return loc == null ? false : new File(loc).exists();
	}

	@Override
	public void shutdown() {

		loc = null;

	}

	@Override
	public String getLocation() {
		return loc.toString();
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getHumanReadableName() {
		return hrn;
	}

	@Override
	public String getUn() {
		return null;
	}

	@Override
	public String getPw() {

		return null;

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

		// System.err.println("cashed files = "+cashedFiles);

		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setLatestRevision(getCurrentRevision());
		delta.setRepository(repository);

		//

		File root = new File(loc);

		HashSet<File> files = allFiles(root);

		// System.err.println("files = "+files);

		//
		cashedFiles.removeAll(files);

		for (File f : cashedFiles) {

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
			c.setPath(f.getPath().replaceAll("\\\\", "/")
					.replaceAll(loc.replaceAll("\\\\", "/"), "").substring(1));
			commit.getItems().add(c);

		}

		cashedFiles.clear();

		if (files != null && files.size() > 0)
			for (File f : files) {

				cashedFiles.add(f);

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
				c.setPath(f.getPath().replaceAll("\\\\", "/")
						.replaceAll(loc.replaceAll("\\\\", "/"), "")
						.substring(1));
				commit.getItems().add(c);

			}

		return delta;
	}

	private HashSet<File> allFiles(File dir) {

		HashSet<File> ret = new HashSet<>();

		File[] files = dir.listFiles();

		for (File file : files) {
			if (!file.isDirectory())
				ret.add(file);
			else
				ret.addAll(allFiles(file));
		}

		return ret;
	}

	@Override
	public List<VcsCommitItem> getDelta(String string) throws Exception {
		return getDelta(null, string).getCompactedCommitItems();
	}

}
