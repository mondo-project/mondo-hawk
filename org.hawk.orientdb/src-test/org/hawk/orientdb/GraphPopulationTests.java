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
import java.util.HashMap;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.util.FluidMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for populating the graph with nodes and vertices and navigating it.
 */
public class GraphPopulationTests {

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
	public void oneNode() {
		assertEquals(0, db.allNodes("eobject").size());
		db.createNode(new HashMap<String, Object>(), "eobject");
		assertEquals(1, db.allNodes("eobject").size());
	}

	@Test
	public void oneNodeBatch() {
		db.enterBatchMode();
		db.createNode(new HashMap<String, Object>(), "metamodel");
		db.exitBatchMode();
		assertEquals(1, db.allNodes("metamodel").size());
	}

	@Test
	public void oneNodeRollback() {
		try (OrientTransaction tx = db.beginTransaction()) {
			db.createNode(new HashMap<String, Object>(), "metamodel");
			assertEquals(1, db.allNodes("metamodel").size());
			tx.failure();
		}
		assertEquals(0, db.allNodes("metamodel").size());
	}

	@Test
	public void twoNodesBatch() {
		db.enterBatchMode();
		IGraphNode x1 = db.createNode(FluidMap.create().add("x", 1), "eobject");
		IGraphNode x10 = db.createNode(FluidMap.create().add("x", 10), "eobject");
		IGraphEdge e = db.createRelationship(x1, x10, "dep", FluidMap.create().add("y", "abc"));
		db.exitBatchMode();

		assertEquals(1, e.getStartNode().getProperty("x"));
		assertEquals(10, e.getEndNode().getProperty("x"));
		assertEquals("abc", e.getProperty("y"));
	}
}
