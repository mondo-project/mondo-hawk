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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.util.FluidMap;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for populating the graph with nodes and vertices and navigating it.
 */
public class GraphPopulationTest {

	private OrientDatabase db;

	@After
	public void teardown() throws Exception {
		db.delete();
	}

	@Test
	public void oneNode() {
		db = new OrientDatabase();
		db.run("memory:oneNode", null, new DefaultConsole());
		assertEquals(0, db.allNodes("eobject").size());
		db.createNode(new HashMap<String, Object>(), "eobject");
		assertEquals(1, db.allNodes("eobject").size());
	}

	@Test
	public void oneNodeBatch() {
		db = new OrientDatabase();
		db.run("memory:oneNodeBatch", null, new DefaultConsole());
		db.enterBatchMode();

		final String idValue = "http://foo.bar";

		// OSchemaShared#checkFieldNameIfValid refers to these invalid characters
		char[] invalidChars = ":,; %=".toCharArray();
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put("id", idValue);
		for (char invalidChar : invalidChars) {
			props.put("my" + invalidChar + "value", invalidChar + "");
		}

		OrientNode n = db.createNode(props, "metamodel");
		assertEquals(props.keySet(), n.getPropertyKeys());
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			assertEquals(entry.getValue(), n.getProperty(entry.getKey()));
		}
		db.exitBatchMode();
		assertEquals(1, db.allNodes("metamodel").size());
	}

	@Test
	public void oneNodeRollback() {
		db = new OrientDatabase();
		db.run("memory:oneNodeRollback", null, new DefaultConsole());
		try (OrientTransaction tx = db.beginTransaction()) {
			db.createNode(new HashMap<String, Object>(), "metamodel");
			assertEquals(1, db.allNodes("metamodel").size());
			tx.failure();
		}
		assertEquals(0, db.allNodes("metamodel").size());
	}

	@Test
	public void twoNodesBatch() {
		db = new OrientDatabase();
		db.run("memory:twoNodesBatch", null, new DefaultConsole());

		db.enterBatchMode();
		IGraphNode x1 = db.createNode(FluidMap.create().add("x", 1), "eobject");
		IGraphNode x10 = db.createNode(FluidMap.create().add("x", 10), "eobject");
		IGraphEdge e = db.createRelationship(x1, x10, "dep", FluidMap.create().add("y", "abc"));
		db.exitBatchMode();

		assertEquals(1, e.getStartNode().getProperty("x"));
		assertEquals(10, e.getEndNode().getProperty("x"));
		assertEquals("abc", e.getProperty("y"));
		assertEquals("dep", e.getType());
		assertEquals(1, size(x1.getOutgoingWithType("dep")));
		assertEquals(1, size(x10.getIncomingWithType("dep")));
	}

	private <T> int size(Iterable<T> it) {
		Iterator<T> iterator = it.iterator();
		int count = 0;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
	}
}
