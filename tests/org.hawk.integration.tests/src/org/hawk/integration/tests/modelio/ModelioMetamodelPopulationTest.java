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
package org.hawk.integration.tests.modelio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.TypeNode;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.modelio.exml.listeners.ModelioGraphChangeListener;
import org.hawk.modelio.exml.metamodel.ModelioPackage;
import org.hawk.modelio.exml.metamodel.register.MetamodelRegister;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ModelioMetamodelPopulationTest extends ModelIndexingTest {

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> validationListener = new GraphChangeListenerRule<>(new SyncValidationListener());

	@Rule
	public GraphChangeListenerRule<ModelioGraphChangeListener> modelioListener = new GraphChangeListenerRule<>(new ModelioGraphChangeListener());

	private final String METAMODEL_PATH = "resources/metamodels/";

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public ModelioMetamodelPopulationTest(IGraphDatabaseFactory dbf) {
		super(dbf, new ModelioModelSupportFactory());
	}

	@Override
	public void setup() throws Throwable {
		super.setup();
		indexer.registerMetamodels(new File(METAMODEL_PATH, "metamodel_descriptor.xml"));
	}

	@Test
	public void metamodel() throws Exception {

		int nTypes = 0;
		final Collection<ModelioPackage> pkgs = MetamodelRegister.INSTANCE.getRegisteredPackages();
		try (IGraphTransaction tx = db.beginTransaction()) {
			nTypes = visitPackages(nTypes, pkgs);
			tx.success();
		}

		// From 'grep -c MClass MMetamodel.java' on modelio-metamodel-lib
		assertEquals(409, nTypes);
	}

	protected int visitPackages(int nTypes, final Collection<ModelioPackage> pkgs) {
		final GraphWrapper gw = new GraphWrapper(db);

		for (ModelioPackage mpkg : pkgs) {
			MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(mpkg.getNsURI());

			final Set<String> types = new HashSet<>();
			for (TypeNode typeNode : mmNode.getTypes()) {
				types.add(typeNode.getTypeName());
				++nTypes;
			}

			for (IHawkClassifier mc : mpkg.getClasses()) {
				assertTrue(types.contains(mc.getName()));
			}
		}

		return nTypes;
	}

	@Test
	public void zoo() throws Throwable {
		requestFolderIndex(new File("resources/models/zoo"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, validationListener.getListener().getTotalErrors());
				assertEquals(6, eol("return Class.all.size;"));
				return null;
			}
		});
	}
}
