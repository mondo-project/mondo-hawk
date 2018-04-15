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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.junit.Rule;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNLogEntry;

/**
 * Tests for the {@link TemporarySVNRepository} JUnit 4 rule.
 */
public class TemporarySVNRepositoryTests {

	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	@Test
	public void initWorks() {
		System.out.println("Created repository in " + svnRepository.getRepositoryDirectory() + ", checkout in "
				+ svnRepository.getCheckoutDirectory());
		assertNotNull(svnRepository.getRepository().getLocation());
	}

	@Test
	public void addRemove() throws Exception {
		File firstFile = svnRepository.write("this is a test", "test.txt").toFile();

		// Add a file and commit
		svnRepository.add(firstFile);
		SVNCommitInfo ciInfo = svnRepository.commit("initial commit");
		assertEquals(1, ciInfo.getNewRevision());
		List<SVNLogEntry> info = svnRepository.log(ciInfo.getNewRevision(), ciInfo.getNewRevision());
		assertEquals(1, info.size());
		assertEquals(1, info.get(0).getChangedPaths().size());

		// Remove a file and commit
		svnRepository.remove(firstFile);
		svnRepository.commit("remove file");
		assertFalse("The file should have been deleted after the update", firstFile.exists());
	}
}
