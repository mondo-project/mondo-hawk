/*******************************************************************************
 * Copyright (c) 2017-2018 Aston University.
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
package org.hawk.integration.tests.uml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.junit.Test;

/**
 * Tests for UML indexing within a workspace. These *must* be run as JUnit
 * plug-in tests, as we need to have access to the Workspace repository type.
 */
public class UMLWorkspaceIndexingTest extends AbstractUMLIndexingTest {

	public UMLWorkspaceIndexingTest(IGraphDatabaseFactory dbf) {
		super(dbf);
	}

	@Test
	public void workspaceCrosslinks() throws Throwable {
		final File testResourcesBase = new File(BASE_DIRECTORY, "crossfile-refs");
		IProject project1 = openProject(new File(testResourcesBase, "model1"));
		IProject project2 = openProject(new File(testResourcesBase, "model2"));

		try {
			requestWorkspaceIndexing();
			waitForSync(() -> {
				assertEquals(3, eolWorkspace("return Class.all.size;"));
				assertEquals(new HashSet<>(Arrays.asList("Class1", "Class3")), eolWorkspace(
						"return Class.all.selectOne(c|c.name='Class2').generalization.general.name.flatten.asSet;"));
				return false;
			});
		} finally {
			project1.close(null);
			project2.close(null);
		}
	}
}
