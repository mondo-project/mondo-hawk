/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
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
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ScopedQueryTest extends ModelIndexingTest {

	private static final String MM_URI = "http://github.com/mondo-hawk/testing/xrefs";

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation = new GraphChangeListenerRule<>(
			new SyncValidationListener());

	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		return BackendTestSuite.caseParams();
	}

	public ScopedQueryTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	@Before
	public void prepare() throws Exception {
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/crossrefs.ecore"));
		requestFolderIndex(new File("resources/models/scopedQuery"));
	}

	@Test
	public void listFiles() throws Throwable {
		waitForSync(() -> {
			try (IGraphTransaction tx = db.beginTransaction()) {
				GraphWrapper gw = new GraphWrapper(db);

				Function<String, Supplier<Integer>> query = (String path) -> () -> (Integer) gw
						.getFileNodes(Collections.singleton("*"), Collections.singleton(path)).size();

				assertEquals(3, (int) query.apply("*").get());
				assertEquals(2, (int) query.apply("/subfolder/*").get());
				assertEquals(1, (int) query.apply("/subfolder/subfolder/*").get());

				return null;
			}
		});
	}

	@Test
	public void instanceCounts() throws Throwable {
		waitForSync(() -> {
			assertEquals(0, syncValidation.getListener().getTotalErrors());
			assertEquals("With no context, it should return all six elements", 6, eol("return Element.all.size;"));
			assertEquals("With file context '*', it should return all six elements", 6, eol("return Element.all.size;", fc("*")));
			assertEquals("With file context '/root.model', it should return only the two root elements",
					new HashSet<>(Arrays.asList(0, 1)), eol("return Element.all.id.asSet;", fc("/root.model")));

			assertEquals("With file context '/subfolder/*', it should return four elements", 4,
					eol("return Element.all.size;",	fc("/subfolder/*")));
			assertEquals("With file context '/subfolder/subfolder/*', it should return only two elements", 2,
					eol("return Element.all.size;",	fc("/subfolder/subfolder/*")));

			return null;
		});
	}

	@Test
	public void instanceCountsAllOf() throws Throwable {
		waitForSync(() -> {
			assertEquals(0, syncValidation.getListener().getTotalErrors());

			try (IGraphTransaction tx = db.beginTransaction()) {
				assertEquals("With no context, it should return all six elements of the exact type", 6,
						queryEngine.getAllOf("Element", ModelElementNode.EDGE_LABEL_OFTYPE).size());
				assertEquals("OFKIND does not include the exact type", 0,
						queryEngine.getAllOf("Element", ModelElementNode.EDGE_LABEL_OFKIND).size());

				assertEquals("With file context '*', it should return all six elements", 6,
						queryEngine.getAllOf(MM_URI, "Element", "*").size());
				assertEquals("With file context '/root.model', it should return only the two root elements", 2,
						queryEngine.getAllOf(MM_URI, "Element", "/root.model").size());
				assertEquals("With file context '/subfolder/*', it should return four elements", 4,
						queryEngine.getAllOf(MM_URI, "Element", "/subfolder/*").size());
				assertEquals("With file context '/subfolder/subfolder/*', it should return only two elements", 2,
						queryEngine.getAllOf(MM_URI, "Element", "/subfolder/subfolder/*").size());
				tx.success();
			}

			return null;
		});
	}
	
	@Test
	public void forwardRefs() throws Throwable {
		waitForSync(() -> {
			assertEquals(0, syncValidation.getListener().getTotalErrors());

			assertForwardRefs(null, 0, 1);
			assertForwardRefs(null, 1, 3);

			assertForwardRefs(null, 12, 1);
			assertForwardRefs("/subfolder/*", 12, 0);
			assertForwardRefs(null, 15, 2);
			assertForwardRefs("/subfolder/*", 15, 1);

			assertForwardRefs(null, 23, 3);
			assertForwardRefs("/subfolder/*", 23, 2);
			assertForwardRefs("/subfolder/subfolder/*", 23, 1);
			assertForwardRefs(null, 27, 1);

			return null;
		});
	}

	@Test
	public void reverseRefs() throws Throwable {
		waitForSync(() -> {
			assertEquals(0, syncValidation.getListener().getTotalErrors());

			assertReverseRefs(null, 0, 3);
			assertReverseRefs(null, 1, 1);

			assertReverseRefs(null, 12, 1);
			assertReverseRefs("/subfolder/*", 12, 0);
			assertReverseRefs(null, 15, 3);
			assertReverseRefs("/subfolder/*", 15, 2);

			assertReverseRefs(null, 23, 2);
			assertReverseRefs("/subfolder/*", 23, 1);
			assertReverseRefs("/subfolder/subfolder/*", 23, 1);

			assertReverseRefs(null, 27, 1);
			assertReverseRefs("/subfolder/*", 27, 1);
			assertReverseRefs("/subfolder/subfolder/*", 27, 1);

			return null;
		});
	}

	private void assertForwardRefs(String path, int id, int expectedSize) throws InvalidQueryException, QueryExecutionException {
		Map<String, Object> context = fc(path);
		assertEquals(String.format("With context %s, element %d should see %d elements", context, id, expectedSize),
				expectedSize,
				eol(String.format("return Element.all.selectOne(e|e.id=%d).xrefs.size;", id), context));
	}

	private void assertReverseRefs(String path, int id, int expectedSize) throws InvalidQueryException, QueryExecutionException {
		Map<String, Object> context = fc(path);
		assertEquals(String.format("With context %s, element %d should be seen by %d elements", context, id, expectedSize),
				expectedSize,
				eol(String.format("return Element.all.selectOne(e|e.id=%d).revRefNav_xrefs.size;", id), context));
	}

	private Map<String, Object> fc(String path) {
		if (path == null) {
			return Collections.emptyMap();
		} else {
			Map<String, Object> map = new HashMap<>();
			map.put(CEOLQueryEngine.PROPERTY_FILECONTEXT, path);
			map.put(CEOLQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING, true + "");
			return map;
		}
	}
}
