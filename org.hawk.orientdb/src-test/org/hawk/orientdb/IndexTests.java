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
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.util.FluidMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for populating and querying indices.
 */
public class IndexTests {

	private OrientDatabase db;

	@Before
	public void setup() {
		db = new OrientDatabase();
		db.run(new File("testdb"), new DefaultConsole());
	}

	@After
	public void teardown() throws Exception {
		db.delete();
	}

	@Test
	public void query() {
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
	public void removeByNode() {
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
	public void removeByFullKey() {
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove("id", mmBarURI, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByValueNode() {
		final String mmBarURI = populateForRemove();
		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove(null, mmBarURI, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByFieldNode() {
		final String mmBarURI = populateForRemove();

		final IGraphIterable<IGraphNode> iter = checkBeforeRemove(mmBarURI);
		try (IGraphTransaction tx = db.beginTransaction()) {
			db.getMetamodelIndex().remove("id", null, iter.getSingle());
			tx.success();
		}
		checkAfterRemove(mmBarURI);
	}

	@Test
	public void removeByNode3Arg() {
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
	public void integerRanges() {
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
	public void floatingRanges() {
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
	}

	private IGraphIterable<IGraphNode> checkBeforeRemove(final String mmBarURI) {
		final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", mmBarURI);
		assertEquals(1, iter.size());
		return iter;
	}

}
