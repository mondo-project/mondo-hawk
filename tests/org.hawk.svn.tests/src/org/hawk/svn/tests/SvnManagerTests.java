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
package org.hawk.svn.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hawk.core.ICredentialsStore;
import org.hawk.core.ICredentialsStore.Credentials;
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.util.DefaultConsole;
import org.hawk.svn.SvnManager;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;

/**
 * Tests for the {@link SvnManager} class.
 */
public class SvnManagerTests {

	private static final String DUMMY_PASS = "dummypass";
	private static final String DUMMY_USER = "dummyuser";

	@Rule public TemporarySVNRepository svn = new TemporarySVNRepository();

	private SvnManager vcs;
	private ICredentialsStore credStore;

	@Before
	public void setUp() throws Exception {
		credStore = mock(ICredentialsStore.class);
		when(credStore.get(anyString())).thenReturn(new Credentials(DUMMY_USER, DUMMY_PASS));

		IModelIndexer indexer = mock(IModelIndexer.class);
		when(indexer.getConsole()).thenReturn(new DefaultConsole());
		when(indexer.getCredentialsStore()).thenReturn(credStore);

		vcs = new SvnManager();
		vcs.init(svn.getRepositoryURL().toString(), indexer);
		vcs.run();

		assertTrue(vcs.isActive());
	}

	@After
	public void tearDown() {
		vcs.shutdown();
	}

	@Test
	public void frozen() {
		assertFalse(vcs.isFrozen());
		vcs.setFrozen(true);
		assertTrue(vcs.isFrozen());
		vcs.setFrozen(false);
		assertFalse(vcs.isFrozen());
	}

	@Test
	public void emptyHistory() throws Exception {
		assertEquals("0", vcs.getFirstRevision());
		assertEquals("0", vcs.getCurrentRevision());

		assertEquals(svn.getRepositoryURL().toString(), vcs.getLocation());
		assertEquals(DUMMY_USER, vcs.getUsername());
		assertEquals(DUMMY_PASS, vcs.getPassword());
	}

	@Test
	public void oneAdd() throws Exception {
		File testFile = svn.write("example", "test.txt").toFile();
		svn.add(testFile);
		svn.commit("initial commit");

		assertEquals("1", vcs.getCurrentRevision());
		assertEquals("0", vcs.getFirstRevision());

		Collection<VcsCommitItem> items = vcs.getDelta("0");
		assertEquals(1, items.size());
		VcsCommitItem item = items.iterator().next();
		assertEquals(VcsChangeType.ADDED, item.getChangeType());
		assertEquals("/" + testFile.getName(), item.getPath());

		// getDelta(X) gets all the changes strictly after revision X
		assertEquals(0, vcs.getDelta("1").size());
	}

	@Test
	public void compactedChanges() throws Exception {
		File testFile = svn.write("one more test", "my.xmi").toFile();
		svn.add(testFile);
		svn.commit("initial commit");
		svn.write("replacing contents", "my.xmi");
		svn.commit("rewrote my.xmi");
		svn.remove(testFile);
		svn.commit("deleted my.xmi");

		// Results are compacted - we end up with the last state (here, deleted)
		Collection<VcsCommitItem> items = vcs.getDelta("0");
		assertEquals(1, items.size());
		VcsCommitItem item = items.iterator().next();
		assertEquals(VcsChangeType.DELETED, item.getChangeType());

		// Check original log entries
		VcsRepositoryDelta delta = vcs.getDelta("1", "3");
		assertSame(vcs, delta.getManager());
		assertEquals(3, delta.getCommits().size());
		assertEquals(items, delta.getCompactedCommitItems());

		List<VcsCommit> commits = delta.getCommits();
		assertEquals(VcsChangeType.ADDED, commits.get(0).getItems().get(0).getChangeType());
		assertEquals(VcsChangeType.UPDATED, commits.get(1).getItems().get(0).getChangeType());
		assertEquals(VcsChangeType.DELETED, commits.get(2).getItems().get(0).getChangeType());
	}

	@Test
	public void noExtensionFilesAreIgnored() throws Exception {
		assertFilenameIsIgnored("ihavenoextension");
	}

	@Test
	public void photoFilesAreIgnored() throws Exception {
		assertFilenameIsIgnored("photo.jpg");
	}

	@Test
	public void startingDotFilesAreIgnored() throws Exception {
		assertFilenameIsIgnored(".gitignore");
	}

	@Test
	public void setCredentialsStoresUserPass() throws Exception {
		final String newUser = "anotherUser";
		final String newPass = "anotherPass";

		vcs.setCredentials(newUser, newPass, credStore);
		verify(credStore).put(
			svn.getRepositoryURL().toString(),
			new Credentials(newUser, newPass));
	}

	@Test
	public void repositoryPath() throws Exception {
		assertEquals("/test.txt",
			vcs.getRepositoryPath(svn.getRepositoryURL().toString() + "/test.txt"));
		assertEquals("/test.txt", vcs.getRepositoryPath("/test.txt"));
	}

	@Test
	public void importFileHEAD() throws Exception {
		File testFile = svn.write("example", "something.xmi").toFile();
		svn.add(testFile);
		svn.commit("initial commit");

		File fTempDir = Files.createTempDirectory("import").toFile();
		try {
			File fImported = vcs.importFile(null, "something.xmi", new File(fTempDir, "something.xmi"));
			assertEquals(Collections.singletonList("example"), Files.readAllLines(fImported.toPath()));
		} finally {
			fTempDir.delete();
		}
	}

	@Test
	public void importPreviousFile() throws Exception {
		File testFile = svn.write("example", "something.xmi").toFile();
		svn.add(testFile);
		svn.commit("initial commit");
		svn.write("changed", "something.xmi").toFile();
		svn.commit("second commit");

		File fTempDir = Files.createTempDirectory("import").toFile();
		try {
			File fImported = vcs.importFile("1", "something.xmi", new File(fTempDir, "something.xmi"));
			assertEquals(Collections.singletonList("example"), Files.readAllLines(fImported.toPath()));
			vcs.importFile("2", "something.xmi", new File(fTempDir, "something.xmi"));
			assertEquals(Collections.singletonList("changed"), Files.readAllLines(fImported.toPath()));
		} finally {
			fTempDir.delete();
		}
	}

	protected void assertFilenameIsIgnored(final String filename) throws IOException, SVNException, Exception {
		File testFile = svn.write("this should not be seen", filename).toFile();
		svn.add(testFile);
		svn.commit("initial commit");
		assertEquals(0, vcs.getDelta("0").size());
	}
}
