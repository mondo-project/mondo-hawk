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
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.backend.tests.factories.GreycatDatabaseFactory;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.backend.tests.factories.LevelDBGreycatDatabaseFactory;
import org.hawk.core.IModelIndexer;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.SvnManager;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.graph.TimeAwareIndexer;
import org.hawk.timeaware.graph.TimeAwareModelUpdater;
import org.hawk.timeaware.queries.TimeAwareEOLQueryEngine;
import org.hawk.timeaware.queries.TimelineEOLQueryEngine;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.junit.Before;
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
public class NodeHistoryTest extends ModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	private final TreeFactory treeFactory = TreeFactory.eINSTANCE;
	private ResourceSet rsTree;

	private TimeAwareEOLQueryEngine timeAwareQueryEngine;

	private TimelineEOLQueryEngine timelineQueryEngine;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return Arrays.asList(
    		new Object[] { new GreycatDatabaseFactory(), new EMFModelSupportFactory() },
    		new Object[] { new LevelDBGreycatDatabaseFactory(), new EMFModelSupportFactory() }
    	);
    }

	public NodeHistoryTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Before
	public void setUp() throws Exception {
		indexer.registerMetamodels(new File("../org.hawk.integration.tests/resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodels(new File("../org.hawk.integration.tests/resources/metamodels/XMLType.ecore"));
		indexer.registerMetamodels(new File("resources/metamodels/Tree.ecore"));

		rsTree = new ResourceSetImpl();
		rsTree.getResourceFactoryRegistry()
			.getExtensionToFactoryMap()
			.put("*", new XMIResourceFactoryImpl());

		timeAwareQueryEngine = new TimeAwareEOLQueryEngine();
		indexer.addQueryEngine(timeAwareQueryEngine);
		
		timelineQueryEngine = new TimelineEOLQueryEngine();
		indexer.addQueryEngine(timelineQueryEngine);
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
				assertEquals(0, timeAwareEOL("return Model.atypes.selectOne(t|t.name='Tree').all.size;"));
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
	
	@Override
	protected GraphModelUpdater createModelUpdater() {
		return new TimeAwareModelUpdater();
	}

	@Override
	protected IModelIndexer createIndexer(File indexerFolder, FileBasedCredentialsStore credStore) {
		return new TimeAwareIndexer("test", indexerFolder, credStore, console);
	}

	private void requestSVNIndex() throws Exception {
		final SvnManager vcs = new SvnManager();
		vcs.init(svnRepository.getRepositoryURL().toString(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
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
