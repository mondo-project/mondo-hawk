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
package org.hawk.integration.tests;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.integration.tests.bpmn.ModelVersioningTest;
import org.hawk.integration.tests.emf.DerivedFeatureTest;
import org.hawk.integration.tests.emf.CountInstancesTest;
import org.hawk.integration.tests.emf.TreeUpdateTest;
import org.hawk.integration.tests.manifests.ManifestIndexQueryTest;
import org.hawk.integration.tests.modelio.ModelioMetamodelPopulationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite.SuiteClasses;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;

@RunWith(ParameterizedSuite.class)
@SuiteClasses({
	ManifestIndexQueryTest.class,
	DerivedFeatureTest.class,
	CountInstancesTest.class,
	ModelioMetamodelPopulationTest.class,
	ModelVersioningTest.class,
	TreeUpdateTest.class,
})
public class IntegrationTestSuite {

	@Parameters(name="Parameters are {0}")
	public static Object[] params() {
		return BackendTestSuite.params();
	}

	@Parameter(0)
	public IGraphDatabaseFactory dbFactory;
}
