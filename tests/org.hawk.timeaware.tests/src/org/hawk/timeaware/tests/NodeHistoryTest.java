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
package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.tmatesoft.svn.core.SVNException;

/**
 * Tests for the time-aware indexing of model element nodes.
 */
@RunWith(Parameterized.class)
public class NodeHistoryTest extends AbstractTimeAwareModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	Object[][] baseParams = BackendTestSuite.timeAwareBackends();

    	Object[][] params = new Object[baseParams.length][];
    	for (int i = 0; i < baseParams.length; i++) {
    		params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
    	}

    	return Arrays.asList(params);
    }

	public NodeHistoryTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void createDeleteNode() throws Throwable {
		twoCommitTree();
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// .all works on revision 0
				assertEquals(0, timeAwareEOL("return Tree.all.size;"));

				// We also deleted everything in the latest revision
				assertEquals(0, timeAwareEOL("return Tree.latest.all.size;"));

				// .created can return instances that have been created from a certain moment in time (even if not alive anymore)
				assertEquals(1, timeAwareEOL("return Tree.latest.prev.size;"));

				assertEquals("xy", timeAwareEOL("return Tree.latest.prev.all.first.label;"));

				return null;
			}
		});
	}

	private void twoCommitTree() throws IOException, SVNException, Exception {
		final File fTree = new File(svnRepository.getCheckoutDirectory(), "root.xmi");
		Resource rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree t = treeFactory.createTree();
		t.setLabel("xy");
		rTree.getContents().add(t);
		rTree.save(null);

		svnRepository.add(fTree);
		svnRepository.commit("First commit");
		svnRepository.remove(fTree);
		svnRepository.commit("Second commit - remove file");

		requestSVNIndex();
	}

	@Test
	public void countInstancesFromModelTypes() throws Throwable {
		twoCommitTree();
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').all.size;"));
				assertEquals(1, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').latest.prev.all.size;"));
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').latest.prev.prev.all.size;"));
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').earliest.all.size;"));
				return null;
			}
		});
	}

	@Test
	public void countInstancesFromModel() throws Throwable {
		twoCommitTree();
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').all.size;"));
				assertEquals(1, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').latest.prev.all.size;"));
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').latest.prev.prev.all.size;"));
				assertEquals(0, timeAwareEOL("return Model.types.selectOne(t|t.name='Tree').earliest.all.size;"));
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void countInstancesTimeline() throws Throwable {
		twoCommitTree();
		waitForSync(() -> {
			List<List<Object>> results = (List<List<Object>>) timelineEOL("return Tree.all.size;");
			assertEquals(0, results.get(0).get(1));
			assertEquals(1, results.get(1).get(1));
			assertEquals(0, results.get(2).get(1));
			return null;
		});
	}

	@Test
	public void countInstancesModelAll() throws Throwable {
		final File fTree = new File(svnRepository.getCheckoutDirectory(), "root.xmi");
		Resource rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree t = treeFactory.createTree();
		t.setLabel("xy");
		rTree.getContents().add(t);
		rTree.save(null);

		svnRepository.add(fTree);
		svnRepository.commit("First commit");
		requestSVNIndex();

		waitForSync(() -> {
			assertEquals(0, timeAwareEOL("return Model.allInstances.collect(t|t.label).size;"));
			assertEquals(0, timeAwareEOL("return Model.allInstances.size;"));
			assertEquals(1, timeAwareEOL("return Model.allInstancesNow.size;"));
			return null;
		});
	}

	private void requestSVNIndex() throws Exception {
		requestSVNIndex(svnRepository);
	}

	protected Object timeAwareEOL(final String eolQuery) throws InvalidQueryException, QueryExecutionException {
		return timeAwareEOL(eolQuery, null);
	}

	protected Object timeAwareEOL(final String eolQuery, Map<String, Object> context) throws InvalidQueryException, QueryExecutionException {
		return timeAwareQueryEngine.query(indexer, eolQuery, context);
	}

	protected Object timelineEOL(final String eolQuery) throws InvalidQueryException, QueryExecutionException {
		return timelineQueryEngine.query(indexer, eolQuery, null);
	}
}
