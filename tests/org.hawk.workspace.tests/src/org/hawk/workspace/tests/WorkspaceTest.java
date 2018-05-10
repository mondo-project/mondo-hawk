/*******************************************************************************
 * Copyright (c) 2017-2018 Aston University.
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
package org.hawk.workspace.tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.util.DefaultConsole;
import org.hawk.workspace.LocalHistoryWorkspace;
import org.hawk.workspace.Workspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit plugin test suite for workspace indexing in Hawk.
 */
@RunWith(Parameterized.class)
public class WorkspaceTest {

	private final Supplier<Workspace> supplier;
	private IWorkspaceRoot root;
	private IWorkspace workspace;
	private Workspace vcs;

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ (Supplier<Workspace>) Workspace::new },
			{ (Supplier<Workspace>) LocalHistoryWorkspace::new }
		});
	}

	public WorkspaceTest(Supplier<Workspace> supplier) {
		this.supplier = supplier;
	}

	@Before
	public void setup() {
		this.workspace = ResourcesPlugin.getWorkspace();
		this.root = workspace.getRoot();
	}

	@After
	public void teardown() throws Exception {
		if (vcs != null) {
			vcs.shutdown();
		}
		for (IProject proj : root.getProjects()) {
			File fProject = proj.getLocation().toFile();
			proj.delete(IProject.FORCE, null);
			FileUtils.deleteDirectory(fProject);
		}
	}

	@Test
	public void existingProjectIsListed() throws Exception {
		final IProject project = createProject("myproject");
		createFile(project, "my.xmi", "something");
		Workspace vcs = createVCS();

		// No changes detected yet
		assertEquals("0", vcs.getCurrentRevision());
		assertEquals("0", vcs.getFirstRevision());

		// All files are listed
		final Collection<VcsCommitItem> items = vcs.getDelta(null);
		assertEquals(2, items.size());
		final Iterator<VcsCommitItem> itItems = items.iterator();
		assertEquals("/myproject/.project", itItems.next().getPath());
		assertEquals("/myproject/my.xmi", itItems.next().getPath());
	}

	@Test
	public void newProject() throws Exception {
		Workspace vcs = createVCS();
		assertEquals(0, vcs.getDelta(null).size());

		IProject project = createProject("newproject");
		createFile(project, "ab.xmi", "contents");

		assertEquals(2, vcs.getDelta(null).size());
	}

	@Test
	public void deleteProject() throws Exception {
		Workspace vcs = createVCS();

		IProject project = createProject("tobedeleted");
		createFile(project, "bc.xmi", "xyz");
		assertEquals("1", vcs.getCurrentRevision());
		assertEquals(2, vcs.getDelta(null).size());

		assertEquals(0, vcs.getDelta(null).size());
		assertEquals("1", vcs.getCurrentRevision());

		project.delete(IProject.FORCE, null);
		Collection<VcsCommitItem> itemsAfterDelete = vcs.getDelta(null);
		assertEquals(2, itemsAfterDelete.size());
		for (VcsCommitItem item : itemsAfterDelete) {
			assertEquals(VcsChangeType.DELETED, item.getChangeType());
		}
	}

	@Test
	public void openProject() throws Exception {
		IProject project = createProject("tobeopened");
		createFile(project, "closed.xmi", "content");
		project.close(null);

		Workspace vcs = createVCS();
		assertEquals(0, vcs.getDelta(null).size());
		assertEquals("0", vcs.getCurrentRevision());

		project.open(null);
		final Collection<VcsCommitItem> delta = vcs.getDelta(null);
		assertEquals(2, delta.size());
		assertEquals("1", vcs.getCurrentRevision());
		for (VcsCommitItem item : delta) {
			assertEquals(VcsChangeType.ADDED, item.getChangeType());
		}
	}

	@Test
	public void closeProject() throws Exception {
		IProject project = createProject("tobeclosed");
		createFile(project, "closed.xmi", "content");

		Workspace vcs = createVCS();
		assertEquals(2, vcs.getDelta(null).size());
		assertEquals("0", vcs.getCurrentRevision());

		project.close(null);
		final Collection<VcsCommitItem> delta = vcs.getDelta(null);
		assertEquals(2, delta.size());
		assertEquals("1", vcs.getCurrentRevision());
		for (VcsCommitItem item : delta) {
			assertEquals(VcsChangeType.DELETED, item.getChangeType());
		}
	}

	@Test
	public void changeFile() throws Exception {
		Workspace vcs = createVCS();

		IProject project = createProject("tobechanged");
		IFile file = createFile(project, "iwillchange.xmi", "oldcontents");
		assertEquals("1", vcs.getCurrentRevision());
		assertEquals(2, vcs.getDelta(null).size());
		assertEquals(0, vcs.getDelta(null).size());

		file.setContents(new ByteArrayInputStream("newcontents".getBytes()), IFile.FORCE | IFile.KEEP_HISTORY, null);
		assertEquals("2", vcs.getCurrentRevision());

		Collection<VcsCommitItem> itemsAfterUpdate = vcs.getDelta(null);
		assertEquals(1, itemsAfterUpdate.size());
		VcsCommitItem item = itemsAfterUpdate.iterator().next();
		assertEquals(VcsChangeType.UPDATED, item.getChangeType());
		assertEquals("/tobechanged/iwillchange.xmi", item.getPath());
	}

	@Test
	public void importProject() throws Exception {
		IProject project = createProject("tobeimported");
		createFile(project, "toimport.xmi", "imported");
		IPath projectPath = project.getLocation();
		project.close(null);
		project.delete(false, null);

		Workspace vcs = createVCS();
		assertEquals(0, vcs.getDelta(null).size());
		assertEquals("0", vcs.getCurrentRevision());

		IProjectDescription desc = workspace.loadProjectDescription(projectPath.append(".project"));
		IProject importedProject = root.getProject(desc.getName());
		importedProject.create(desc, null);
		importedProject.open(null);

		Collection<VcsCommitItem> delta = vcs.getDelta(null);
		assertEquals(2, delta.size());
		for (VcsCommitItem item : delta) {
			assertEquals(VcsChangeType.ADDED, item.getChangeType());
		}
	}

	protected Workspace createVCS() throws Exception {
		final IModelIndexer indexer = mock(IModelIndexer.class);
		when(indexer.getConsole()).thenReturn(new DefaultConsole());

		vcs = supplier.get();
		vcs.init(null, indexer);
		vcs.run();
		return vcs;
	}

	protected IFile createFile(IContainer container, final String filename, final String content) throws CoreException {
		IFile file = container.getFile(new Path(filename));
		file.create(new ByteArrayInputStream(content.getBytes()), IResource.NONE, null);
		return file;
	}

	protected IProject createProject(final String name) throws CoreException {
		IProject project = root.getProject(name);
		project.create(null);
		project.open(null);
		return project;
	}
}
