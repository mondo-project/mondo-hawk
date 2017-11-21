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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.GraphNodeWrapper;
import org.hawk.graph.internal.updater.GraphModelBatchInjector;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.uml.vcs.PredefinedUMLLibraries;
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
		addUMLComponents();
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

					// We shouldn't have any lingering proxies
					IGraphIterable<IGraphNode> proxies = db.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME).query("*", "*");
					for (IGraphNode n : proxies) {
						System.out.println(n);
					}
					assertThat(proxies.size(), equalTo(0));

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
			assertThat(eol("return ModelLibrary.all.size;"), equalTo(4));

			/*
			 * A stereotype application is serialized in XMI like this:
			 * 
			 * <standard:ModelLibrary xmi:id="_jVC18MMDEeCj2YHTjQqCqw" base_Package="_0"/>
			 * 
			 * This means we can find stereotype applications by using
			 * Stereotype.all just fine, and then we can use base_X to see the
			 * instance of the metaclass that was extended.
			 */
			assertThat(eol("return ModelLibrary.all.base_Package.flatten.size;"), equalTo(4));

			// Try stereotypes from the Ecore profile (the libraries use a few of those)
			assertThat(eol("return EDataType.all.size;",
					Collections.singletonMap(EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
							"http://www.eclipse.org/uml2/schemas/Ecore/5")),
					equalTo(5));

			/*
			 * Find all applications of a profile: profiles are equivalent to
			 * EPackages, and Hawk does not allow you to link from a model
			 * element node to a metamodel node, only to a type node. For that
			 * reason, profileApplication is mapped to a string, which we can
			 * then use as part of meta-level queries to read the profile that
			 * was applied.
			 */
			assertThat(eol("return Package.all.select(s|s.profileApplication.contains('http://www.omg.org/spec/UML/20131001/StandardProfile')).size;"), equalTo(4));

			tx.success();
		}
	}

	protected void addUMLComponents() throws Throwable {
		final PredefinedUMLLibraries vcs = new PredefinedUMLLibraries();
		vcs.init(null, indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
		waitForSync();

		/*// results in even more unresolved proxies (to Ecore and the UML metamodels)
		final PredefinedUMLProfiles vcsProfiles = new PredefinedUMLProfiles();
		vcsProfiles.init(null, indexer);
		vcs.run();
		indexer.addVCSManager(vcsProfiles, true);
		waitForSync();
		*/
	}
}
