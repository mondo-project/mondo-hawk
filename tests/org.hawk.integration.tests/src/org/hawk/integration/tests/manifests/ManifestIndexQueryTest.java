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
package org.hawk.integration.tests.manifests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the indexing of Eclipe manifests.
 */
public class ManifestIndexQueryTest extends ModelIndexingTest {

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public ManifestIndexQueryTest(IGraphDatabaseFactory dbf) {
		super(dbf, new ManifestModelSupportFactory());
	}

	@Test
	public void indexDuplicateDependencies() throws Throwable {
		requestFolderIndex(new File("resources/models/dupdep"));
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				Object result = eol("return ManifestRequires.all.bundle.size;");
				assertEquals(2, (int) result);
				return null;
			}
		});
	}

}
