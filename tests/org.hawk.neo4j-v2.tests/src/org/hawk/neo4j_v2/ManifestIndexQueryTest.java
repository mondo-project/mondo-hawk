package org.hawk.neo4j_v2;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.Callable;

import org.hawk.manifest.metamodel.ManifestMetaModelResourceFactory;
import org.hawk.manifest.model.ManifestModelResourceFactory;
import org.junit.Test;

/**
 * Tests for the indexing of Eclipe manifests.
 */
public class ManifestIndexQueryTest extends AbstractGraphTest {

	@Override
	protected void setupIndexerFactories() {
		indexer.addMetaModelResourceFactory(new ManifestMetaModelResourceFactory());
		indexer.addModelResourceFactory(new ManifestModelResourceFactory());
	}

	@Test
	public void indexDuplicateDependencies() throws Throwable {
		setup("manifestdupdep");
		requestFolderIndex(new File("resources/models/dupdep"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				Object result = queryEngine.query(indexer, "return ManifestRequires.all.bundle.size;", null);
				assertEquals(2, (int) result);
				return null;
			}
		});
	}

}
