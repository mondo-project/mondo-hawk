/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Ossmeter team (https://opensourceprojects.eu/p/ossmeter) - SVN delta computation algorithm
 ******************************************************************************/
package org.hawk.svn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepository;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.util.DefaultConsole;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SvnManager extends AbstractVcsManager {

	private SvnRepository r;
	private IAbstractConsole console;

	private boolean isActive = false;

	private static String password() {
		final JFrame parent = new JFrame();
		parent.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		String s = JOptionPane.showInputDialog(parent, "pw plz", "hi there");
		parent.dispose();
		return s;
	}

	public SvnManager() {
	}

	private VcsRepository getRepository() {
		return r;
	}

	public static void main(String[] _) throws Exception {
		System.err.println("testing");
		String pass = password();
		System.err.println("testing2");
		SvnManager m = new SvnManager();
		System.err.println("testing3");
		m.run("https://cssvn.york.ac.uk/repos/sosym/kostas/Hawk/org.hawk.emf/src/org/hawk/emf/model/examples/single/0",
				"kb634", pass, null);
		System.err.println("testing4");
		m.test();
		System.err.println("testing5-end");
	}

	private void test() {
		try {
			console = new DefaultConsole();
			System.err.println("------------");
			System.err.println(getDelta(getRepository(), "0"));
			shutdown();
			System.err.println("------------");
			// System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run(String vcsloc, String un, String pw, IAbstractConsole c)
			throws Exception {

		try {

			console = c;

			r = new SvnRepository();
			r.setUrl(vcsloc);
			r.setUsername(un);
			r.setPassword(pw);
			pw = null;

			SvnManager m = new SvnManager();

			m.getFirstRevision(r);

			isActive = true;
			//
			// VcsRepositoryDelta d = m.getDelta(r, revision);
			//
			// for (VcsCommitItem i : d.getCompactedCommitItems())
			// System.err.println(i.toString());

		} catch (Exception e) {
			System.err.println("exception in svnmanager run():");
			e.printStackTrace();
		}

	}

	protected static SVNRepository getSVNRepository(SvnRepository repository) {
		SvnUtil.setupLibrary();
		SVNRepository svnRepository = SvnUtil.connectToSVNInstance(
				repository.getUrl(), repository.getUsername(),
				repository.getPassword());
		return svnRepository;
	}

	@Override
	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision, String endRevision) throws Exception {
		SvnRepository _svnRepository = (SvnRepository) repository;
		SVNRepository svnRepository = getSVNRepository(_svnRepository);

		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setRepository(repository);

		String userProviderURL = _svnRepository.getUrl();
		// System.err.println(_svnRepository.getUrl());
		String rootURL = svnRepository.getRepositoryRoot(false)
				.toDecodedString();
		// System.err.println(rootURL);

		String overLappedURL = makeRelative(rootURL, userProviderURL);
		// if (!overLappedURL.startsWith("/")) {
		// overLappedURL = "/" + overLappedURL;
		// }
		// System.err.println(overLappedURL);

		if (!startRevision.equals(endRevision)) {
			Collection<?> c = svnRepository.log(new String[] { "" }, null,
					Long.valueOf(startRevision), Long.valueOf(endRevision),
					true, true);

			// System.err.println(c);

			for (Object o : c) {

				SVNLogEntry svnLogEntry = (SVNLogEntry) o;

				VcsCommit commit = new VcsCommit();

				commit.setAuthor(svnLogEntry.getAuthor());
				commit.setMessage(svnLogEntry.getMessage());
				commit.setRevision(svnLogEntry.getRevision() + "");
				commit.setDelta(delta);
				commit.setJavaDate(svnLogEntry.getDate());
				delta.getCommits().add(commit);

				Map<String, SVNLogEntryPath> changedPaths = svnLogEntry
						.getChangedPaths();
				for (String path : changedPaths.keySet()) {
					SVNLogEntryPath svnLogEntryPath = changedPaths.get(path);

					// String[] exts =
					// {".cxx",".h",".hxx",".cpp",".cpp",".html"};

					// System.err.println(path);
					// if (svnLogEntryPath.getKind() == SVNNodeKind.FILE) {
					String[] blacklist = { ".png", ".jpg", ".bmp", ".zip",
							".jar", ".gz", ".tar" };

					if (path.lastIndexOf(".") <= 0)
						continue;
					String ext = path.substring(path.lastIndexOf("."),
							path.length());
					// System.err.println(ext + " in " + blacklist + " == " +
					// !Arrays.asList(blacklist).contains(ext));
					if (!Arrays.asList(blacklist).contains(ext)) {

						if (path.contains(overLappedURL)) {

							VcsCommitItem commitItem = new VcsCommitItem();
							commit.getItems().add(commitItem);
							commitItem.setCommit(commit);

							// XXX KOSTAS - removed starting / for Hawk + entire
							// relative making.
							// String relativePath = makeRelative(overLappedURL,
							// path);
							// if (!relativePath.startsWith("/")) {
							// relativePath = "/" + relativePath;
							// }
							// commitItem.setPath(relativePath);
							commitItem.setPath(path);

							if (svnLogEntryPath.getType() == 'A') {
								commitItem.setChangeType(VcsChangeType.ADDED);
							} else if (svnLogEntryPath.getType() == 'M') {
								commitItem.setChangeType(VcsChangeType.UPDATED);
							} else if (svnLogEntryPath.getType() == 'D') {
								commitItem.setChangeType(VcsChangeType.DELETED);
							} else if (svnLogEntryPath.getType() == 'R') {
								commitItem
										.setChangeType(VcsChangeType.REPLACED);
							} else {
								System.err
										.println("Found unrecognised svn log entry type: "
												+ svnLogEntryPath.getType());
								commitItem.setChangeType(VcsChangeType.UNKNOWN);
							}
						}
					}
				}
			}
		}

		for (VcsCommitItem c : delta.getCompactedCommitItems()) {
			System.out.println(c.getPath());
		}

		return delta;
	}

	// @Override
	// public String getContents(VcsCommitItem item) throws Exception {
	//
	// SVNRepository repository = getSVNRepository((SvnRepository) item
	// .getCommit().getDelta().getRepository());
	//
	// SVNProperties fileProperties = new SVNProperties();
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	//
	// repository.getFile(item.getPath(),
	// Long.valueOf(item.getCommit().getRevision()), fileProperties,
	// baos);
	//
	// // Store mimetype?
	// // Think about adding a notion of a filter
	// // String mimeType =
	// // fileProperties.getStringValue(SVNProperty.MIME_TYPE);
	//
	// // System.err.println("File being read from SVN: " + item.getPath());
	//
	// StringBuffer sb = new StringBuffer();
	// BufferedReader reader = new BufferedReader(new InputStreamReader(
	// new ByteArrayInputStream(baos.toByteArray())));
	// String line;
	// while ((line = reader.readLine()) != null) {
	// // Think about a platform-wide new line character
	// sb.append(line + "\r\n");
	// }
	// return sb.toString();
	// }

	@Override
	public String getCurrentRevision(VcsRepository repository) throws Exception {
		return getSVNRepository((SvnRepository) repository).getLatestRevision()
				+ "";
	}

	/**
	 * Cache the log?
	 */
	@Override
	public String getFirstRevision(VcsRepository repository) throws Exception {
		SVNRepository svnRepository = getSVNRepository((SvnRepository) repository);
		Collection<?> c = svnRepository.log(new String[] { "" }, null, 0,
				Long.valueOf(getCurrentRevision(repository)), true, true);

		for (Object o : c) {
			return String.valueOf(((SVNLogEntry) o).getRevision());
		}
		return null;
	}

	// @Override
	// public int compareVersions(VcsRepository repository, String versionOne,
	// String versionTwo) throws Exception {
	// return (Long.valueOf(versionOne).compareTo(Long.valueOf(versionTwo)));
	// }

	// /**
	// * Is there a more efficient implementation? (simple cache?)
	// */
	// @Override
	// public String[] getRevisionsForDate(VcsRepository repository, Date date)
	// throws Exception {
	// String[] revs = new String[2];
	//
	// SvnRepository _svnRepository = (SvnRepository) repository;
	// SVNRepository svnRepository = getSVNRepository(_svnRepository);
	//
	// Collection<?> c = svnRepository.log(new String[] { "" }, null, 0,
	// Long.valueOf(getCurrentRevision(repository)), true, true);
	// boolean foundStart = false;
	// SVNLogEntry svnLogEntry;
	//
	// for (Object o : c) {
	// svnLogEntry = (SVNLogEntry) o;
	// int dateComparison = date.compareTo(svnLogEntry.getDate());
	//
	// if (!foundStart && dateComparison == 0) {
	// revs[0] = String.valueOf(svnLogEntry.getRevision());
	// revs[1] = String.valueOf(svnLogEntry.getRevision());
	// foundStart = true;
	// } else if (foundStart && dateComparison == 0) {
	// revs[1] = String.valueOf(svnLogEntry.getRevision());
	// } else if (dateComparison < 0) { // Future
	// break;
	// }
	// }
	// System.out.println("SVN revisions: " + revs[0] + ", " + revs[1]);
	// return revs;
	// }
	//
	// /**
	// */
	// @Override
	// public Date getDateForRevision(VcsRepository repository, String revision)
	// throws Exception {
	// SvnRepository _svnRepository = (SvnRepository) repository;
	// SVNRepository svnRepository = getSVNRepository(_svnRepository);
	//
	// Collection<?> c = svnRepository.log(new String[] { "" }, null, 0,
	// Long.valueOf(getCurrentRevision(repository)), true, true);
	// SVNLogEntry svnLogEntry;
	//
	// for (Object o : c) {
	// svnLogEntry = (SVNLogEntry) o;
	// if (svnLogEntry.getRevision() == Long.valueOf(revision)) {
	// return new Date(svnLogEntry.getDate());
	// }
	// }
	// return null;
	// }

	private String makeRelative(String base, String extension) {
		StringBuilder result = new StringBuilder();
		List<String> baseSegments = Arrays.asList(base.split("/"));
		String[] extensionSegments = extension.split("/");
		for (String ext : extensionSegments) {
			if (!baseSegments.contains(ext)) {
				result.append(extension.substring(extension.indexOf(ext)));
				break;
			}
		}
		return result.toString();
	}

	@Override
	public void importFiles(String path, File temp) {
		SVNRepository svnRepository = getSVNRepository((SvnRepository) r);

		try {

			OutputStream o = new FileOutputStream(temp);

			svnRepository.getFile(path, SVNRevision.HEAD.getNumber(),
					new SVNProperties(), o);

			o.flush();
			o.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void shutdown() {

		r = null;
		console = null;

	}

	@Override
	public String getLocation() {
		return r.getUrl();
	}

	@Override
	public String getUn() {
		return r.getUsername();
	}

	@Override
	public String getPw() {
		return r.getPassword();
	}

	@Override
	public String getType() {
		return "org.hawk.svn.SvnManager";
	}

	@Override
	public String getHumanReadableName() {
		return "SVN Monitor";
	}

	@Override
	public String getCurrentRevision() throws Exception {
		return getCurrentRevision(r);
	}

	@Override
	public List<VcsCommitItem> getDelta(String string) throws Exception {
		if (Integer.parseInt(string) < 0)
			return getDelta(r, getFirstRevision(r)).getCompactedCommitItems();
		else
			return getDelta(r, string).getCompactedCommitItems();
	}
}
