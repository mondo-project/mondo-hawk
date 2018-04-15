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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.svn.tests.rules;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnLog;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCreate;

/**
 * JUnit 4 rule for creating a SVN repository and a working copy before each
 * test, then deleting them during teardown. Includes additional methods for
 * managing the files, checking the log and creating commits.
 */
public class TemporarySVNRepository extends ExternalResource {

	private File fSvnRepoDir, fSvnCheckoutDir;
	private SvnOperationFactory svnOperationFactory;
	private SVNClientManager svnClientManager;
	private SVNRepository svnRepository;
	private SVNURL svnRepoURL;

	public File getRepositoryDirectory() {
		return fSvnRepoDir;
	}

	public File getCheckoutDirectory() {
		return fSvnCheckoutDir;
	}

	public SVNRepository getRepository() {
		return svnRepository;
	}

	public SVNURL getRepositoryURL() {
		return svnRepoURL;
	}

	/**
	 * Convenience version of {@link #write(InputStream, Path)}.
	 */
	public Path write(String text, String firstPathComponent, String... otherComponents) throws IOException {
		return write(new ByteArrayInputStream(text.getBytes()), Paths.get(firstPathComponent, otherComponents));
	}

	/**
	 * Convenience version of {@link #write(InputStream, Path)}.
	 */
	public Path write(InputStream is, String firstPathComponent, String... otherComponents) throws IOException {
		return write(is, Paths.get(firstPathComponent, otherComponents));
	}

	/**
	 * Writes a file in a certain path of the working copy.
	 * @return Absolute path of the file in the file system.
	 */
	public Path write(InputStream is, Path pathWithinWC) throws IOException {
		final Path destinationPath = fSvnCheckoutDir.toPath().resolve(pathWithinWC);
		Files.copy(is, destinationPath, StandardCopyOption.REPLACE_EXISTING);
		return destinationPath;
	}

	/**
	 * Schedules files for addition to the next commit. The files should be in the
	 * checkout directory. Parent files will be added as needed.
	 * 
	 * @throws SVNException
	 *             Failed to add a file.
	 */
	public void add(File... files) throws SVNException {
		SvnScheduleForAddition svnAdd = svnOperationFactory.createScheduleForAddition();
		svnAdd.setAddParents(true);
		for (File f : files) {
			svnAdd.addTarget(SvnTarget.fromFile(f));
		}

		svnAdd.run();
	}

	/**
	 * Schedules files for removal in the next commit. The files should be in the
	 * checkout directory.
	 * 
	 * @throws SVNException
	 *             Failed to remove a file.
	 */
	public void remove(File... files) throws SVNException {
		SvnScheduleForRemoval svnRm = svnOperationFactory.createScheduleForRemoval();
		svnRm.setDeleteFiles(true);
		for (File f : files) {
			svnRm.addTarget(SvnTarget.fromFile(f));
		}
		svnRm.run();
	}

	/**
	 * Creates a new commit in this repository.
	 *
	 * @return Information about the newly created commit.
	 * @throws SVNException
	 *             Failed to perform the commit.
	 */
	public SVNCommitInfo commit(String msg) throws SVNException {
		final SvnTarget svnTarget = SvnTarget.fromFile(fSvnCheckoutDir);

		SvnCommit svnCommit = svnOperationFactory.createCommit();
		svnCommit.setSingleTarget(svnTarget);
		svnCommit.setCommitMessage(msg);
		SVNCommitInfo ciInfo = svnCommit.run();

		// We need to do 'svn up' or log(...) will not return the expected results
		SvnUpdate svnUp = svnOperationFactory.createUpdate();
		svnUp.setSingleTarget(svnTarget);
		svnUp.run();

		return ciInfo;
	}

	/**
	 * Returns the log entry for a specific revision number.
	 */
	public List<SVNLogEntry> log(long from, long to) throws SVNException {
		final SvnRevisionRange svnRevisionRange = SvnRevisionRange.create(SVNRevision.create(from),
				SVNRevision.create(to));
		final SvnLog svnLog = svnOperationFactory.createLog();
		svnLog.setRevisionRanges(Collections.singleton(svnRevisionRange));
		svnLog.setSingleTarget(SvnTarget.fromFile(fSvnCheckoutDir));
		svnLog.setDiscoverChangedPaths(true);
		svnLog.setLimit(1);

		final List<SVNLogEntry> logEntries = new ArrayList<>();
		svnLog.run(logEntries);
		return logEntries;
	}

	@Override
	protected void before() throws Throwable {
		fSvnRepoDir = Files.createTempDirectory("hawkSvnRepo").toFile();
		fSvnCheckoutDir = Files.createTempDirectory("hawkSvnCo").toFile();
		svnOperationFactory = new SvnOperationFactory();
		svnClientManager = SVNClientManager.newInstance();

		SvnRepositoryCreate svnCreate = svnOperationFactory.createRepositoryCreate();
		svnCreate.setRepositoryRoot(fSvnRepoDir);
		svnRepoURL = svnCreate.run();

		SvnCheckout svnCheckout = svnOperationFactory.createCheckout();
		svnCheckout.setSource(SvnTarget.fromURL(svnRepoURL));
		svnCheckout.setSingleTarget(SvnTarget.fromFile(fSvnCheckoutDir));
		svnCheckout.run();

		svnRepository = svnClientManager.createRepository(SVNURL.fromFile(fSvnCheckoutDir), true);
	}

	@Override
	protected void after() {
		try {
			svnRepository.closeSession();
			svnClientManager.dispose();
			svnOperationFactory.dispose();
			deleteRecursively(fSvnRepoDir);
			deleteRecursively(fSvnCheckoutDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists())
			return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
