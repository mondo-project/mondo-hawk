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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import org.eclipse.epsilon.eol.models.IModelElement;
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

	public void setup(String testCaseName) throws Throwable {
		final File dbFolder = new File("testdb" + testCaseName);
		FileUtils.deleteRecursively(dbFolder);
		dbFolder.mkdir();

		final File indexerFolder = new File("testindexer" + testCaseName);
		FileUtils.deleteRecursively(indexerFolder);
		indexerFolder.mkdir();

		modelFolder = new File("testmodels" + testCaseName);
		FileUtils.deleteRecursively(modelFolder);
		modelFolder.mkdir();
		modelPath = new File(modelFolder, "tree.model").toPath();
		Files.copy(new File("resources/models/tree/tree.model").toPath(), modelPath);

		console = new DefaultConsole();
		db = new OrientDatabase();
		db.run("plocal:" + dbFolder.getAbsolutePath(), dbFolder, console);

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(new File("keystore"), "admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore, console);
		indexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		indexer.addModelResourceFactory(new EMFModelResourceFactory());
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
		setup("addChild");
		replaceWith("add-child");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(3, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
				return null;
			}
		});
	}

	@Test
	public void removeChild() throws Throwable {
		setup("removeChild");
		replaceWith("remove-child");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(1, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
				return null;
			}
		});
	}

	@Test
	public void renameChild() throws Throwable {
		setup("renameChild");
		replaceWith("rename-child");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(2, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
				assertEquals(1, queryEngine.contextlessQuery(db, "return Tree.all.select(t|t.label='t90001').size;"));
				return null;
			}
		});
	}

	@Test
	public void renameRoot() throws Throwable {
		setup("renameRoot");
		replaceWith("rename-root");
		indexer.requestImmediateSync();
		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(2, queryEngine.getAllOf("Tree", ModelElementNode.EDGE_LABEL_OFTYPE).size());
				assertEquals(1, queryEngine.contextlessQuery(db, "return Tree.all.select(t|t.label='t40').size;"));
				return null;
			}
		});
	}

	private void replaceWith(final String basename) throws IOException {
		final File replacementFile = new File("resources/models/changed-trees/" + basename + ".model");
		Files.copy(replacementFile.toPath(), modelPath, StandardCopyOption.REPLACE_EXISTING);
	}
}
