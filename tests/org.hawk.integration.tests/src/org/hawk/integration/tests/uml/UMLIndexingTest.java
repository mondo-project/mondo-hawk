/*******************************************************************************
 * Copyright (c) 2017 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.uml;

import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.epsilon.emc.GraphNodeWrapper;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.uml.vcs.UMLLibraries;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for UML indexing. These *must* be run as JUnit plug-in tests, as we
 * rely on the URI mappings registered for the predefined libraries by the UML
 * plugins.
 */
public class UMLIndexingTest extends ModelIndexingTest {

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public UMLIndexingTest(IGraphDatabaseFactory dbf) {
		super(dbf, new UMLModelSupportFactory());
	}

	@Override
	public void setup() throws Throwable {
		UMLResourcesUtil.initGlobalRegistries();
		super.setup();
		addUMLLibraries();
	}

	@Test
	public void zoo() throws Throwable {
		requestFolderIndex(new File("resources/models/uml/zoo"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertThat(eol("return Model.types.select(t|t.name='Profile').size;"), equalTo(1));
				assertThat(eol("return Class.all.size;",
					Collections.singletonMap(CEOLQueryEngine.PROPERTY_FILECONTEXT, "*model.uml")), equalTo(4));

				try (IGraphTransaction tx = db.beginTransaction()) {
					GraphNodeWrapper attr = (GraphNodeWrapper) eol(
							"return Class.all.selectOne(c|c.name='Animal').ownedAttribute.selectOne(a|a.name='age');");

					// Check cross-reference to UML predefined library
					final IGraphNode node = attr.getNode();
					assertThat(node.getOutgoingWithType("type"), iterableWithSize(1));
					tx.success();
				}

				return null;
			}
		});
	}

	@Test
	public void stereotypeAsModel() throws Throwable {
		requestFolderIndex(new File("resources/models/uml/simpleProfile"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertThat(eol("return Stereotype.all.select(s|s.name='special').size;"), equalTo(1));
				return null;
			}
		});
	}

	@Test
	public void indexLibraries() throws Throwable {
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertThat(eol("return Package.all.size;"), equalTo(4));
			tx.success();
		}
	}

	protected void addUMLLibraries() throws Throwable {
		final UMLLibraries vcs = new UMLLibraries();
		vcs.init(null, indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// do nothing
				return null;
			}});
	}
}
