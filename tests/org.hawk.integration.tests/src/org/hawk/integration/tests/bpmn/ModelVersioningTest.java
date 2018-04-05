/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.bpmn;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

/**
 * Integration test case that indexes a sequence of versions of a BPMN model.
 */
public class ModelVersioningTest extends ModelIndexingTest {

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public ModelVersioningTest(IGraphDatabaseFactory dbf) {
		super(dbf, new BPMNModelSupportFactory());
	}

	private Path modelPath;

	public void prepare(String baseModel) throws Throwable {
		modelPath = new File(modelFolder.getRoot(), new File(baseModel).getName()).toPath();
		Files.copy(new File("resources/models/" + baseModel).toPath(), modelPath);
		requestFolderIndex(modelFolder.getRoot());

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				return null;
			}
		});
	}

	@Test
	public void bpmn() throws Throwable {
		prepare("bpmn/v0-B.2.0.bpmn");
		final Callable<Object> noErrors = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				return null;
			}
		};

		for (int i = 1; i <= 8; i++) {
			replaceWith("bpmn/v" + i + "-B.2.0.bpmn");
			indexer.requestImmediateSync();
			waitForSync(noErrors);
		}
	}

	private void replaceWith(final String replacement) throws IOException {
		final File replacementFile = new File("resources/models/" + replacement);
		Files.copy(replacementFile.toPath(), modelPath,
				StandardCopyOption.REPLACE_EXISTING);
		System.err.println("Copied " + replacementFile + " over " + modelPath);
	}
}
