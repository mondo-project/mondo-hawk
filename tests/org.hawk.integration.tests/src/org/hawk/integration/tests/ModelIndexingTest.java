/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.hawk.backend.tests.LogbackOnlyErrorsRule;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.localfolder.LocalFolder;
import org.hawk.workspace.Workspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Base class for all integration test cases involving the indexing of a certain
 * graph and querying afterwards.
 */
@RunWith(Parameterized.class)
public class ModelIndexingTest {
	@Rule
	public LogbackOnlyErrorsRule logRule = new LogbackOnlyErrorsRule();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Rule
	public TestName testName = new TestName();

	/**
	 * Rule that will attach a {@link SyncValidationListener} to the instance
	 * during the test.
	 */
	public class GraphChangeListenerRule<T extends IGraphChangeListener> extends ExternalResource {
		private T listener;

		public T getListener() {
			return listener;
		}

		public GraphChangeListenerRule(T listener) {
			this.listener = listener;
		}

		@Override
		protected void before() throws Throwable {
			if (indexer == null) {
				ModelIndexingTest.this.setup();
			}
			indexer.addGraphChangeListener(listener);
			listener.setModelIndexer(indexer);
		}

		@Override
		protected void after() {
			indexer.removeGraphChangeListener(listener);
		}
	}

	public interface IModelSupportFactory {
		IMetaModelResourceFactory createMetaModelResourceFactory();

		IModelResourceFactory createModelResourceFactory();
	}

	private DefaultConsole console;

	protected ModelIndexerImpl indexer;
	protected EOLQueryEngine queryEngine;
	protected IGraphDatabase db;

	private IGraphDatabaseFactory dbFactory;
	private IModelSupportFactory msFactory;

	public ModelIndexingTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory msFactory) {
		this.dbFactory = dbFactory;
		this.msFactory = msFactory;
	}

	@Before
	public void setup() throws Throwable {
		if (indexer != null) {
			// Might have been invoked by a rule before
			return;
		}

		final File indexerFolder = tempFolder.getRoot();
		final File dbFolder = new File(indexerFolder, "test_" + testName.getMethodName());
		dbFolder.mkdir();

		console = new DefaultConsole();
		db = dbFactory.create();
		db.run(dbFolder, console);

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(new File("keystore"),
				"admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore, console);
		indexer.addMetaModelResourceFactory(msFactory.createMetaModelResourceFactory());
		indexer.addModelResourceFactory(msFactory.createModelResourceFactory());

		queryEngine = new EOLQueryEngine();
		indexer.addQueryEngine(queryEngine);
		indexer.setMetaModelUpdater(new GraphMetaModelUpdater());
		indexer.addModelUpdater(new GraphModelUpdater());
		indexer.setDB(db, true);

		indexer.init(0, 0);
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		db.delete();
	}

	protected void waitForSync() throws Throwable {
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// nothing to do
				return null;
			}
		});
	}

	protected void waitForSync(final Callable<?> r) throws Throwable {
		final Semaphore sem = new Semaphore(0);
		final SyncEndListener changeListener = new SyncEndListener(r, sem);
		indexer.addGraphChangeListener(changeListener);
		if (!sem.tryAcquire(600, TimeUnit.SECONDS)) {
			fail("Synchronization timed out");
		} else {
			indexer.removeGraphChangeListener(changeListener);
			if (changeListener.getThrowable() != null) {
				throw changeListener.getThrowable();
			}
		}
	}

	protected void requestFolderIndex(final File folder) throws Exception {
		final LocalFolder vcs = new LocalFolder();
		vcs.init(folder.getAbsolutePath(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
	}

	protected Object eol(final String eolQuery) throws InvalidQueryException, QueryExecutionException {
		return eol(eolQuery, null);
	}

	protected Object eol(final String eolQuery, Map<String, Object> context) throws InvalidQueryException, QueryExecutionException {
		return queryEngine.query(indexer, eolQuery, context);
	}

	protected Object eolWorkspace(final String query) throws InvalidQueryException, QueryExecutionException {
		return eol(query,
			Collections.singletonMap(EOLQueryEngine.PROPERTY_REPOSITORYCONTEXT, Workspace.REPOSITORY_URL));
	}

	protected void requestWorkspaceIndexing() throws Exception {
		final Workspace vcs = new Workspace();
		vcs.init("/", indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
	}

	protected IProject openProject(final File projectFolder) throws CoreException {
		final File projectFile = new File(projectFolder, ".project");
		final Path projectPath = new Path(projectFile.getAbsolutePath());

		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProjectDescription description = ws.loadProjectDescription(projectPath);
		IProject project = ws.getRoot().getProject(description.getName());
		if (!project.exists()) {
			project.create(description, null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}

		return project;
	}
}