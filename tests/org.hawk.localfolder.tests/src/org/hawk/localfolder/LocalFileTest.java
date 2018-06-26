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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.localfolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.util.DefaultConsole;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalFileTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File mainFile;

	private LocalFile vcs;

	@Before
	public void setup() throws Exception {
		mainFile = folder.newFile("main.xmi");
		write("Hello!");

		final IModelIndexer indexer = mock(IModelIndexer.class);
		when(indexer.getConsole()).thenReturn(new DefaultConsole());

		vcs = new LocalFile();
		vcs.init(mainFile.toPath().toUri().toString(), indexer);
		vcs.run();
	}

	protected void write(final String text) throws FileNotFoundException {
		try (PrintWriter pw = new PrintWriter(mainFile)) {
			pw.println(text);
		}
	}

	@Test
	public void initialVersion() throws Exception {
		assertEquals(LocalFile.FIRST_REV, vcs.getCurrentRevision());

		List<VcsCommitItem> delta = vcs.getDelta(null);
		assertEquals(1, delta.size());
		final VcsCommitItem vcsCommitItem = delta.get(0);
		assertNotNull(vcsCommitItem.getCommit().getRevision());
		assertEquals("/"+ mainFile.getName(), vcsCommitItem.getPath());
		assertEquals(VcsChangeType.ADDED, vcsCommitItem.getChangeType());

		assertEquals(0, vcs.getDelta(null).size());
	}

	@Test
	public void laterVersion() throws Exception {
		vcs.getDelta(null);

		write("somethingelse");
		List<VcsCommitItem> delta = vcs.getDelta(null);
		assertEquals(1, delta.size());
		assertEquals(VcsChangeType.UPDATED, delta.get(0).getChangeType());

		assertEquals(0, vcs.getDelta(null).size());
	}
		
}
