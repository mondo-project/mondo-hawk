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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
	public void travelToMissingTimepointReturnsNull() throws Throwable {
		twoCommitTree();
		waitForSync(() -> {
			GraphNodeWrapper gnw = (GraphNodeWrapper) timeAwareEOL("return Tree.latest.prev.all.first;");
			assertNotNull(gnw.getNode());
			assertNull(((ITimeAwareGraphNode) gnw.getNode()).travelInTime(ITimeAwareGraphNode.NO_SUCH_INSTANT));
			return null;
		});
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

	private void twoCommitTree() throws Exception {
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

	private Tree keepAddingChildren() throws Exception {
		final File fTree = new File(svnRepository.getCheckoutDirectory(), "m.xmi");
		Resource rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree tRoot = treeFactory.createTree();
		tRoot.setLabel("Root");
		rTree.getContents().add(tRoot);
		rTree.save(null);
		svnRepository.add(fTree);
		svnRepository.commit("Create root");

		for (String childLabel : Arrays.asList("T1", "T2", "T3")) {
			Tree t1 = treeFactory.createTree();
			t1.setLabel(childLabel);
			tRoot.getChildren().add(t1);
			rTree.save(null);
			svnRepository.commit("Add " + childLabel);
		}

		requestSVNIndex();
		return tRoot;
	}

	@Test
	public void rangesAreBothInclusive() throws Throwable {
		keepAddingChildren();
		waitForSync(() -> {
			GraphNodeWrapper gnw = (GraphNodeWrapper) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.latest.label = 'Root');"
			);

			ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) gnw.getNode();
			final long earliestInstant = taNode.getEarliestInstant();
			final long latestInstant = taNode.getLatestInstant();

			final List<ITimeAwareGraphNode> allVersions = taNode.getAllVersions();
			assertEquals(earliestInstant, allVersions.get(allVersions.size() - 1).getTime());
			assertEquals(latestInstant, allVersions.get(0).getTime());

			final List<ITimeAwareGraphNode> versionsUpTo = taNode.getVersionsUpTo(latestInstant);
			assertEquals(earliestInstant, versionsUpTo.get(versionsUpTo.size() - 1).getTime());
			assertEquals(latestInstant, versionsUpTo.get(0).getTime());

			final List<ITimeAwareGraphNode> versionsFrom = taNode.getVersionsFrom(earliestInstant);
			assertEquals(earliestInstant, versionsFrom.get(versionsFrom.size() - 1).getTime());
			assertEquals(latestInstant, versionsFrom.get(0).getTime());
	
			final List<ITimeAwareGraphNode> versionsBW = taNode.getVersionsBetween(earliestInstant, latestInstant);
			assertEquals(earliestInstant, versionsBW.get(versionsFrom.size() - 1).getTime());
			assertEquals(latestInstant, versionsBW.get(0).getTime());
			
			return null;
		});

	}

	@Test
	public void alwaysTrue() throws Throwable {
		keepAddingChildren();
		waitForSync(() -> {
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').always(v|v.label = 'Root');"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').never(v|v.label <> 'Root');"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventually(v|v.children.size > 2);"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventually(v|v.children.size > 3);"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventuallyAtMost(v | v.children.size > 2, 2);"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventuallyAtMost(v | v.children.size > 0, 2);"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventuallyAtLeast(v | v.children.size > 2, 2);"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').eventuallyAtLeast(v | v.children.size > 0, 2);"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').since(v|v.children.size > 1).always(v | v.children.size>1);"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').since(v|v.children.size > 1).eventually(v | v.children.size<1);"
			));

			return null;
		});
	}

	@Test
	public void after() throws Throwable {
		keepAddingChildren();
		waitForSync(() -> {
			assertEquals(".after is an open left range, i.e. excludes matching version", 2, (int) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').earliest.after(v|v.children.size > 0).children.size;"
			));
			assertNull(".after with no match returns null", timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').after(v|v.children.size > 5);"
			));
			return null;
		});
	}

	@Test
	public void until() throws Throwable {
		Tree tRoot = keepAddingChildren();
		Tree tFourthChild = TreeFactory.eINSTANCE.createTree();
		tFourthChild.setLabel("T4");
		tRoot.getChildren().add(tFourthChild);
		tRoot.eResource().save(null);
		svnRepository.commit("Added fourth child");
		indexer.requestImmediateSync();

		waitForSync(() -> {
			assertEquals(".until is a closed end range, i.e. includes matching version", 2, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').until(v|v.children.size > 1).latest.children.size;"
			));
			assertNull(".until with no match returns null", timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').until(v|v.children.size > 5);"
			));
			assertEquals(".since + .until works", 2, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').since(v|v.children.size > 1).until(v|v.children.size > 2).versions.size;"
			));
			return null;
		});
	}

	@Test
	public void before() throws Throwable {
		Tree tRoot = keepAddingChildren();
		Tree tFourthChild = TreeFactory.eINSTANCE.createTree();
		tFourthChild.setLabel("T4");
		tRoot.getChildren().add(tFourthChild);
		tRoot.eResource().save(null);
		svnRepository.commit("Added fourth child");
		indexer.requestImmediateSync();

		waitForSync(() -> {
			assertEquals(".before is a open end range, i.e. excludes matching version", 1, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').before(v|v.children.size > 1).latest.children.size;"
			));
			assertNull(".before with no match returns null", timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').before(v|v.children.size > 5);"
			));
			assertEquals(".after + .before works", 1, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').after(v|v.children.size.println('after for ' + v.time + ': ') > 0).before(v|v.children.size.println('before for ' + v.time + ': ') > 2).versions.size;"
			));
			assertFalse(".after + .before can give an undefined node", (boolean) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').after(v|v.children.size > 0).before(v|v.children.size > 1).isDefined();"
			));

			return null;
		});
	}

	@Test
	public void sinceThen() throws Throwable {
		keepAddingChildren();

		waitForSync(() -> {
			assertFalse("Type node - Without .sinceThen, always uses all versions", (boolean) timeAwareEOL(
				"return Tree.earliest.next.always(v|v.all.size > 0);"
			));
			assertTrue("Type node - With .sinceThen, scope is limited to that version onwards", (boolean) timeAwareEOL(
				"return Tree.earliest.next.sinceThen.always(v|v.all.size > 0);"
			));

			assertFalse("Model element - Without .sinceThen, always uses all versions", (boolean) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label = 'Root').next.always(v|v.children.size > 0);"
			));
			assertTrue("Model element - With .sinceThen, scope is limited to that version onwards", (boolean) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label = 'Root').next.sinceThen.always(v|v.children.size.println('Number of children at ' + v.time + ': ') > 0);"
			));

			return null;
		});
	}

	@Test
	public void whenPoints() throws Throwable {
		Tree tRoot = keepAddingChildren();
		tRoot.getChildren().remove(2);
		tRoot.eResource().save(null);
		svnRepository.commit("Removed third child");
		indexer.requestImmediateSync();

		waitForSync(() -> {
			assertEquals(".when with all versions", 5, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size >= 0).versions.size;"
			));
			assertFalse(".when with no versions returns null", (boolean) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size > 5).isDefined();"
			));
			assertEquals(".when with some contiguous versions", 2, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size < 2).versions.size;"
			));
			assertEquals(".when with one version", 1, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size = 3).versions.size;"
			));
			assertEquals(".when with some non-contiguous versions", 2, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size = 2).versions.size;"
			));
			assertEquals(".when with non-contiguous versions + back and forth", 2, (int) timeAwareEOL(
				"return Tree.earliest.next.all.selectOne(t|t.label='Root').when(v|v.children.size = 2).next.prev.children.size;"
			));

			return null;
		});
	}

	@Test
	public void onceFalse() throws Throwable {
		Tree tRoot = keepAddingChildren();
		tRoot.setLabel("SomethingElse");
		tRoot.eResource().save(null);
		svnRepository.commit("Changed label");
		indexer.requestImmediateSync();
		
		waitForSync(() -> {
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.latest.label='SomethingElse').always(v|v.label = 'Root');"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').always(v|v.label = 'Root');"
			));

			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').never(v|v.label = 'Root');"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').never(v|v.label <> 'Root');"
			));

			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventually(v|v.label <> 'Root');"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventually(v|v.label = 'Root');"
			));

			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventuallyAtMost(v|v.label <> 'Root', 1);"
			));
			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventuallyAtMost(v|v.label = 'Root', 2);"
			));

			assertFalse((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventuallyAtLeast(v|v.label <> 'Root', 2);"
			));
			assertTrue((boolean) timeAwareEOL(
				"return Tree.latest.prev.all.selectOne(t|t.label='Root').eventuallyAtLeast(v|v.label = 'Root', 2);"
			));

			assertNotNull(".since by itself returns a node", timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.latest.label='SomethingElse').earliest.since(v|v.label <> 'Root');"
			));
			assertFalse(".since + .eventually works", (boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').earliest.since(v|v.label <> 'Root').eventually(v|v.label = 'Root');"
			));
			assertTrue(".since + .never works", (boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').earliest.since(v|v.label <> 'Root').never(v|v.label.println('Label at ' + v.time + ': ') = 'Root');"
			));
			assertTrue(".since + .always works", (boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.label='Root').earliest.since(v|v.label <> 'Root').always(v|v.children.size > 1);"
			));
			assertFalse(".since can be chained", (boolean) timeAwareEOL(
				"return Tree.latest.all.selectOne(t|t.latest.label='SomethingElse').earliest.since(v|v.label <> 'Root').since(v|v.children.size = 0).isDefined();"
			));

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
