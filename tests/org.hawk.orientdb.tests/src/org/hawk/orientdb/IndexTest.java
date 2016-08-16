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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.hawk.graph.ModelElementNode;
import org.hawk.orientdb.util.FluidMap;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for populating and querying indices.
 */
public class IndexTest {

	protected OrientDatabase db;

	public void setup(String testCase) throws Exception {
		db = new OrientDatabase();
		db.run("memory:index_test_" + testCase, null, new DefaultConsole());
	}

	@After
	public void teardown() throws Exception {
		db.delete();
	}

	@Test
	public void query() throws Exception {
		setup("query");

		final String mmBarURI = "http://foo/bar";
		final String mmFileURI = "file://a/b/c.d";
		final FluidMap mmBarNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);
		final FluidMap mmFileNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmFileURI);

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmNode, FluidMap.create().add("id", mmBarURI));

			IGraphNode mmFileNode = db.createNode(mmFileNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmFileNode, FluidMap.create().add("id", mmFileURI));

			tx.success();
		}

		// Query with partial wildcard
		final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", "http://*");
		assertSame(db, iter.getSingle().getGraph());
		assertEquals(1, iter.size());

		// Query with full value
		final IGraphIterable<IGraphNode> iter3 = db.getMetamodelIndex().query("id", mmBarURI);
		assertSame(db, iter3.getSingle().getGraph());
		assertEquals(1, iter3.size());
		assertEquals(mmBarURI, iter3.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		// Query with wildcard on one field
		final IGraphIterable<IGraphNode> iter4 = db.getMetamodelIndex().query("id", "*");
		assertSame(db, iter4.getSingle().getGraph());
		assertEquals(2, iter4.size());

		// Retrieve with full value
		final IGraphIterable<IGraphNode> iter2 = db.getMetamodelIndex().get("id", mmFileURI);
		assertEquals(1, iter2.size());
		assertEquals(mmFileURI, iter2.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		// Use */* (all fields, all values): not available with OrientDB?
		assertEquals(new HashSet<>(Arrays.asList(mmBarURI, mmFileURI)), db.getKnownMMUris());
	}

	@Test
	public void removeByNode() throws Exception {
		setup("removeByNode");
		final String mmBarURI = populateForRemove();

		// NOTE: changes in Lucene indexes are not complete until we commit the transaction
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByFullKey() throws Exception {
		setup("removeByFullKey");
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove("id", mmBarURI, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByValueNode() throws Exception {
		setup("removeByValueNode");
		final String mmBarURI = populateForRemove();
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(null, mmBarURI, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByFieldNode() throws Exception {
		setup("removeByFieldNode");
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove("id", null, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByNode3Arg() throws Exception {
		setup("removeByNode3Arg");
		final String mmBarURI = populateForRemove();

		// NOTE: changes in Lucene indexes are not complete until we commit the transaction
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(null, null, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void integerRanges() throws Exception {
		setup("removeByIntegerRanges");
		final String mmBarURI = "http://foo/bar";
		final FluidMap mmBarNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, FluidMap.create().add("value", 5));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, FluidMap.create().add("value", 8));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, FluidMap.create().add("value", 1));
			tx.success();
		}

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
	}

	@Test
	public void floatingRanges() throws Exception {
		setup("floatingRanges");
		final String mmBarURI = "http://foo/bar";
		final FluidMap mmBarNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		final IGraphNodeIndex idx = db.getMetamodelIndex();
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode, FluidMap.create().add("value", 5.3));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode2, FluidMap.create().add("value", 8.1d));

			IGraphNode mmNode3 = db.createNode(mmBarNodeProps, "metamodel");
			idx.add(mmNode3, FluidMap.create().add("value", 1.34));
			tx.success();
		}

		assertEquals(3, idx.query("value", 0.0, 10.0, false, false).size());
		assertEquals(3, idx.query("value", 1.3, 8.2, false, false).size());
		assertEquals(2, idx.query("value", 1.4, 8.2, false, false).size());
		assertEquals(2, idx.query("value", 1.3, 8.0, false, false).size());
		assertEquals(1, idx.query("value", 1.4, 8.0, false, false).size());
		assertEquals(1, idx.query("value", 5.2, 5.4, false, false).size());
		assertEquals(0, idx.query("value", 5.2, 5.2, false, false).size());
		assertEquals(0, idx.query("value", 5.2, 5.1, false, false).size());
	}

	@Test
	public void invalidIndexNames() throws Exception {
		// OIndexManagerShared#createIndex checks for these
		final char[] invalidChars = ":,; %=/\\".toCharArray();
		setup("invalidIndexNames");

		OrientNode n;
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(null, "eobject");
			tx.success();
		}
		for (char invalidChar : invalidChars) {
			final String name = "my" + invalidChar + "index";
			try (IGraphTransaction tx = db.beginTransaction()) {
				IGraphNodeIndex idx = db.getOrCreateNodeIndex(name);
				idx.add(n, "id", 1);
				tx.success();
			}

			assertTrue(db.getNodeIndexNames().contains(name));
			assertTrue(db.getIndexStore().getNodeFieldIndexNames(name).contains("id"));
			IGraphNodeIndex idx = db.getOrCreateNodeIndex(name);

			IGraphIterable<IGraphNode> results = idx.query("id", 1);
			assertEquals(1, results.size());
			results = idx.query("id", 0, 1, true, true);
			assertEquals(1, results.size());
		}
		for (char invalidChar : invalidChars) {
			final String name = "my" + invalidChar + "index";
			db.getOrCreateEdgeIndex(name);
			assertTrue(db.getEdgeIndexNames().contains(name));
			// TODO: no methods to add things in IGraphEdgeIndex?
		}
	}

	@Test
	public void getMultipleMatches() throws Exception {
		setup("getMultipleMatches");

		IGraphNodeIndex idxRoots = db.getOrCreateNodeIndex("roots");
		try (IGraphTransaction tx = db.beginTransaction()) {
			OrientNode n1 = db.createNode(null, "eobject");
			OrientNode n2 = db.createNode(null, "eobject");
			OrientNode n3 = db.createNode(null, "eobject");
			OrientNode f1 = db.createNode(null, "file");
			OrientNode f2 = db.createNode(null, "file");

			db.createRelationship(n1, f1, ModelElementNode.EDGE_LABEL_FILE);
			db.createRelationship(n2, f1, ModelElementNode.EDGE_LABEL_FILE);
			db.createRelationship(n3, f2, ModelElementNode.EDGE_LABEL_FILE);
			idxRoots.add(n1, "file", "a.xmi");
			idxRoots.add(n2, "file", "a.xmi");
			idxRoots.add(n3, "file", "b.xmi");
			
			tx.success();
		}

		final IGraphIterable<IGraphNode> aRoots = idxRoots.get("file", "a.xmi");
		assertEquals(2, aRoots.size());
		assertEquals(2, iteratorSize(aRoots.iterator()));
		assertNotNull(aRoots.getSingle());

		final IGraphIterable<IGraphNode> bRoots = idxRoots.get("file", "b.xmi");
		assertEquals(1, bRoots.size());
		assertEquals(1, iteratorSize(bRoots.iterator()));
		assertNotNull(bRoots.getSingle());

		final IGraphIterable<IGraphNode> cRoots = idxRoots.get("file", "c.xmi");
		assertEquals(0, cRoots.size());
		assertEquals(0, iteratorSize(cRoots.iterator()));
		try {
			cRoots.getSingle();
			fail("NoSuchElementException should have been thrown");
		} catch (NoSuchElementException ex) {
			// good!
		}
	}

	@Test
	public void addNullValue() throws Exception {
		setup("addNullMap");

		IGraphNodeIndex idxRoots = db.getOrCreateNodeIndex("roots");
		try (IGraphTransaction tx = db.beginTransaction()) {
			OrientNode n1 = db.createNode(null, "eobject");
			idxRoots.add(n1, "file", null);
			tx.success();
		}
	}

	@Test
	public void addNullMap() throws Exception {
		setup("addNullMap");

		IGraphNodeIndex idxRoots = db.getOrCreateNodeIndex("roots");
		try (IGraphTransaction tx = db.beginTransaction()) {
			OrientNode n1 = db.createNode(null, "eobject");
			idxRoots.add(n1, null);
			tx.success();
		}
	}

	private String populateForRemove() {
		final String mmBarURI = "http://foo/bar";
		final FluidMap mmBarNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmNode, FluidMap.create().add("id", mmBarURI));

			IGraphNode mmNode2 = db.createNode(mmBarNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmNode2, FluidMap.create().add("id", "file://foo"));

			tx.success();
		}
		return mmBarURI;
	}

	private void checkAfterRemove(final String mmBarURI) {
		final IGraphIterable<IGraphNode> iter2 = db.getMetamodelIndex().query("id", mmBarURI);
		assertEquals(0, iter2.size());
		final IGraphIterable<IGraphNode> iter3 = db.getMetamodelIndex().query("id", "http://*");
		assertEquals(0, iter3.size());
		final IGraphIterable<IGraphNode> iter4 = db.getMetamodelIndex().query("id", "file://*");
		assertEquals(1, iter4.size());
		final IGraphIterable<IGraphNode> iter5 = db.getMetamodelIndex().query("id", "fil*://*");
		assertEquals(1, iter5.size());
		final IGraphIterable<IGraphNode> iter6 = db.getMetamodelIndex().query("id", "*file://*");
		assertEquals(1, iter6.size());
	}

	private IGraphIterable<IGraphNode> checkBeforeRemove(final String mmBarURI) {
		final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", mmBarURI);
		assertEquals(1, iter.size());
		return iter;
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
