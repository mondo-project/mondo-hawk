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
package org.hawk.integration.tests;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.integration.tests.bpmn.ModelVersioningTest;
import org.hawk.integration.tests.emf.DerivedFeatureTest;
import org.hawk.integration.tests.emf.DerivedFromMetaPropertiesTest;
import org.hawk.integration.tests.emf.MetamodelQueryTest;
import org.hawk.integration.tests.emf.ScopedQueryTest;
import org.hawk.integration.tests.emf.SubtreeContextTest;
import org.hawk.integration.tests.emf.CountInstancesTest;
import org.hawk.integration.tests.emf.TreeUpdateTest;
import org.hawk.integration.tests.manifests.ManifestIndexQueryTest;
import org.hawk.integration.tests.modelio.ModelioMetamodelPopulationTest;
import org.hawk.integration.tests.modelio.ModelioProxyResolutionTest;
import org.hawk.integration.tests.uml.UMLIndexingTest;
import org.hawk.integration.tests.uml.UMLWorkspaceIndexingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite.SuiteClasses;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;

@RunWith(ParameterizedSuite.class)
@SuiteClasses({
	CountInstancesTest.class,
	DerivedFeatureTest.class,
	DerivedFromMetaPropertiesTest.class,
	ManifestIndexQueryTest.class,
	MetamodelQueryTest.class,
	ModelioProxyResolutionTest.class,
	ModelioMetamodelPopulationTest.class,
	ModelVersioningTest.class,
	ScopedQueryTest.class,
	SubtreeContextTest.class,
	TreeUpdateTest.class,
	UMLIndexingTest.class,
	UMLWorkspaceIndexingTest.class,
})
public class IntegrationTestSuite {

	@Parameters(name="Parameters are {0}")
	public static Object[] params() {
		return BackendTestSuite.params();
	}

	@Parameter(0)
	public IGraphDatabaseFactory dbFactory;
}
