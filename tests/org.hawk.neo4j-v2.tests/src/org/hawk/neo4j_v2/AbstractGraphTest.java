package org.hawk.neo4j_v2;

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
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.localfolder.LocalFolder;
import org.junit.After;

public class AbstractGraphTest {

	private DefaultConsole console;
	private Neo4JDatabase db;

	protected ModelIndexerImpl indexer;
	protected CEOLQueryEngine queryEngine;

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists())
			return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

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

		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(new File("keystore"),
				"admin".toCharArray());

		indexer = new ModelIndexerImpl("test", indexerFolder, credStore, console);
		setupIndexerFactories();

		queryEngine = new CEOLQueryEngine();
		indexer.addQueryEngine(queryEngine);
		indexer.setMetaModelUpdater(new GraphMetaModelUpdater());
		indexer.addModelUpdater(new GraphModelUpdater());
		indexer.setDB(db, true);

		indexer.init(0, 0);
	}

	protected void setupIndexerFactories() {
		indexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		indexer.addModelResourceFactory(new EMFModelResourceFactory());
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown(ShutdownRequestType.ALWAYS);
		db.delete();
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
		indexer.requestImmediateSync();
	}

}