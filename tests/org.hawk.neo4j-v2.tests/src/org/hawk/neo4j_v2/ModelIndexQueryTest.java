package org.hawk.neo4j_v2;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.junit.Test;

/**
 * Integration test case that indexes a simple model and performs a query.
 */
public class ModelIndexQueryTest extends AbstractGraphTest {

	private SyncValidationListener validationListener;

	@Override
	public void setup(String testCaseName) throws Exception {
		super.setup(testCaseName);
		validationListener = new SyncValidationListener();
		indexer.addGraphChangeListener(validationListener);
		validationListener.setModelIndexer(indexer);
	}

	@Override
	public void teardown() throws Exception {
		indexer.removeGraphChangeListener(validationListener);
		super.teardown();
	}

	@Test
	public void tree() throws Throwable {
		setup("tree");
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(2, queryEngine.query(indexer, "return Tree.all.size;", null));
				return null;
			}
		});
	}

	@Test
	public void set0() throws Throwable {
		setup("set0");
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/JDTAST.ecore"));

		requestFolderIndex(new File("resources/models/set0"));
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(1, queryEngine.query(indexer, "return IJavaProject.all.size;", null));

				final int reportedSize = (Integer) queryEngine.query(indexer, "return TypeDeclaration.all.size;", null);
				final Collection<?> actualList = (Collection<?>) queryEngine.query(indexer,
						"return TypeDeclaration.all;", null);
				assertEquals(reportedSize, actualList.size());

				return null;
			}
		});
	}
}
