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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
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
	protected OrientDatabase db;
	private ModelIndexerImpl indexer;
	private EOLQueryEngine queryEngine;
	private SyncValidationListener validationListener;

	public void setup(String testCaseName, boolean doValidation) throws Exception {
		final File dbFolder = new File("testdb" + testCaseName);
		FileUtils.deleteRecursively(dbFolder);
		dbFolder.mkdir();

		final File indexerFolder = new File("testindexer" + testCaseName);
		FileUtils.deleteRecursively(indexerFolder);
		indexerFolder.mkdir();

		console = new DefaultConsole();
		createDB(dbFolder);

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(
				new File("keystore"), "admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore,
				console);
		indexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		indexer.addModelResourceFactory(new EMFModelResourceFactory());
		queryEngine = new EOLQueryEngine();
		indexer.addQueryEngine(queryEngine);
		indexer.setMetaModelUpdater(new GraphMetaModelUpdater());
		indexer.addModelUpdater(new GraphModelUpdater());
		indexer.setDB(db, true);
		indexer.init(0, 0);
		queryEngine.load(indexer);

		if (doValidation) {
			validationListener = new SyncValidationListener();
			indexer.addGraphChangeListener(validationListener);
			validationListener.setModelIndexer(indexer);
		}
	}

	protected void createDB(final File dbFolder) throws Exception {
		db = new OrientDatabase();
		db.run("plocal:" + dbFolder.getAbsolutePath(), dbFolder, console);
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		indexer.removeGraphChangeListener(validationListener);
		db.delete();
	}

	@Test
	public void tree() throws Throwable {
		setup("tree", true);
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/Tree.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.init(new File("resources/models/tree").toURI().toASCIIString(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, 200, new Callable<Object>() {
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
	public void treeCrossResourceContainment() throws Throwable {
		setup("tree", true);
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/Tree.ecore"));

		final LocalFolder vcs = new LocalFolder();
		final String uri = new File("resources/models/tree-xres").toURI().toASCIIString();
		vcs.init(uri, indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, 200, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(3, queryEngine.getAllOfType("Tree").size());
				assertEquals(2, queryEngine.query(indexer,
						"return Tree.all.selectOne(t|t.label='root').children.size;", null));
				assertEquals("root", queryEngine.query(indexer,
						"return Tree.all.selectOne(t|t.label='xyz').eContainer.label;", null));
				assertEquals("root", queryEngine.query(indexer,
						"return Tree.all.selectOne(t|t.label='abc').eContainer.label;", null));
				return null;
			}
		});
	}

	@Test
	public void set0() throws Throwable {
		setup("set0", true);
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/JDTAST.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.init(new File("resources/models/set0").getAbsolutePath(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, 800, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(1, queryEngine.getAllOfType("IJavaProject").size());
				assertEquals(1, queryEngine.query(indexer,
						"return IJavaProject.all.size;", null));

				// Test query argument support
				final Map<String, Object> args = new HashMap<>();
				args.put("ename", "org.eclipse.jdt.apt.pluggable.core");
				final Map<String, Object> context = new HashMap<>();
				context.put(EOLQueryEngine.PROPERTY_ARGUMENTS, args);
				assertEquals(false, queryEngine.query(indexer,
					"return IJavaProject.all.selectOne(p|p.elementName=ename).isReadOnly;", context));

				final int reportedSize = (Integer) queryEngine.query(indexer,  "return TypeDeclaration.all.size;", null);
				final Collection<?> actualList = (Collection<?>)queryEngine.query(indexer, "return TypeDeclaration.all;", null);
				assertEquals(reportedSize, actualList.size());

				return null;
			}
		});
	}

	/**
	 * Only for manual stress testing: replace with path to folder with only the
	 * GraBaTs 2009 set1 .xmi.
	 */
	@Ignore
	@Test
	public void set1() throws Throwable {
		performanceIntegrationTest("set1", "/home/antonio/Desktop/grabats2009/set1");
	}

	/**
	 * Only for manual stress testing: replace with path to folder with only the
	 * GraBaTs 2009 set2 .xmi.
	 */
	@Ignore
	@Test
	public void set2() throws Throwable {
		performanceIntegrationTest("set2", "/home/antonio/Desktop/grabats2009/set2");
	}

	public void performanceIntegrationTest(final String name, final String folder) throws Exception, Throwable {
		setup(name, false);
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/JDTAST.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.init(folder, indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		SyncEndListener.waitForSync(indexer, 2000, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return null;
			}
		});
	}
}
