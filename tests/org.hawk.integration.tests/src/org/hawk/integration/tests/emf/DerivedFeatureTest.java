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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class DerivedFeatureTest extends ModelIndexingTest {

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public DerivedFeatureTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	} 

	@Test
	public void derivedEdgeCollection() throws Throwable {
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree-dedges"));

		waitForSync(new Callable<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object call() throws Exception {
				indexer.addDerivedAttribute("Tree", "Tree", "descendants", "dummy", true, true, false,
						EOLQueryEngine.TYPE, "return self.closure(c|c.children).flatten;");

				// Forward derived edges
				{
					final Map<String, Integer> expected = new HashMap<>();
					expected.put("t3", 3);
					expected.put("t4", 1);
					expected.put("t5", 0);
					expected.put("t6", 0);

					for (Entry<String, Integer> entry : expected.entrySet()) {
						final Map<String, Object> context = Collections.singletonMap(EOLQueryEngine.PROPERTY_ARGUMENTS,
								(Object) Collections.singletonMap("nodeLabel", entry.getKey()));

						Object result = eol("return Tree.all.selectOne(t|t.label=nodeLabel).descendants.size;", context);
						assertEquals(
							String.format("%s should have %d descendants", entry.getKey(), entry.getValue()),
							entry.getValue(), result);
					}
				}

				// Reverse derived edges
				{
					final Map<String, Integer> reverseExpected = new HashMap<>();
					reverseExpected.put("t3", 0);
					reverseExpected.put("t4", 1);
					reverseExpected.put("t5", 1);
					reverseExpected.put("t6", 2);

					for (Entry<String, Integer> entry : reverseExpected.entrySet()) {
						final Map<String, Object> context = Collections.singletonMap(EOLQueryEngine.PROPERTY_ARGUMENTS,
								(Object) Collections.singletonMap("nodeLabel", entry.getKey()));

						Object result = eol("return Tree.all.selectOne(t|t.label=nodeLabel).revRefNav_descendants.size;", context);
						assertEquals(
							String.format("%s should have %d ancestors", entry.getKey(), entry.getValue()),
							entry.getValue(), result);
					}
				}

				// Reverse reference navigation for the edges returns the element nodes and not just the derived value nodes
				List<String> expected = Arrays.asList("t3", "t4");
				Collection<String> result = (Collection<String>) eol("return Tree.all.selectOne(t|t.label='t6').revRefNav_descendants.label.flatten;");
				assertEquals(expected.size(), result.size());
				for (String e: expected) {
					assertTrue(result.contains(e));
				}

				return null;
			}
		});
	}

	@Test
	public void derivedEdgeSingle() throws Throwable {
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree-dedges"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				indexer.addDerivedAttribute("Tree", "Tree", "maxDescendant", "dummy", false, true, false,
						EOLQueryEngine.TYPE, "return self.closure(c|c.children).flatten.sortBy(t|t.label).last;");

				// Forward derived edge
				{
					// TODO: isMany is not honored on queries - need to add first
					Object result = eol("return Tree.all.selectOne(t|t.label='t3').maxDescendant.first.label;");
					assertEquals("t6", result);
				}

				// Reverse derived edges
				{
					final Map<String, Integer> reverseExpected = new HashMap<>();
					reverseExpected.put("t3", 0);
					reverseExpected.put("t4", 0);
					reverseExpected.put("t5", 0);
					reverseExpected.put("t6", 2); // t6 is the descendant of t3 and t4 with the last label in dictionary order

					for (Entry<String, Integer> entry : reverseExpected.entrySet()) {
						final Map<String, Object> context = Collections.singletonMap(EOLQueryEngine.PROPERTY_ARGUMENTS,
								(Object) Collections.singletonMap("nodeLabel", entry.getKey()));

						Object result = eol("return Tree.all.selectOne(t|t.label=nodeLabel).revRefNav_maxDescendant.size;", context);
						assertEquals(
							String.format("%s should have %d maxDescendant reverse refs", entry.getKey(), entry.getValue()),
							entry.getValue(), result);
					}
				}

				return null;
			}
		});
	}

	@Test
	public void deriveThenAdd() throws Throwable {
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/crossrefs.ecore"));
		indexer.addDerivedAttribute("http://github.com/mondo-hawk/testing/xrefs", "Element", "nRefs", "dummy", false, true, false,
				EOLQueryEngine.TYPE, "return self.xrefs.size;");
		requestFolderIndex(new File("resources/models/scopedQuery"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(1, eol("return Element.all.selectOne(e|e.id=0).nRefs;"));
				assertEquals(3, eol("return Element.all.selectOne(e|e.id=1).nRefs;"));
				assertEquals(3, eol("return Element.all.selectOne(e|e.id=23).nRefs;"));
				return null;
			}
		});
		
	}
	
	// TODO: indexed lookup tests
}
