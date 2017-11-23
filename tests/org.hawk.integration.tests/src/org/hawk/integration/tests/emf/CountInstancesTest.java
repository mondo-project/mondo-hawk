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
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Indexes models and counts the instances of a certain type.
 */
public class CountInstancesTest extends ModelIndexingTest {

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public CountInstancesTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	@Test
	public void tree() throws Throwable {
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				assertEquals(2, eol("return Tree.all.size;"));
				return null;
			}
		});
	}

	@Test
	public void treeCrossResourceContainment() throws Throwable {
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree-xres"));

		waitForSync(new Callable<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());

				// Test for bug #56: select(t:Type|xyz) does not work
				assertEquals(3, eol("return Model.allContents.select(t:Tree|true).size;"));

				final Collection<String> labels = (Collection<String>) eol("return Model.allContents.collect(t:Tree|t.label);");
				assertEquals(3, labels.size());
				for (String e : Arrays.asList("xyz", "root", "abc")) {
					assertTrue(labels.contains(e));
				}

				assertEquals(3, eol("return Tree.all.size;"));
				assertEquals(2, eol("return Tree.all.selectOne(t|t.label='root').children.size;"));
				assertEquals("root", eol("return Tree.all.selectOne(t|t.label='xyz').eContainer.label;"));
				assertEquals("root", eol("return Tree.all.selectOne(t|t.label='abc').eContainer.label;"));
				return null;
			}
		});
	}

	@Test
	public void set0() throws Throwable {
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/JDTAST.ecore"));

		requestFolderIndex(new File("resources/models/set0"));
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				assertEquals(1, eol("return IJavaProject.all.size;"));

				final int reportedSize = (Integer) eol("return TypeDeclaration.all.size;");
				final Collection<?> actualList = (Collection<?>) eol("return TypeDeclaration.all;");
				assertEquals(reportedSize, actualList.size());

				return null;
			}
		});
	}
}
