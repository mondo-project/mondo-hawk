/*******************************************************************************
 * Copyright (c) 2018 The University of York, Aston University.
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
package org.hawk.integration.tests.modelio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.updater.GraphModelBatchInjector;
import org.hawk.graph.updater.GraphModelInserter;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.graph.updater.proxies.ProxyReferenceList;
import org.hawk.graph.updater.proxies.ProxyReferenceList.ProxyReference;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

public class ModelioProxyResolutionTest extends ModelIndexingTest {

	private static final String PACKAGE_FRAGMENT = "ea878bd2-7ef9-4ce1-a11e-35fa129981bb";
	private static final String ANIMAL_FRAGMENT = "4ed7f59f-f723-4f88-b6fc-ea6b83eb3108";
	private static final String STRING_FRAGMENT = "00000004-0000-000d-0000-000000000000";

	private static final Path ZOO_MODEL = Paths
		.get("resources", "models", "zoo", "data", "fragments", "Zoo", "model");

	private static final Map<String, String> CLASS_TO_FRAGMENT = new HashMap<>();
	static {
		CLASS_TO_FRAGMENT.put("Animal", ANIMAL_FRAGMENT);
		CLASS_TO_FRAGMENT.put("Area", "0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf");
		CLASS_TO_FRAGMENT.put("Elephant", "2d7b2cba-e694-4b33-bd9e-4d2f1db4cc7b");
		CLASS_TO_FRAGMENT.put("Lion", "c312e899-9f08-43db-8954-4db87789f843");
		CLASS_TO_FRAGMENT.put("Zebra", "9b9791e8-8e77-4fd0-ada7-e62bdfad9ec4");
		CLASS_TO_FRAGMENT.put("Zoo", "4b6abbc6-130e-42fa-b1db-bbf1ba6d0065");
	}

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	private GraphModelUpdater updater;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public ModelioProxyResolutionTest(IGraphDatabaseFactory dbf) {
		super(dbf, new ModelioModelSupportFactory());
	}

	@Override
	public void setup() throws Throwable {
		super.setup();
		indexer.registerMetamodels(new File(ModelioModelSupportFactory.METAMODEL_PATH, "metamodel_descriptor.xml"));
		updater = (GraphModelUpdater) indexer.getModelUpdaters().get(0);
	}

	@Test
	public void elephantResolve() throws Throwable {
		copyClasses("Elephant");
		requestFolderIndex(tempFolder.getRoot());
		waitForSync(() -> {
			// The file should have exactly three unique IDs that others can refer to
			try (IGraphTransaction tx = db.beginTransaction()) {
				final IGraphNodeIndex fragmentIndex = db
					.getOrCreateNodeIndex(GraphModelBatchInjector.FRAGMENT_DICT_NAME);
				assertEquals(3, fragmentIndex.query("id", "*").size());

				/*
				 * We should have two lists with unresolved refs: one to the supertype, and one
				 * to the package.
				 */
				final GraphModelInserter inserter = updater.createInserter();
				List<ProxyReferenceList> lists = inserter.getProxyReferenceLists(indexer.getGraph());
				assertEquals(2, lists.size());

				final Set<String> unresolved = collectUnresolvedFragments(lists);
				assertEquals(new HashSet<>(Arrays.asList(PACKAGE_FRAGMENT, ANIMAL_FRAGMENT)), unresolved);
				tx.success();
			}

			return null;
		});

		copyClasses("Animal", "Area");
		indexer.requestImmediateSync();
		waitForSync(() -> {
			// We should only be missing the package and the string datatype
			List<ProxyReferenceList> lists = updater.createInserter().getProxyReferenceLists(indexer.getGraph());

			// Three lists come from Animal, Area, Elephant, which are missing the package.
			// Two lists come from Animal.name and Elephant.name, which are missing datatype. 
			assertEquals(5, lists.size());
 
			final Set<String> unresolved = collectUnresolvedFragments(lists);
			assertEquals(new HashSet<>(Arrays.asList(
				STRING_FRAGMENT, PACKAGE_FRAGMENT
			)), unresolved);

			return null;
		});
	}

	@Test
	public void zoo() throws Throwable {
		requestFolderIndex(new File("resources/models/zoo"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				// We should only be missing the string datatype
				List<ProxyReferenceList> lists = updater.createInserter().getProxyReferenceLists(indexer.getGraph());
				final Set<String> unresolved = collectUnresolvedFragments(lists);
				assertEquals(Collections.emptySet(), unresolved);
				return null;
			}
		});
	}

	private Set<String> collectUnresolvedFragments(List<ProxyReferenceList> lists) {
		final Set<String> unresolved = new HashSet<>();
		for (ProxyReferenceList list : lists) {
			assertTrue(list.getTargetFile().isFragmentBased());
			for (ProxyReference ref : list.getReferences()) {
				assertTrue(ref.getTarget().isFragmentBased());
				unresolved.add(ref.getTarget().getFragment());
			}
		}
		return unresolved;
	}

	/**
	 * To be used internally for debugging test cases.
	 */
	@SuppressWarnings("unused")
	private void printProxyRefs(List<ProxyReferenceList> lists) {
		for (ProxyReferenceList list : lists) {
			System.out.println("----");
			System.out.println("Source: " + list.getSourceNodeID());
			System.out.println("Target file: " + list.getTargetFile());
			for (ProxyReference ref : list.getReferences()) {
				System.out.println("* " + ref);
			}
		}
	}

	private void copyClasses(String... classes) throws IOException {
		for (String klass : classes) { 
			FileUtils.copyFileToDirectory(getClass(klass), tempFolder.getRoot());
		}
	}

	private File getClass(String klass) {
		return getFragmentFile("Class", CLASS_TO_FRAGMENT.get(klass));
	}

	private File getFragmentFile(String type, String fragment) {
		if (fragment == null) {
			throw new NoSuchElementException();
		}
		return ZOO_MODEL.resolve(Paths.get("Standard." + type, fragment + ".exml")).toFile();
	}

	
}
