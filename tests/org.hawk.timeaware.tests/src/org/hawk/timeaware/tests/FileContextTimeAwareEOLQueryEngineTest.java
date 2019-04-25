/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 * Tests for time-aware queries that use additional context parameters, e.g.
 * repository/file paths.
 */
@RunWith(Parameterized.class)
public class FileContextTimeAwareEOLQueryEngineTest extends AbstractTimeAwareModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepositoryA = new TemporarySVNRepository();

	@Rule
	public TemporarySVNRepository svnRepositoryB = new TemporarySVNRepository();

	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		Object[][] baseParams = BackendTestSuite.timeAwareBackends();

		Object[][] params = new Object[baseParams.length][];
		for (int i = 0; i < baseParams.length; i++) {
			params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
		}

		return Arrays.asList(params);
	}

	public FileContextTimeAwareEOLQueryEngineTest(IGraphDatabaseFactory dbFactory,
			IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void testCounts() throws Throwable {
		final File fTreeA = new File(svnRepositoryA.getCheckoutDirectory(), "root.xmi");
		Resource rTreeA = rsTree.createResource(URI.createFileURI(fTreeA.getAbsolutePath()));

		// Do first the creation of a tree in the first SVN repository
		Tree a1 = treeFactory.createTree();
		a1.setLabel("A1");
		rTreeA.getContents().add(a1);
		rTreeA.save(null);
		svnRepositoryA.add(fTreeA);
		final SVNCommitInfo cInfoA1 = svnRepositoryA.commit("first in A");

		// Now do the same at least one second later, in the second SVN repository
		final long startMillis = System.currentTimeMillis();
		final File fTreeB = new File(svnRepositoryB.getCheckoutDirectory(), "main.xmi");
		Resource rTreeB = rsTree.createResource(URI.createFileURI(fTreeB.getAbsolutePath()));

		final long elapsed = System.currentTimeMillis() - startMillis;
		Thread.sleep(Math.max(0, 1000 - elapsed));
		Tree b1 = treeFactory.createTree();
		b1.setLabel("B1");
		rTreeB.getContents().add(b1);
		rTreeB.save(null);
		svnRepositoryB.add(fTreeB);
		final SVNCommitInfo cInfoB1 = svnRepositoryB.commit("first in B");

		// Run the SVN indexing now
		requestSVNIndex();

		// Sanity check: last version should include both trees
		assertEquals(2, timeAwareEOL("return Tree.latest.all.size;"));

		// Try asking only for one repository
		assertEquals(1, timeAwareEOLInRepository("return Tree.latest.all.size;", svnRepositoryA));
		assertEquals("A1", timeAwareEOLInRepository("return Tree.latest.all.first.label;", svnRepositoryA));
		assertEquals(1, timeAwareEOLInRepository("return Tree.latest.all.size;", svnRepositoryB));
		assertEquals("B1", timeAwareEOLInRepository("return Tree.latest.all.first.label;", svnRepositoryB));

		// TODO: add a context attribute for starting timepoint?
	}

	private void requestSVNIndex() throws Throwable {
		requestSVNIndex(svnRepositoryA);
		waitForSync();

		requestSVNIndex(svnRepositoryB);
		waitForSync();
	}
}
