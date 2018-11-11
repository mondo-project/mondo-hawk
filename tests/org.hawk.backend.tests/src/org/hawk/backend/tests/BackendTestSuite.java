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
package org.hawk.backend.tests;

import java.util.Collections;

import org.hawk.backend.tests.factories.GreycatDatabaseFactory;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.backend.tests.factories.LevelDBGreycatDatabaseFactory;
import org.hawk.backend.tests.factories.Neo4JDatabaseFactory;
import org.hawk.backend.tests.factories.OrientDatabaseFactory;
import org.hawk.core.graph.IGraphDatabase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite.SuiteClasses;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterContext;
import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;

@RunWith(ParameterizedSuite.class)
@SuiteClasses({
	IndexTest.class,
	DatabaseManagementTest.class,
	GraphPopulationTest.class,
})
public class BackendTestSuite {

	@Parameters(name="{0}")
	public static Object[] params() {
		return new Object[][] {
			{new Neo4JDatabaseFactory()},
			{new OrientDatabaseFactory()},
			//{new RemoteOrientDatabaseFactory()}, // TODO enable automatically when server available
			{new GreycatDatabaseFactory()},
			{new LevelDBGreycatDatabaseFactory()},
		};
	}

	/**
	 * Predefined method for all the various {@link Parameterized}
	 * GraphQueryTest subclasses that only require a {@link IGraphDatabase} on
	 * their constructor.
	 */
	public static Iterable<Object[]> caseParams() {
	    if (ParameterContext.isParameterSet()) {
	        return Collections.singletonList(ParameterContext.getParameter(Object[].class));
	    } else {
			/*
			 * Change uncommented line depending on which backend you want to test outside
			 * this suite, so you may simply right-click on the specific JUnit test class
			 * and run it directly.
			 */
	    	return Collections.singletonList(new Object[]{
	    		//new Neo4JDatabaseFactory()
	    		new OrientDatabaseFactory()
	    		//new GreycatDatabaseFactory()
	    	});
	    }
	}

	@Parameter(0)
	public IGraphDatabaseFactory dbFactory;
}
