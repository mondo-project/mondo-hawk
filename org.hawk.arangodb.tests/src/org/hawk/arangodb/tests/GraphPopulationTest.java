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
package org.hawk.arangodb.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawk.arangodb.ArangoDatabase;
import org.hawk.arangodb.ArangoNode;
import org.hawk.arangodb.ArangoTransaction;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.junit.After;
import org.junit.Test;

import com.arangodb.util.MapBuilder;

/**
 * Tests for populating the graph with nodes and vertices and navigating it.
 */
public class GraphPopulationTest {

	protected ArangoDatabase db;

	public void setup(final String testCase) throws IOException, Exception {
		db = new ArangoDatabase();
		db.run(File.createTempFile("arango", "db"), new DefaultConsole());
	}

	@After
	public void teardown() throws Exception {
		db.delete();
	}

	@Test
	public void oneNode() throws Exception {
		setup("oneNode");
		assertEquals(0, db.allNodes("eobject").size());
		db.createNode(new HashMap<String, Object>(), "eobject");
		assertEquals(1, db.allNodes("eobject").size());
	}

	@Test
	public void oneNodeProperty() throws Exception {
		setup("oneNodeProperty");
		IGraphIterable<IGraphNode> eobs = db.allNodes("eobject");
		assertEquals(0, eobs.size());
		ArangoNode n;
		final String yValue = "hello";
		try (IGraphTransaction tx = db.beginTransaction()) {
			n = db.createNode(new MapBuilder().put("x", 1.34).put("y", yValue).get(), "eobject");
			tx.success();
		}
		assertTrue((double)eobs.getSingle().getProperty("x") > 1.3);
		assertEquals(1, eobs.size());

		try (IGraphTransaction tx = db.beginTransaction()) {
			n.setProperty("x", 2.57);
			tx.success();
		}
		assertTrue((double)eobs.getSingle().getProperty("x") > 2.5);

		try (IGraphTransaction tx = db.beginTransaction()) {
			n.setProperty("x", null);
			tx.success();
		}
		assertNull(eobs.getSingle().getProperty("x"));
		assertEquals(yValue, eobs.getSingle().getProperty("y"));

		try (IGraphTransaction tx = db.beginTransaction()) {
			n.removeProperty("y");
			tx.success();
		}
		assertNull(eobs.getSingle().getProperty("y"));
	}

	@Test
	public void oneNodeBatch() throws Exception {
		setup("oneNodeBatch");
		db.enterBatchMode();

		final String idValue = "http://foo.bar";

		// OSchemaShared#checkFieldNameIfValid refers to these invalid characters
		char[] invalidChars = ":,; %=".toCharArray();
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put("id", idValue);
		for (char invalidChar : invalidChars) {
			props.put("my" + invalidChar + "value", invalidChar + "");
		}

		ArangoNode n = db.createNode(props, "metamodel");
		assertEquals(props.keySet(), n.getPropertyKeys());
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			assertEquals(entry.getValue(), n.getProperty(entry.getKey()));
		}
		db.exitBatchMode();
		assertEquals(1, db.allNodes("metamodel").size());
	}

	@Test
	public void oneNodeRemove() throws Exception {
		setup("oneNodeRemove");
		assertEquals(0, db.allNodes("eobject").size());
		ArangoNode n;
		try (IGraphTransaction tx = db.beginTransaction()) { 
			n = db.createNode(null, "eobject");
			tx.success();
		}
		assertEquals(1, db.allNodes("eobject").size());
		try (IGraphTransaction tx = db.beginTransaction()) {
			n.delete();
			tx.success();
		}
		assertEquals(0, db.allNodes("eobject").size());
	}

	@Test
	public void oneNodeRemoveRollback() throws Exception {
		setup("oneNodeRemoveRollback");
		assertEquals(0, db.allNodes("eobject").size());
		ArangoNode n;
		try (IGraphTransaction tx = db.beginTransaction()) { 
			n = db.createNode(null, "eobject");
			tx.success();
		}
		assertEquals(1, db.allNodes("eobject").size());
		try (IGraphTransaction tx = db.beginTransaction()) {
			n.delete();
			tx.failure();
		}
		assertEquals(1, db.allNodes("eobject").size());
	}

	@Test
	public void twoNodesBatch() throws Exception {
		setup("twoNodesBatch");

		db.enterBatchMode();
		IGraphNode x1 = db.createNode(new MapBuilder().put("x", 1).get(), "eobject");
		IGraphNode x10 = db.createNode(new MapBuilder().put("x", 10).get(), "eobject");
		IGraphEdge e = db.createRelationship(x1, x10, "dep", new MapBuilder().put("y", "abc").get());
		db.exitBatchMode();

		assertEquals(1, e.getStartNode().getProperty("x"));
		assertEquals(10, e.getEndNode().getProperty("x"));
		assertEquals("abc", e.getProperty("y"));
		assertEquals("dep", e.getType());
		assertEquals(1, size(x1.getOutgoingWithType("dep")));
		assertEquals(1, size(x10.getIncomingWithType("dep")));
	}

	@Test
	public void twoNodesBatchRemoveRel() throws Exception {
		setup("twoNodesBatchRemoveRel");

		db.enterBatchMode();
		IGraphNode x1 = db.createNode(new MapBuilder().put("x", 1).get(), "eobject");
		IGraphNode x10 = db.createNode(new MapBuilder().put("x", 10).get(), "eobject");
		IGraphEdge e = db.createRelationship(x1, x10, "dep", new MapBuilder().put("y", "abc").get());
		db.exitBatchMode();

		db.enterBatchMode();
		e.delete();
		db.exitBatchMode();

		assertEquals(0, size(x1.getOutgoingWithType("dep")));
		assertEquals(0, size(x10.getIncomingWithType("dep")));
	}

	@Test
	public void threeNodesRemoveMiddle() throws Exception {
		setup("twoNodesBatchRemoveMiddle");

		IGraphNode left, middle, right;
		try (IGraphTransaction tx = db.beginTransaction()) {
			left = db.createNode(null, "eobject");
			middle = db.createNode(null, "eobject");
			right = db.createNode(null, "eobject");
			db.createRelationship(left, middle, "x", null);
			db.createRelationship(middle, right, "x", null);
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, size(left.getIncoming()));
			assertEquals(1, size(left.getOutgoing()));
			assertEquals(1, size(middle.getIncoming()));
			assertEquals(1, size(middle.getOutgoing()));
			assertEquals(1, size(right.getIncoming()));
			assertEquals(0, size(right.getOutgoing()));
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			middle.delete();
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(0, size(left.getIncoming()));
			assertEquals(0, size(left.getOutgoing()));
			assertEquals(0, size(right.getIncoming()));
			assertEquals(0, size(right.getOutgoing()));
		}
	}

	@Test
	public void escapeInvalidClassCharacters() throws Exception {
		setup("invalidClassCharacters");

		char[] invalidClassChars = ":,; %@=.".toCharArray();
		for (char invalidChar : invalidClassChars) {
			db.enterBatchMode();
			final String nodeType = "my" + invalidChar + "object";
			db.createNode(null, nodeType);
			assertEquals(1, db.allNodes(nodeType).size());
			db.exitBatchMode();
		}
	}

	@Test
	public void escapeInvalidCharactersEdges() throws Exception {
		setup("invalidClassCharactersEdges");

		db.enterBatchMode();
		ArangoNode n1 = db.createNode(null, "eobject");
		ArangoNode n2 = db.createNode(null, "eobject");
		db.exitBatchMode();

		char[] invalidClassChars = ":,; %@=.".toCharArray();
		for (char invalidChar : invalidClassChars) {
			final String type = "link" + invalidChar + "type";
			db.enterBatchMode();
			db.createRelationship(n1, n2, type, Collections.<String,Object>singletonMap("test", "t"));
			assertEquals("There should be one outgoing " + type + " edge for n1", 1, size(n1.getOutgoingWithType(type)));
			assertEquals("There should be one incoming " + type + " edge for n2", 1, size(n2.getIncomingWithType(type)));
			db.exitBatchMode();
		}
	}

	@Test
	public void escapeInvalidFieldCharacters() throws Exception {
		setup("invalidFieldCharactersEdges");

		db.enterBatchMode();
		ArangoNode n1 = db.createNode(null, "eobject");
		db.exitBatchMode();

		char[] invalidClassChars = ":,; %@=.".toCharArray();
		for (char invalidChar : invalidClassChars) {
			db.enterBatchMode();
			final String propName = "x" + invalidChar + "a";
			n1.setProperty(propName, 1);
			assertEquals("Property " + propName + " should have been set", 1, n1.getProperty(propName));
			db.exitBatchMode();
		}
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
