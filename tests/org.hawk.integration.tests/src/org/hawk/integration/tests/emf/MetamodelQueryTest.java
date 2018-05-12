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
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for querying the metamodel with Hawk.
 */
public class MetamodelQueryTest extends ModelIndexingTest {

	private static final String JDTAST_PT = "org.amma.dsl.jdt.primitiveTypes";
	private static final String JDTAST_DOM = "org.amma.dsl.jdt.dom";
	private static final String JDTAST_CORE = "org.amma.dsl.jdt.core";
	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public MetamodelQueryTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	@Before
	public void setUp() throws Throwable {
		indexer.registerMetamodels(
			new File("resources/metamodels/Ecore.ecore"),
			new File("resources/metamodels/JDTAST.ecore"));
	}

	@Test
	public void metamodels() throws Exception {
		assertEquals("org.hawk.emf.metamodel.EMFMetaModelResourceFactory", eolmm(JDTAST_CORE, ".metamodelType"));
		assertNotNull(eolmm(JDTAST_CORE, ".resource"));
		assertEquals(new HashSet<>(Arrays.asList(JDTAST_DOM, JDTAST_PT)),
				eolmm(JDTAST_CORE, ".dependencies.collect(dep|dep.uri).asSet"));

		assertEquals(0, eolmm(JDTAST_PT, ".types.size"));
		assertEquals(21, eolmm(JDTAST_CORE, ".types.size"));
	}

	private Object eolmm(String uri, String suffix) throws InvalidQueryException, QueryExecutionException {
		return eol(String.format("return Model.metamodels.selectOne(mm|mm.uri='%s')%s;", uri, suffix));
	}
}
