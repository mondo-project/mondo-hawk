package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
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
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the time-aware indexing of model element nodes.
 */
public class NodeHistoryTest extends ModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	private final TreeFactory treeFactory = TreeFactory.eINSTANCE;
	private ResourceSet rsTree;

	private TimeAwareEOLQueryEngine timeAwareQueryEngine;

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
	}

	@Test
	public void createDeleteNode() throws Throwable {
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
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// .all works on the latest revision (keeps time-aware querying working)
				assertEquals(0, timeAwareEOL("return Tree.all.size;"));

				// .created can return instances that have been created from a certain moment in time (even if not alive anymore)
				assertEquals(1, timeAwareEOL("return Tree.latest.prev.size;"));

				assertEquals("xy", timeAwareEOL("return Tree.latest.prev.all.first.label;"));

				return null;
			}
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

}
