/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.integration.tests.mm.Tree.TreePackage;
import org.hawk.timeaware.graph.TimeAwareMetaModelUpdater;
import org.hawk.timeaware.graph.TimeAwareModelUpdater;
import org.hawk.timeaware.graph.VersionAnnotator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for basic CRUD of version annotators in the
 * {@link TimeAwareModelUpdater}.
 */
@RunWith(Parameterized.class)
public class VersionAnnotatorCRUDTest extends AbstractTimeAwareModelIndexingTest {

	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		Object[][] baseParams = BackendTestSuite.timeAwareBackends();

		Object[][] params = new Object[baseParams.length][];
		for (int i = 0; i < baseParams.length; i++) {
			params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
		}

		return Arrays.asList(params);
	}

	public VersionAnnotatorCRUDTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void createListOne() {
		final VersionAnnotator annotator = new VersionAnnotator.Builder()
			.metamodelURI(TreePackage.eNS_URI)
			.typeName("Tree")
			.label("Important")
			.language(new EOLQueryEngine().getType())
			.expression("return true;")
			.build();

		assertEquals(0, tammUpdater.listVersionAnnotators(indexer).size());
		assertFalse(indexer.getGraph().nodeIndexExists(TimeAwareMetaModelUpdater.VA_TYPES_IDXNAME));
		tammUpdater.addVersionAnnotator(indexer, annotator);
		assertTrue(indexer.getGraph().nodeIndexExists(TimeAwareMetaModelUpdater.VA_TYPES_IDXNAME));

		assertEquals(Collections.singleton(annotator),
				tammUpdater.listVersionAnnotators(indexer));
		assertEquals(Collections.singleton(annotator),
				tammUpdater.listVersionAnnotators(indexer, TreePackage.eNS_URI, "Tree"));

		tammUpdater.removeVersionAnnotator(indexer, TreePackage.eNS_URI, "Tree", annotator.getVersionLabel());
		assertEquals(0, tammUpdater.listVersionAnnotators(indexer).size());
		assertEquals(0, indexer.getGraph()
			.getOrCreateNodeIndex(TimeAwareMetaModelUpdater.VA_TYPES_IDXNAME)
			.query(TimeAwareMetaModelUpdater.VA_TYPES_IDXKEY, annotator.getVersionLabel())
			.size()
		);
	}

	@Test(expected=IllegalArgumentException.class)
	public void cannotOverrideExistingSlot() {
		final VersionAnnotator annotator = new VersionAnnotator.Builder()
				.metamodelURI(TreePackage.eNS_URI)
				.typeName("Tree")
				.label("parent")
				.language(new EOLQueryEngine().getType())
				.expression("return true;")
				.build();

		tammUpdater.addVersionAnnotator(indexer, annotator);
	}

	@Test(expected=IllegalArgumentException.class)
	public void cannotRemoveNonVASlot() {
		tammUpdater.removeVersionAnnotator(indexer, TreePackage.eNS_URI, "Tree", "parent");
	}

	@Test(expected=NoSuchElementException.class)
	public void cannotRemoveNonExistingVA() {
		tammUpdater.removeVersionAnnotator(indexer, TreePackage.eNS_URI, "Tree", "Important");
	}
	
}
