package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Arrays;

import org.hawk.backend.tests.factories.GreycatDatabaseFactory;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.backend.tests.factories.Neo4JDatabaseFactory;
import org.hawk.backend.tests.factories.OrientDatabaseFactory;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.hawk.timeaware.graph.VCSManagerIndex;
import org.hawk.timeaware.graph.VCSManagerIndex.RepositoryNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VCSManagerIndexTests {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Rule
	public TestName testName = new TestName();

	private final IGraphDatabaseFactory dbFactory;
	private IGraphDatabase db;
	private VCSManagerIndex idx;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return Arrays.asList(
    		new Object[] { new Neo4JDatabaseFactory() },
    		new Object[] { new OrientDatabaseFactory() },
    		new Object[] { new GreycatDatabaseFactory() }
    	);
    }
	
	public VCSManagerIndexTests(IGraphDatabaseFactory factory) {
		this.dbFactory = factory;
	}

	@Before
	public void setUp() throws Exception {
		this.db = dbFactory.create();

		final File indexerFolder = tempFolder.getRoot();
		final File dbFolder = new File(indexerFolder, "test_" + testName.getMethodName());
		dbFolder.mkdir();
		db.run(dbFolder, new DefaultConsole());

		try (IGraphTransaction tx = db.beginTransaction()) {
			this.idx = new VCSManagerIndex(db);
			tx.success();
		}
	}

	@Test
	public void create() throws Exception {
		final String repoURI = "platform:/resource";

		RepositoryNode node;
		try (IGraphTransaction tx = db.beginTransaction()) {
			node = idx.getOrCreateRepositoryNode(repoURI);
			tx.success();
		}
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(repoURI, node.getURI());
			assertNull(node.getLastRevision());
			assertEquals(node.getNode().getId(), idx.getOrCreateRepositoryNode(repoURI).getNode().getId());
			tx.success();
		}
	}

	@Test
	public void changeLastRevision() throws Exception {
		final String repoURI = "file:/tmp/example";
		final String lastRev = "1";

		try (IGraphTransaction tx = db.beginTransaction()) {
			RepositoryNode node = idx.getOrCreateRepositoryNode(repoURI);
			node.setLastRevision(lastRev);
			tx.success();
		}
		try (IGraphTransaction tx = db.beginTransaction()) {
			RepositoryNode node = idx.getOrCreateRepositoryNode(repoURI);
			assertEquals(lastRev, node.getLastRevision());
			tx.success();
		}
	}

	@Test
	public void recreate() throws Exception {
		final String repoURI = "svn:/host/path";

		Object firstId;
		try (IGraphTransaction tx = db.beginTransaction()) {
			RepositoryNode node = idx.getOrCreateRepositoryNode(repoURI);
			firstId = node.getNode().getId();
			tx.success();
		}
		try (IGraphTransaction tx = db.beginTransaction()) {
			RepositoryNode node = idx.getOrCreateRepositoryNode(repoURI);
			assertEquals(firstId, node.getNode().getId());
			idx.removeRepositoryNode(repoURI);
			tx.success();
		}
		try (IGraphTransaction tx = db.beginTransaction()) {
			RepositoryNode node = idx.getOrCreateRepositoryNode(repoURI);
			assertNotEquals(firstId, node.getNode().getId());
			tx.success();
		}
	}

}
