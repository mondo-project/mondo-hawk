/*******************************************************************************
 * Copyright (c) 2015-2019 The University of York, Aston University.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.backend.tests.factories.LevelDBGreycatDatabaseFactory;
import org.hawk.backend.tests.factories.Neo4JDatabaseFactory;
import org.hawk.backend.tests.factories.OrientDatabaseFactory;
import org.hawk.backend.tests.factories.RocksDBGreycatDatabaseFactory;
import org.hawk.core.graph.IGraphDatabase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite.SuiteClasses;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterContext;
import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;

/**
 * General test suite for basic requirements for all Hawk backends. This also
 * serves as a starting point for any other test case which should be run for
 * every backend available.
 *
 * To speed up Tycho builds, it is possible to use the {@link #USE_ONLY_ENV}
 * environment variable to limit testing to a single backend. This makes it
 * possible to test all backends in separate VMs at once, reducing the risk of
 * hitting the time limit. If set to the simple class name of a specific
 * {@link IGraphDatabaseFactory} (e.g. "Neo4JDatabaseFactory"), only that
 * factory will be used for the tests.
 *
 * It is also possible to skip *all* backend tests by setting the
 * {@link #SKIP_BACKEND_ENV} environment variable (the value does not matter).
 * This should make it possible to run only the backend tests first, and
 * then run all non-backend tests afterwards together with the rest of the
 * normal build.
 */
@RunWith(ParameterizedSuite.class)
@SuiteClasses({
	IndexTest.class,
	DatabaseManagementTest.class,
	GraphPopulationTest.class,
})
public class BackendTestSuite {

	private static final boolean EMPTY_IS_OKAY = true;

	public static final String USE_ONLY_ENV = "HAWK_BACKEND_TESTS_ONLY";
	public static final String SKIP_BACKEND_ENV = "HAWK_BACKEND_TESTS_SKIP";

	@Parameters(name="{0}")
	public static Object[][] params() {
		return allBackends();
	}

	/**
	 * Predefined method for all the various {@link Parameterized}
	 * GraphQueryTest subclasses that only require a {@link IGraphDatabase} on
	 * their constructor.
	 */
	public static Iterable<Object[]> caseParams() {
	    if (ParameterContext.isParameterSet()) {
               // This reuses in a particular test case the parameter set by the top test suite
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

	public static Object[][] allBackends() {
		final List<IGraphDatabaseFactory> factories = new ArrayList<>();
		factories.add(new Neo4JDatabaseFactory());
		factories.add(new OrientDatabaseFactory());
		// factories.add(new RemoteOrientDatabaseFactory()); // TODO enable when server is available
		factories.add(new RocksDBGreycatDatabaseFactory());
		factories.add(new LevelDBGreycatDatabaseFactory());
		return filterFactories(factories, !EMPTY_IS_OKAY);
	}

	public static Object[][] timeAwareBackends() {
		final List<IGraphDatabaseFactory> factories = new ArrayList<>();
		factories.add(new RocksDBGreycatDatabaseFactory());
		factories.add(new LevelDBGreycatDatabaseFactory());
		return filterFactories(factories, EMPTY_IS_OKAY);
	}

	private static Object[][] filterFactories(List<IGraphDatabaseFactory> factories, boolean emptyIsOK) {
		final String skipBackends = System.getenv(SKIP_BACKEND_ENV);
		if (skipBackends != null) {
			return new Object[0][];
		}

		final String useOnlyThisFactory = System.getenv(USE_ONLY_ENV);
		if (useOnlyThisFactory != null) {
			for (Iterator<IGraphDatabaseFactory> it = factories.iterator(); it.hasNext(); ) {
				IGraphDatabaseFactory f = it.next();
				if (!f.getClass().getSimpleName().equals(useOnlyThisFactory)) {
					it.remove();
				}
			}
			if (factories.size() > 1) {
				throw new IllegalArgumentException(USE_ONLY_ENV + " was set, but more than one factory matched it");
			}
			if (factories.isEmpty() && emptyIsOK != EMPTY_IS_OKAY) {
				throw new IllegalArgumentException(USE_ONLY_ENV + " was set, but no factories matched it");
			}
		}

		final Object[][] results = new Object[factories.size()][];
		for (int i = 0; i < factories.size(); i++) {
			IGraphDatabaseFactory factory = factories.get(i);
			results[i] = new Object[] { factory };
		}

		return results;
	}

	@Parameter(0)
	public IGraphDatabaseFactory dbFactory;
}
