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
package org.hawk.backend.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for populating and querying indices.
 */
public class IndexTest extends TemporaryDatabaseTest {

	@Parameters(name="Parameters are {0}")
	public static Iterable<Object[]> params() {
		return BackendTestSuite.caseParams();
	}

	public IndexTest(IGraphDatabaseFactory dbFactory) {
		super(dbFactory);
	}

	@Test
	public void query() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final String mmFileURI = "file://a/b/c.d";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);
		final Map<String, Object> mmFileNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmFileURI);

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmNode, Collections.singletonMap("id", mmBarURI));

			IGraphNode mmFileNode = db.createNode(mmFileNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmFileNode, Collections.singletonMap("id", mmFileURI));

			tx.success();
		}

		// Query with partial wildcard
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", "http://*");
			assertSame(db, iter.getSingle().getGraph());
			assertEquals(1, iter.size());
			assertTrue(iter.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString().startsWith("http://"));
			tx.success();
		}

		// Query with full value
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> iter3 = db.getMetamodelIndex().query("id", mmBarURI);
			assertEquals(1, iter3.size());

			final IGraphNode node = iter3.getSingle();
			assertSame(db, node.getGraph());
			assertEquals(mmBarURI, node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
			tx.success();
		}

		// Query with wildcard on one field
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> iter4 = db.getMetamodelIndex().query("id", "*");
			assertSame(db, iter4.getSingle().getGraph());
			assertEquals(2, iter4.size());
			tx.success();
		}

		// Retrieve with full value
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> iter2 = db.getMetamodelIndex().get("id", mmFileURI);
			assertEquals(1, iter2.size());
			assertEquals(mmFileURI, iter2.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
			tx.success();
		}

		// Use */* (all fields, all values)
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(new HashSet<>(Arrays.asList(mmBarURI, mmFileURI)), db.getKnownMMUris());
			tx.success();
		}
	}

	@Test
	public void queryWithPipes() throws Exception {
		// Pipe characters should be treated as such, not as "OR" (as in regexps)
		final String sN1 = "/a||/foo/bar", sN2 = "/b||/test";

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1 = db.createNode(null, "metamodel");
			db.getMetamodelIndex().add(n1, "id", sN1);

			IGraphNode n2 = db.createNode(null, "metamodel");
			db.getMetamodelIndex().add(n2, "id", sN2);

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(1, db.getMetamodelIndex().query("id", "/*||/foo/*").size());
			tx.success();
		}
	}

	@Test
	public void removeByNode() throws Exception {
 		final String mmBarURI = populateForRemove();

		// NOTE: changes in Lucene indexes are not complete until we commit the
		// transaction
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByNodeMultipleIndices() throws Exception {
		IGraphNode n;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(null, "x");
			db.getMetamodelIndex().add(n, "a", 1);
			db.getFileIndex().add(n, "f", "/x/y");
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(1, db.getMetamodelIndex().query("*", "*").size());
			assertEquals(1, db.getFileIndex().query("*", "*").size());
			db.getMetamodelIndex().remove(n);
			assertEquals(0, db.getMetamodelIndex().query("*", "*").size());
			assertEquals(1, db.getFileIndex().query("*", "*").size());

			tx.success();
		}
	}

	@Test
	public void removeByFullKey() throws Exception {
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle(), "id", mmBarURI);
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByFullKeyInt() throws Exception {
		IGraphNode n1, n2;
		final String key = "int";
		try (IGraphTransaction tx = db.beginTransaction()) {
			n1 = db.createNode(null, "v");
			n2 = db.createNode(null, "v");
			db.getMetamodelIndex().add(n1, key, 1);
			db.getMetamodelIndex().add(n2, key, 2);

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphNodeIndex mmIdx = db.getMetamodelIndex();
			assertEquals(2, mmIdx.query(key, 1, 2, true, true).size());
			mmIdx.remove(n1, key, 1);
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, db.getMetamodelIndex().get(key, 1).size());
			assertEquals(1, db.getMetamodelIndex().get(key, 2).size());
			assertEquals(1, db.getMetamodelIndex().query(key, 1, 2, true, true).size());
			assertEquals(1, db.getMetamodelIndex().query(key, "*").size());
			tx.success();
		}
	}

	@Test
	public void removeByFullKeyDouble() throws Exception {
		final String key = "double";

		IGraphNode n1, n2;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n1 = db.createNode(null, "v");
			n2 = db.createNode(null, "v");
			db.getMetamodelIndex().add(n1, key, 1.1);
			db.getMetamodelIndex().add(n2, key, 2.3);

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphNodeIndex mmIdx = db.getMetamodelIndex();
			mmIdx.remove(n1, key, 1.1);
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, db.getMetamodelIndex().get(key, 1.1).size());
			assertEquals(1, db.getMetamodelIndex().get(key, 2.3).size());
			assertEquals(1, db.getMetamodelIndex().query(key, 1.0, 3.0, false, false).size());
			assertEquals(1, db.getMetamodelIndex().query(key, "*").size());
			tx.success();
		}
	}
	
	@Test
	public void removeByValueNode() throws Exception {
		final String mmBarURI = populateForRemove();
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle(), null, mmBarURI);
			tx.success();
		} catch (UnsupportedOperationException ex) {
			Assume.assumeTrue("Removing a node by value with no key is not supported on this backend", false);
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByFieldNode() throws Exception {
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle(), "id", null);
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByNode3Arg() throws Exception {
		final String mmBarURI = populateForRemove();

		// NOTE: changes in Lucene indexes are not complete until we commit the transaction
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle(), null, null);
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void integerExact() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, Collections.singletonMap("value", 5));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, Collections.singletonMap("value", 8));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, Collections.singletonMap("value", 8));

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, idx.get("value", 1).size());
			assertEquals(0, idx.query("value", 1).size());

			assertEquals(1, idx.get("value", 5).size());
			assertEquals(1, idx.query("value", 5).size());

			assertEquals(2, idx.get("value", 8).size());
			assertEquals(2, idx.query("value", 8).size());

			assertEquals(3, idx.query("value", "*").size());
			assertEquals(3, idx.query("*", "*").size());

			tx.success();
		}
	}

	@Test
	public void doubleExact() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, Collections.singletonMap("value", 5.3));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, Collections.singletonMap("value", 8.1));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, Collections.singletonMap("value", 8.1));

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, idx.get("value", 5.2).size());
			assertEquals(0, idx.query("value", 5.2).size());
			assertEquals(0, idx.get("value", 8.4).size());
			assertEquals(0, idx.query("value", 8.4).size());

			assertEquals(1, idx.get("value", 5.3).size());
			assertEquals(1, idx.query("value", 5.3).size());
			assertEquals(2, idx.get("value", 8.1).size());
			assertEquals(2, idx.query("value", 8.1).size());

			assertEquals(3, idx.query("value", "*").size());
			assertEquals(3, idx.query("*", "*").size());

			tx.success();
		}
	}

	@Test
	public void integerRanges() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, Collections.singletonMap("value", 5));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, Collections.singletonMap("value", 8));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, Collections.singletonMap("value", 1));
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(3, idx.query("value", 0, 10, false, false).size());
			assertEquals(1, idx.query("value", 1, 8, false, false).size());
			assertEquals(2, idx.query("value", 1, 8, true, false).size());
			assertEquals(2, idx.query("value", 1, 8, false, true).size());
			assertEquals(3, idx.query("value", 1, 8, true, true).size());
			assertEquals(2, idx.query("value", 2, 8, true, true).size());
			assertEquals(2, idx.query("value", 1, 7, true, true).size());
			assertEquals(1, idx.query("value", 4, 6, false, false).size());
			assertEquals(1, idx.query("value", 5, 5, true, true).size());
			assertEquals(0, idx.query("value", 5, 5, false, false).size());
			assertEquals(0, idx.query("value", 5, 4, false, false).size());
			tx.success();
		}
	}

	@Test
	public void floatingRanges() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, Collections.singletonMap("value", 5.3));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, Collections.singletonMap("value", 8.1d));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, Collections.singletonMap("value", 1.34));
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(3, idx.query("value", 0.0, 10.0, false, false).size());
			assertEquals(3, idx.query("value", 1.3, 8.2, false, false).size());
			assertEquals(2, idx.query("value", 1.4, 8.2, false, false).size());
			assertEquals(2, idx.query("value", 1.3, 8.0, false, false).size());
			assertEquals(1, idx.query("value", 1.4, 8.0, false, false).size());
			assertEquals(1, idx.query("value", 5.2, 5.4, false, false).size());
			assertEquals(0, idx.query("value", 5.2, 5.2, false, false).size());
			assertEquals(0, idx.query("value", 5.2, 5.1, false, false).size());
			tx.success();
		}
	}

	@Test
	public void invalidIndexNames() throws Exception {
		// OIndexManagerShared#createIndex checks for these
		final char[] invalidChars = ":,; %=/\\".toCharArray();

		IGraphNode n;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(null, "eobject");
			tx.success();
		}
		for (char invalidChar : invalidChars) {
			final String name = "my" + invalidChar + "index";
			try (IGraphTransaction tx = db.beginTransaction()) {
				assertFalse(db.nodeIndexExists(name));
				IGraphNodeIndex idx = db.getOrCreateNodeIndex(name);
				idx.add(n, "id", 1);
				tx.success();
			}

			try (IGraphTransaction tx = db.beginTransaction()) {
				assertTrue(db.getNodeIndexNames().contains(name));
				assertTrue(db.nodeIndexExists(name));
				IGraphNodeIndex idx = db.getOrCreateNodeIndex(name);

				IGraphIterable<IGraphNode> results = idx.query("id", 1);
				assertEquals("Exact query for index " + name + " should produce one result", 1, results.size());
				results = idx.query("id", 0, 1, true, true);
				assertEquals("Range query for index " + name + " should produce one result", 1, results.size());

				final IGraphIterable<IGraphNode> allID = idx.query("id", "*");
				assertEquals("Should find exactly one match in " + name, 1, allID.size());

				tx.success();
			}
		}
	}

	@Test
	public void getMultipleMatches() throws Exception {
		final String objLabel = "object";
		final String fileIdxKey = "file";
		final String fileNodeLabel = fileIdxKey;
		final String fileEdgeLabel = "inFile";

		IGraphNodeIndex idxRoots;
		try (IGraphTransaction tx = db.beginTransaction()) {
			idxRoots = db.getOrCreateNodeIndex("roots");

			IGraphNode n1 = db.createNode(null, objLabel);
			IGraphNode n2 = db.createNode(null, objLabel);
			IGraphNode n3 = db.createNode(null, objLabel);
			IGraphNode f1 = db.createNode(null, fileNodeLabel);
			IGraphNode f2 = db.createNode(null, fileNodeLabel);

			db.createRelationship(n1, f1, fileEdgeLabel);
			db.createRelationship(n2, f1, fileEdgeLabel);
			db.createRelationship(n3, f2, fileEdgeLabel);
			idxRoots.add(n1, fileIdxKey, "a.xmi");
			idxRoots.add(n2, fileIdxKey, "a.xmi");
			idxRoots.add(n3, fileIdxKey, "b.xmi");

			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> aRoots = idxRoots.get(fileIdxKey, "a.xmi");
			assertEquals(2, aRoots.size());
			assertEquals(2, iteratorSize(aRoots.iterator()));
			assertNotNull(aRoots.getSingle());
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> bRoots = idxRoots.get(fileIdxKey, "b.xmi");
			assertEquals(1, bRoots.size());
			assertEquals(1, iteratorSize(bRoots.iterator()));
			assertNotNull(bRoots.getSingle());
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> cRoots = idxRoots.get(fileIdxKey, "c.xmi");
			assertEquals(0, cRoots.size());
			assertEquals(0, iteratorSize(cRoots.iterator()));
			try {
				cRoots.getSingle();
				fail("NoSuchElementException should have been thrown");
			} catch (NoSuchElementException ex) {
				// good!
			}
			tx.success();
		}
	}

	@Test
	public void addNullValue() throws Exception {
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNodeIndex idxRoots = db.getOrCreateNodeIndex("roots");
			IGraphNode n1 = db.createNode(null, "eobject");
			idxRoots.add(n1, "file", null);
			tx.success();
		}
	}

	@Test
	public void addNullMap() throws Exception {
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNodeIndex idxRoots = db.getOrCreateNodeIndex("roots");
			IGraphNode n1 = db.createNode(null, "eobject");
			idxRoots.add(n1, null);
			tx.success();
		}
	}

	@Test
	public void deleteRecreate() throws Exception {
		IGraphNodeIndex idxRoots;
		IGraphNode x;
		try (IGraphTransaction tx = db.beginTransaction()) {
			idxRoots = db.getOrCreateNodeIndex("roots");
			x = db.createNode(null, "eobject");
			idxRoots.add(x, "a", "1");
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(1, idxRoots.query("a", "1").size());
			idxRoots.delete();
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			idxRoots = db.getOrCreateNodeIndex("roots");
			assertEquals(0, idxRoots.query("a", "*").size());
			idxRoots.add(x, "a", "2");
			tx.success();
		}
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, idxRoots.query("a", "1").size());
			assertEquals(1, idxRoots.query("a", "2").size());
		}
	}

	@Test
	public void nodeDelete() throws Exception {
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n = db.createNode(null, "x");
			db.getMetamodelIndex().add(n, "a", 1);
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> results = db.getMetamodelIndex().get("a", 1);
			assertEquals(1, results.size());
			results.getSingle().delete();
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> results = db.getMetamodelIndex().get("a", 1);
			assertEquals(0, results.size());
			tx.success();
		}
	}

	@Test
	public void indexKeyAdditionRollback() throws Exception {
		IGraphNode n;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(null, "x");
			db.getMetamodelIndex().add(n, "b", 1);
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().add(n, "a", 1);
			tx.failure();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, db.getMetamodelIndex().query("a", "*").size());
			assertEquals(1, db.getMetamodelIndex().query("b", "*").size());
			tx.success();
		}
	}

	@Test
	public void indexKeyRemovalRollback() throws Exception {
		IGraphNode n;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(null, "x");
			db.getMetamodelIndex().add(n, "b", 1);
			db.getMetamodelIndex().add(n, "c", "x");
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(n, "b", 1);
			db.getMetamodelIndex().remove(n, "c", "x");
			assertEquals(0, db.getMetamodelIndex().query("b", 1).size());
			assertEquals(0, db.getMetamodelIndex().query("c", "x").size());
			tx.failure();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(1, db.getMetamodelIndex().query("b", 1).size());
			assertEquals(1, db.getMetamodelIndex().query("c", "x").size());
			tx.success();
		}
	}

	private String populateForRemove() throws Exception {
		final String mmBarURI = "http://foo/bar";
		final Map<String, Object> mmBarNodeProps = Collections.singletonMap(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			final IGraphNodeIndex mmIdx = db.getMetamodelIndex();
			mmIdx.add(mmNode, Collections.singletonMap("id", mmBarURI));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			mmIdx.add(mmNode2, Collections.singletonMap("id", "file://foo"));

			tx.success();
		}
		return mmBarURI;
	}

	private void checkAfterRemove(final String mmBarURI) throws Exception {
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphNodeIndex mmIdx = db.getMetamodelIndex();

			assertEquals(0, mmIdx.query("id", mmBarURI).size());
			assertEquals(0, mmIdx.query("id", "http://*").size());
			assertEquals(1, mmIdx.query("id", "file://*").size());
			assertEquals(1, mmIdx.query("id", "fil*://*").size());
			assertEquals(1, mmIdx.query("id", "*file://*").size());
		}
	}

	private IGraphIterable<IGraphNode> checkBeforeRemove(final String mmBarURI) throws Exception {
		try (IGraphTransaction tx = db.beginTransaction()) {
			final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", mmBarURI);
			assertEquals(1, iter.size());
			tx.success();
			return iter;
		}
	}

	private int iteratorSize(Iterator<?> it) {
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}
		return count;
	}
}
