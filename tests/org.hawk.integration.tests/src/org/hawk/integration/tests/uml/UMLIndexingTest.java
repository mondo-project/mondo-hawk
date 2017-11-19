package org.hawk.integration.tests.uml;

import java.io.File;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.equalTo;

public class UMLIndexingTest extends ModelIndexingTest {

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public UMLIndexingTest(IGraphDatabaseFactory dbf) {
		super(dbf, new UMLModelSupportFactory());
	}

	@Test
	public void zoo() throws Throwable {
		requestFolderIndex(new File("resources/models/uml/zoo"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertThat(eol("return Model.types.select(t|t.name='Profile').size;"), equalTo(1));
				assertThat(eol("return Class.all.size;"), equalTo(4));
				return null;
			}
		});
	}
}
