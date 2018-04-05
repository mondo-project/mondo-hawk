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
