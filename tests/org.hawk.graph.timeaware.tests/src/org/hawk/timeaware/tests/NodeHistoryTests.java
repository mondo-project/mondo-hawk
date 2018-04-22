package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.backend.tests.factories.GreycatDatabaseFactory;
import org.hawk.graph.timeaware.TimeAwareModelUpdater;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.SvnManager;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the time-aware indexing of model element nodes.
 */
public class NodeHistoryTests extends ModelIndexingTest {

	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	private final TreeFactory treeFactory = TreeFactory.eINSTANCE;
	private ResourceSet rsTree;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return Collections.singletonList(new Object[0]);
    }
	
	public NodeHistoryTests() {
		super(new GreycatDatabaseFactory(), new EMFModelSupportFactory());
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
				assertEquals(0, eol("return Tree.all.size;"));

				// .created can return instances that have been created from a certain moment in time (even if not alive anymore)
				assertEquals(1, eol("return Tree.created('always').size;"));

				// .isAlive('time') returns true/false depending on whether the node still exists at this point in time
				assertEquals(false, eol("return Tree.created('always').first.isAlive('now');"));

				return null;
			}
		});
	}

	@Override
	protected GraphModelUpdater createModelUpdater() {
		return new TimeAwareModelUpdater();
	}

	private void requestSVNIndex() throws Exception {
		final SvnManager vcs = new SvnManager();
		vcs.init(svnRepository.getRepositoryURL().toString(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
	}

}
