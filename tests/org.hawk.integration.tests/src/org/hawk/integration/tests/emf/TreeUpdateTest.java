/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
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
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

/**
 * Integration test case that indexes a tree model and changes it in some way.
 */
public class TreeUpdateTest extends ModelIndexingTest {

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public TreeUpdateTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	private Path modelPath;

	public void prepare(String baseModel) throws Throwable {
		modelPath = new File(modelFolder.getRoot(), new File(baseModel).getName()).toPath();
		Files.copy(new File("resources/models/" + baseModel).toPath(), modelPath);

		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodels(new File("resources/metamodels/XMLType.ecore"));
		indexer.registerMetamodels(new File("resources/metamodels/Tree.ecore"));

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
	public void addChild() throws Throwable {
		prepare("tree/tree.model");
		replaceWith("changed-trees/add-child.model");
		indexer.requestImmediateSync();
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				try (IGraphTransaction tx = db.beginTransaction()) {
					assertEquals(3, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
					tx.success();
				}
				return null;
			}
		});
	}

	@Test
	public void removeChild() throws Throwable {
		prepare("tree/tree.model");
		replaceWith("changed-trees/remove-child.model");
		indexer.requestImmediateSync();
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				try (IGraphTransaction tx = db.beginTransaction()) {
					assertEquals(1, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
					tx.success();
				}
				return null;
			}
		});
	}

	@Test
	public void renameChild() throws Throwable {
		prepare("tree/tree.model");
		replaceWith("changed-trees/rename-child.model");
		indexer.requestImmediateSync();
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				assertEquals(2, eol("return Tree.all.size;"));
				assertEquals(1, eol("return Tree.all.select(t|t.label='t90001').size;"));
				return null;
			}
		});
	}

	@Test
	public void renameRoot() throws Throwable {
		prepare("tree/tree.model");
		replaceWith("changed-trees/rename-root.model");
		indexer.requestImmediateSync();
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				try (IGraphTransaction tx = db.beginTransaction()) {
					assertEquals(2, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
					assertEquals(1, queryEngine.query(indexer,
							"return Tree.all.select(t|t.label='t40').size;", null));
					tx.success();
				}
				return null;
			}
		});
	}

	private void replaceWith(final String replacement) throws IOException {
		final File replacementFile = new File("resources/models/" + replacement);
		Files.copy(replacementFile.toPath(), modelPath,
				StandardCopyOption.REPLACE_EXISTING);
		System.err.println("Copied " + replacementFile + " over " + modelPath);
	}
}
