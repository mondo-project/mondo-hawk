/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.Iterator;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.LogbackOnlyErrorsRule;
import org.hawk.backend.tests.RedirectSystemErrorRule;
import org.hawk.backend.tests.TemporaryDatabaseTest;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndexFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for version indices in time-aware backends.
 */
@RunWith(Parameterized.class)
public class VersionIndexTest extends TemporaryDatabaseTest {

	private ITimeAwareGraphDatabase taDB;
	private ITimeAwareGraphNodeVersionIndexFactory factory;

	public VersionIndexTest(IGraphDatabaseFactory dbf) {
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

		assumeTrue(db instanceof ITimeAwareGraphNodeVersionIndexFactory);
		this.factory = (ITimeAwareGraphNodeVersionIndexFactory) db;
	}

	@Test
	public void createDeleteIndex() throws Exception {
		final String idxName = "Dummy";

		assertFalse(factory.versionIndexExists(idxName));
		ITimeAwareGraphNodeVersionIndex idx = factory.getOrCreateVersionIndex(idxName);
		assertTrue(factory.versionIndexExists(idxName));
		idx.delete();
		assertFalse(factory.versionIndexExists(idxName));
	}

	@Test
	public void noVersions() throws Exception {
		ITimeAwareGraphNode n1;
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			n1 = taDB.createNode(Collections.singletonMap("x", 1), "test");
			tx.success();
		}

		final ITimeAwareGraphNodeVersionIndex idx = factory.getOrCreateVersionIndex("Dummy");
		assertFalse(idx.getAllVersions(n1).iterator().hasNext());
	}

	@Test
	public void oneVersion() throws Exception {
		ITimeAwareGraphNode n1;
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			n1 = taDB.createNode(Collections.singletonMap("x", 1), "test");
			tx.success();
		}

		final ITimeAwareGraphNodeVersionIndex idx = factory.getOrCreateVersionIndex("OneVersion");

		idx.addVersion(n1);
		assertHasNodes(idx.getAllVersions(n1), n1);
		assertHasNodes(idx.getVersionsSince(n1), n1);
		assertHasNodes(idx.getVersionsUntil(n1), n1);
		assertEmpty(idx.getVersionsAfter(n1));
		assertEmpty(idx.getVersionsBefore(n1));

		idx.removeAllVersions(n1);
		assertEmpty(idx.getAllVersions(n1));
		assertEmpty(idx.getVersionsSince(n1));
		assertEmpty(idx.getVersionsUntil(n1));
		assertEmpty(idx.getVersionsAfter(n1));
		assertEmpty(idx.getVersionsBefore(n1));
	}

	@Test
	public void twoVersionsOneAdded() throws Exception {
		ITimeAwareGraphNode n1T0, n1T5;
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			n1T0 = taDB.createNode(Collections.singletonMap("x", 1), "test");
			n1T5 = n1T0.travelInTime(5);
			n1T5.setProperty("x", 10);
			tx.success();
		}

		final ITimeAwareGraphNodeVersionIndex idx = factory.getOrCreateVersionIndex("OneVersion");

		idx.addVersion(n1T5);
		assertHasNodes(idx.getAllVersions(n1T0), n1T5);
		assertHasNodes(idx.getVersionsSince(n1T0), n1T5);
		assertHasNodes(idx.getVersionsAfter(n1T0), n1T5);
		assertEmpty(idx.getVersionsUntil(n1T0));
		assertEmpty(idx.getVersionsBefore(n1T0));
		
		assertHasNodes(idx.getAllVersions(n1T5), n1T5);
		assertHasNodes(idx.getVersionsSince(n1T5), n1T5);
		assertEmpty(idx.getVersionsAfter(n1T5));
		assertHasNodes(idx.getVersionsUntil(n1T5), n1T5);
		assertEmpty(idx.getVersionsBefore(n1T5));
	}

	@Test
	public void indexSeparation() throws Exception {
		ITimeAwareGraphNode n1T0, n1T5;
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			n1T0 = taDB.createNode(Collections.singletonMap("x", 1), "test");
			n1T5 = n1T0.travelInTime(5);
			n1T5.setProperty("x", 10);
			tx.success();
		}

		final ITimeAwareGraphNodeVersionIndex idxA = factory.getOrCreateVersionIndex("A");
		final ITimeAwareGraphNodeVersionIndex idxB = factory.getOrCreateVersionIndex("B");

		idxA.addVersion(n1T0);
		idxB.addVersion(n1T5);
		assertHasNodes(idxA.getAllVersions(n1T0), n1T0);
		assertHasNodes(idxB.getAllVersions(n1T5), n1T5);
		assertHasNodes(idxA.getVersionsSince(n1T0), n1T0);
		assertEmpty(idxA.getVersionsAfter(n1T0));
		assertHasNodes(idxA.getVersionsUntil(n1T5), n1T0);
		assertEmpty(idxB.getVersionsBefore(n1T5));

		idxA.removeAllVersions(n1T0);
		assertEmpty(idxA.getAllVersions(n1T0));
		assertHasNodes(idxB.getAllVersions(n1T5), n1T5);
	}

	@Test
	public void twoVersionsOneIgnoredOneRemoved() throws Exception {
		ITimeAwareGraphNode n1T0, n1T5, n1T6;
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			n1T0 = taDB.createNode(Collections.singletonMap("x", 1), "test");
			n1T5 = n1T0.travelInTime(5);
			n1T5.setProperty("x", 10);
			n1T6 = n1T5.travelInTime(6);
			n1T6.setProperty("banana", 5);
			tx.success();
		}

		final ITimeAwareGraphNodeVersionIndex idx = factory.getOrCreateVersionIndex("MySituation");
		idx.addVersion(n1T0);
		idx.addVersion(n1T6);
		assertHasNodes(idx.getAllVersions(n1T0), n1T0, n1T6);

		// Removing a version that was not of interest should not result in any changes
		idx.removeVersion(n1T5);
		assertHasNodes(idx.getAllVersions(n1T0), n1T0, n1T6);

		idx.removeVersion(n1T0);
		assertHasNodes(idx.getAllVersions(n1T0), n1T6);
	}

	// TODO add test about multiple indices not bothering each other on adds / deletes

	private static void assertHasNodes(Iterable<ITimeAwareGraphNode> iterable, ITimeAwareGraphNode... expectedNodes) {
		final Iterator<ITimeAwareGraphNode> itVersions = iterable.iterator();

		for (int i = 0; i < expectedNodes.length; i++) {
			assertTrue("Element " + i + " exists", itVersions.hasNext());

			final ITimeAwareGraphNode expectedNode = expectedNodes[i];
			final ITimeAwareGraphNode actualNode = itVersions.next();
			assertEquals("Element " + i + " has the same ID", expectedNode.getId(), actualNode.getId());
			assertEquals("Element " + i + " has the same time", expectedNode.getTime(), actualNode.getTime());
		}
		
		assertFalse("There are exactly " + expectedNodes.length + " elements", itVersions.hasNext());
	}

	private static void assertEmpty(Iterable<ITimeAwareGraphNode> iterable) {
		assertFalse(iterable.iterator().hasNext());
	}

}
