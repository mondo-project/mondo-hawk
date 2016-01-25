/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.bpmn.model.BPMNModelResourceFactory;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.localfolder.LocalFolder;
import org.hawk.orientdb.util.FileUtils;
import org.hawk.orientdb.util.SyncEndListener;
import org.junit.After;
import org.junit.Test;

/**
 * Integration test case that indexes a tree model and changes it in some way.
 */
public class ModelUpdateTest {

	private DefaultConsole console;
	private OrientDatabase db;
	private ModelIndexerImpl indexer;
	private CEOLQueryEngine queryEngine;
	private SyncValidationListener validationListener;
	private File modelFolder;
	private Path modelPath;

	public void setup(String testCaseName, String baseModel) throws Throwable {
		final File dbFolder = new File("testdb" + testCaseName);
		FileUtils.deleteRecursively(dbFolder);
		dbFolder.mkdir();

		final File indexerFolder = new File("testindexer" + testCaseName);
		FileUtils.deleteRecursively(indexerFolder);
		indexerFolder.mkdir();

		modelFolder = new File("testmodels" + testCaseName);
		FileUtils.deleteRecursively(modelFolder);
		modelFolder.mkdir();
		modelPath = new File(modelFolder, new File(baseModel).getName())
				.toPath();
		Files.copy(new File("resources/models/" + baseModel).toPath(),
				modelPath);

		console = new DefaultConsole();
		db = new OrientDatabase();
		db.run("plocal:" + dbFolder.getAbsolutePath(), dbFolder, console);

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(
				new File("keystore"), "admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore,
				console);
		indexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		indexer.addMetaModelResourceFactory(new BPMNMetaModelResourceFactory());
		indexer.addModelResourceFactory(new EMFModelResourceFactory());
		indexer.addModelResourceFactory(new BPMNModelResourceFactory());
		queryEngine = new CEOLQueryEngine();
		indexer.addQueryEngine(queryEngine);
		indexer.setMetaModelUpdater(new GraphMetaModelUpdater());
		indexer.addModelUpdater(new GraphModelUpdater());
		indexer.setDB(db, true);
		indexer.init(0, 0);
		validationListener = new SyncValidationListener();
		indexer.addGraphChangeListener(validationListener);
		validationListener.setModelIndexer(indexer);

		indexer.registerMetamodel(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/XMLType.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/Tree.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.run(modelFolder.getAbsolutePath(), indexer);
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				return null;
			}
		});
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		indexer.removeGraphChangeListener(validationListener);
		db.delete();
	}

	@Test
	public void addChild() throws Throwable {
		setup("addChild", "tree/tree.model");
		replaceWith("changed-trees/add-child.model");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(
						3,
						queryEngine.getAllOf("Tree",
								ModelElementNode.EDGE_LABEL_OFTYPE).size());
				return null;
			}
		});
	}

	@Test
	public void bpmn() throws Throwable {
		setup("bpmn", "bpmn/v0-B.2.0.bpmn");
		final Callable<Object> noErrors = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				return null;
			}
		};

		for (int i = 1; i <= 8; i++) {
			replaceWith("bpmn/v" + i + "-B.2.0.bpmn");
			indexer.requestImmediateSync();
			SyncEndListener.waitForSync(indexer, noErrors);
		}
	}

	@Test
	public void removeChild() throws Throwable {
		setup("removeChild", "tree/tree.model");
		replaceWith("changed-trees/remove-child.model");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(
						1,
						queryEngine.getAllOf("Tree",
								ModelElementNode.EDGE_LABEL_OFTYPE).size());
				return null;
			}
		});
	}

	@Test
	public void renameChild() throws Throwable {
		setup("renameChild", "tree/tree.model");
		replaceWith("changed-trees/rename-child.model");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(
						2,
						queryEngine.getAllOf("Tree",
								ModelElementNode.EDGE_LABEL_OFTYPE).size());
				assertEquals(1, queryEngine.query(indexer,
						"return Tree.all.select(t|t.label='t90001').size;",
						null));
				return null;
			}
		});
	}

	@Test
	public void renameRoot() throws Throwable {
		setup("renameRoot", "tree/tree.model");
		replaceWith("changed-trees/rename-root.model");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(
						2,
						queryEngine.getAllOf("Tree",
								ModelElementNode.EDGE_LABEL_OFTYPE).size());
				assertEquals(1, queryEngine.query(indexer,
						"return Tree.all.select(t|t.label='t40').size;", null));
				return null;
			}
		});
	}

	@Test
	public void reuseDeleted() throws Throwable {
		// Test case for issue #25
		setup("reuse", "tree/tree.model");
		teardown();
		setup("reuse", "tree/tree.model");
		assertFalse("The deleted directory should be reusable", db.getGraph()
				.isClosed());
	}

	private void replaceWith(final String replacement) throws IOException {
		final File replacementFile = new File("resources/models/" + replacement);
		Files.copy(replacementFile.toPath(), modelPath,
				StandardCopyOption.REPLACE_EXISTING);
		System.err.println("Copied " + replacementFile + " over " + modelPath);
	}
}
