package org.hawk.neo4j_v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.core.util.GraphChangeAdapter;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.localfolder.LocalFolder;
import org.junit.After;
import org.junit.Test;

/**
 * Integration test case that indexes a simple model and performs a query.
 */
public class ModelQueryTest {

	private static final class SyncEndListener extends GraphChangeAdapter {
		private final Callable<?> r;
		private final Semaphore sem;
		private Throwable ex = null;

		private SyncEndListener(Callable<?> r, Semaphore sem) {
			this.r = r;
			this.sem = sem;
		}

		@Override
		public void synchroniseEnd() {
			try {
				r.call();
			} catch (Throwable e) {
				ex = e;
			}
			sem.release();
		}

		public Throwable getThrowable() {
			return ex;
		}
	}

	private DefaultConsole console;
	private Neo4JDatabase db;
	private ModelIndexerImpl indexer;
	private CEOLQueryEngine queryEngine;
	private SyncValidationListener validationListener;

	public void setup(String testCaseName) throws Exception {
		final File dbFolder = new File("testdb");
		deleteRecursively(dbFolder);
		dbFolder.mkdir();

		final File indexerFolder = new File("testindexer" + testCaseName);
		deleteRecursively(indexerFolder);
		indexerFolder.mkdir();

		console = new DefaultConsole();
		db = new Neo4JDatabase();
		db.run(dbFolder, console);

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
		indexer.removeGraphChangeListener(validationListener);
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		db.delete();
	}

	@Test
	public void tree() throws Throwable {
		setup("tree");
		indexer.registerMetamodel(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodel(new File("resources/metamodels/Tree.ecore"));

		final LocalFolder vcs = new LocalFolder();
		vcs.init(new File("resources/models/tree").getAbsolutePath(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(2,
						queryEngine.query(indexer, "return Tree.all.size;", null));
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
		vcs.init(new File("resources/models/set0").getAbsolutePath(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		indexer.requestImmediateSync();

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getTotalErrors());
				assertEquals(1, queryEngine.query(indexer,
						"return IJavaProject.all.size;", null));
				return null;
			}
		});
	}

	private void waitForSync(final Callable<?> r) throws Throwable {
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

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists())
			return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}
}
