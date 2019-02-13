/*******************************************************************************
 * Copyright (c) 2018 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.LogbackOnlyErrorsRule;
import org.hawk.backend.tests.RedirectSystemErrorRule;
import org.hawk.backend.tests.TemporaryDatabaseTest;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for timeaware backends, on the interaction of timepoints with index
 * queries and the history of edges and nodes.
 */
@RunWith(Parameterized.class)
public class TimeAwareBackendTest extends TemporaryDatabaseTest {

	private ITimeAwareGraphDatabase taDB;

	public TimeAwareBackendTest(IGraphDatabaseFactory dbf) {
		super(dbf);
	}

	@Rule
	public RedirectSystemErrorRule errRule = new RedirectSystemErrorRule();

	@Rule
	public LogbackOnlyErrorsRule logRule = new LogbackOnlyErrorsRule();

	@Parameters(name="{0}")
	public static Object[] params() {
		return BackendTestSuite.timeAwareBackends();
	}

	@Override
	public void setup() throws Exception {
		super.setup();
		taDB = (ITimeAwareGraphDatabase)db;
	}

	@Test
	public void indexQueryEndedNodes() throws Exception {
		Object nodeId;
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1T0 = db.createNode(Collections.singletonMap("x", 1), "test");
			db.getMetamodelIndex().add(n1T0, Collections.singletonMap("key", "foo"));
			nodeId = n1T0.getId();
			tx.success();
		}

		// same instant, check we have it
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphIterable<? extends IGraphNode> iterable = db.getMetamodelIndex().query("key", "foo");
			assertEquals(1, iterable.size());
			assertEquals(nodeId, iterable.getSingle().getId());
			assertTrue(iterable.iterator().hasNext());
			tx.success();
		}

		// end at instant 1, should still have it from the index
		taDB.setTime(1L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1T1 = db.getNodeById(nodeId);
			((ITimeAwareGraphNode)n1T1).end();
			IGraphIterable<? extends IGraphNode> iterable = db.getMetamodelIndex().query("key", "foo");
			assertEquals(1, iterable.size());
			assertEquals(nodeId, iterable.getSingle().getId());
			assertTrue(iterable.iterator().hasNext());
			tx.success();
		}

		// should not appear from instant 2 onwards
		taDB.setTime(2L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphIterable<? extends IGraphNode> iterable = db.getMetamodelIndex().query("key", "foo");
			assertEquals(0, iterable.size());
			assertFalse(iterable.iterator().hasNext());
			try {
				iterable.getSingle();
				fail("Should throw a NoSuchElementException");
			} catch (NoSuchElementException ex) {
				// pass
			}
			tx.success();
		}

		// move to past, should still have it
		taDB.setTime(0L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphIterable<? extends IGraphNode> iterable = db.getMetamodelIndex().query("key", "foo");
			assertEquals(1, iterable.size());
			assertEquals(nodeId, iterable.getSingle().getId());
			assertTrue(iterable.iterator().hasNext());
			tx.success();
		}
	}

	@Test
	public void indexQueryReplacedNodes() throws Exception {
		Object nodeId;
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1 = db.createNode(Collections.emptyMap(), "test");
			nodeId = n1.getId();
			db.getMetamodelIndex().add(n1, Collections.singletonMap("file", "abc.xmi"));
			tx.success();
		}

		taDB.setTime(1L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			taDB.getNodeById(nodeId).end();
			tx.success();
		}

		taDB.setTime(2L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n2 = db.createNode(Collections.emptyMap(), "test");
			db.getMetamodelIndex().add(n2, Collections.singletonMap("file", "abc.xmi"));
			nodeId = n2.getId();

			IGraphIterable<? extends IGraphNode> iterable = db.getMetamodelIndex().query("file", "abc.xmi");
			assertEquals(1, iterable.size());
			assertEquals(nodeId, iterable.iterator().next().getId());
			assertEquals(nodeId, iterable.getSingle().getId());

			tx.success();
		}
	}

	@Test
	public void nodeEndWithLightEdge() throws Exception {
		nodeEndWithEdges(Collections.emptyMap());
	}

	@Test
	public void nodeEndWithHeavyEdge() throws Exception {
		nodeEndWithEdges(Collections.singletonMap("container", "true"));
	}

	private void nodeEndWithEdges(final Map<String, Object> props) throws Exception {
		Object id1, id2;
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1 = db.createNode(null, "example");
			IGraphNode n2 = db.createNode(null, "example");
			db.createRelationship(n1, n2, "test", props);

			id1 = n1.getId();
			id2 = n2.getId();
			tx.success();
		}

		taDB.setTime(1L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			ITimeAwareGraphNode n1 = taDB.getNodeById(id1);
			ITimeAwareGraphNode n2 = taDB.getNodeById(id2);
			assertTrue(n1.getOutgoing().iterator().hasNext());
			assertTrue(n2.getIncoming().iterator().hasNext());
			n2.end();
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			ITimeAwareGraphNode n1 = taDB.getNodeById(id1);
			ITimeAwareGraphNode n2 = taDB.getNodeById(id2);
			assertTrue(n1.getOutgoing().iterator().hasNext());
			assertTrue(n2.getIncoming().iterator().hasNext());
			tx.success();
		}

		taDB.setTime(0L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			ITimeAwareGraphNode n1 = taDB.getNodeById(id1);
			ITimeAwareGraphNode n2 = taDB.getNodeById(id2);
			assertTrue(n1.getOutgoing().iterator().hasNext());
			assertTrue(n2.getIncoming().iterator().hasNext());
			tx.success();
		}
		
		taDB.setTime(2L);
		try (IGraphTransaction tx = db.beginTransaction()) {
			ITimeAwareGraphNode n1 = taDB.getNodeById(id1);
			ITimeAwareGraphNode n2 = taDB.getNodeById(id2);
			assertFalse(n1.getOutgoing().iterator().hasNext());
			assertFalse(n2.isAlive());
			tx.success();
		}
	}
	
	@Test
	public void createDeleteHeavyEdgeBeginningOfTime() throws Exception {
		taDB.setTime(0L);
		createDeleteHeavyEdge();
	}

	@Test
	public void createDeleteHeavyEdgeNonZeroTime() throws Exception {
		taDB.setTime(1L);
		createDeleteHeavyEdge();
	}

	private void createDeleteHeavyEdge() throws Exception {
		Object heavyEdgeId;
		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode n1 = db.createNode(null, "example");
			IGraphNode n2 = db.createNode(null, "example");
			IGraphEdge e = db.createRelationship(n1, n2, "example", Collections.singletonMap("key", "value"));
			heavyEdgeId = e.getId();
			e.delete();
			tx.success();
		}

		try (IGraphTransaction tx = db.beginTransaction()) {
			ITimeAwareGraphNode backingNode = taDB.getNodeById(heavyEdgeId);
			assertFalse(backingNode.isAlive());
			tx.success();
		}
	}
	
	/* TODO add test cases for derived/indexed properties in combination with time-awareness */
}
