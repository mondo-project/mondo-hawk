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

import java.util.Collections;

import org.hawk.backend.tests.factories.GreycatDatabaseFactory;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
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
	    	return Collections.singletonList(new Object[]{
	    		// Change uncommented line depending on which backend you want to test outside this suite
	    		//new Neo4JDatabaseFactory()
	    		//new OrientDatabaseFactory()
	    		new GreycatDatabaseFactory()
	    	});
	    }
	}

	@Parameter(0)
	public IGraphDatabaseFactory dbFactory;
}
