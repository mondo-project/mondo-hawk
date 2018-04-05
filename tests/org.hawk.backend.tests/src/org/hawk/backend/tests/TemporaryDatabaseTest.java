/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.backend.tests;

import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.util.DefaultConsole;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Ignore
@RunWith(Parameterized.class)
public class TemporaryDatabaseTest {

	protected final IGraphDatabaseFactory dbFactory;
	protected IGraphDatabase db;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	public TemporaryDatabaseTest(IGraphDatabaseFactory dbf) {
		this.dbFactory = dbf;
	}

	@Before
	public void setup() throws Exception {
		db = dbFactory.create();
		db.run(folder.getRoot(), new DefaultConsole());
	}

	@After
	public void teardown() throws Exception {
		db.delete();
	}

}