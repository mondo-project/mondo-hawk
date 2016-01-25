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
import java.util.concurrent.Callable;

import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.localfolder.LocalFolder;
import org.hawk.orientdb.util.FileUtils;
import org.hawk.orientdb.util.SyncEndListener;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test case that indexes a simple model and performs a query.
 */
public class ModelQueryTest {

	private DefaultConsole console;
	private OrientDatabase db;
	private ModelIndexerImpl indexer;
	private CEOLQueryEngine queryEngine;
	private SyncValidationListener validationListener;

	public void setup(String testCaseName) throws Exception {
		final File dbFolder = new File("testdb" + testCaseName);
		FileUtils.deleteRecursively(dbFolder);
		dbFolder.mkdir();

		final File indexerFolder = new File("testindexer" + testCaseName);
		FileUtils.deleteRecursively(indexerFolder);
		indexerFolder.mkdir();

		console = new DefaultConsole();
		db = new OrientDatabase();
		db.run("plocal:" + dbFolder.getAbsolutePath(), dbFolder, console);

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(
				new File("keystore"), "admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore,
				console);
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
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		indexer.removeGraphChangeListener(validationListener);
		db.delete();
	}

	@Test
	public void tree() throws Throwable {
		setup("tree");
		indexer.registerMetamodel(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/Tree.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.run(new File("resources/models/tree").getAbsolutePath(), indexer);
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(2, queryEngine.getAllOfType("Tree").size());
				assertEquals(2, queryEngine.query(indexer,
						"return Tree.all.size;", null));
				return null;
			}
		});
	}

	@Test
	public void set0() throws Throwable {
		setup("set0");
		indexer.registerMetamodel(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/JDTAST.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.run(new File("resources/models/set0").getAbsolutePath(), indexer);
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(1, queryEngine.getAllOfType("IJavaProject").size());
				assertEquals(1, queryEngine.query(indexer,
						"return IJavaProject.all.size;", null));
				return null;
			}
		});
	}

	/**
	 * Only for manual stress testing: replace with path to folder with only the
	 * GraBaTs 2009 set2 .xmi.
	 */
	@Ignore
	@Test
	public void set2() throws Throwable {
		setup("set2");
		indexer.registerMetamodel(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/JDTAST.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.run("/home/antonio/Desktop/grabats2009/set2", indexer);
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
}
