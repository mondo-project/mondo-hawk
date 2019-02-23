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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.contextful.CEOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for UML indexing. These *must* be run as JUnit plug-in tests, as we
 * rely on the URI mappings registered for the predefined libraries by the UML
 * plugins.
 */
public class UMLIndexingTest extends AbstractUMLIndexingTest {

	private static final String SIMPLE_PROFILE_NSURI_PREFIX = "http://github.com/mondo-project/mondo-hawk/simpleProfile";

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	public UMLIndexingTest(IGraphDatabaseFactory dbf) {
		super(dbf);
	}

	@Test
	public void zoo() throws Throwable {
		requestFolderIndex(new File(BASE_DIRECTORY, "zoo"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(1, eol("return Model.types.select(t|t.name='Profile').size;"));
				assertEquals(4, eol("return Class.all.size;",
					Collections.singletonMap(CEOLQueryEngine.PROPERTY_FILECONTEXT, "*model.uml")));

				try (IGraphTransaction tx = db.beginTransaction()) {
					GraphNodeWrapper attr = (GraphNodeWrapper) eol(
							"return Class.all.selectOne(c|c.name='Animal').ownedAttribute.selectOne(a|a.name='age');");

					// Check cross-reference to UML predefined library
					final IGraphNode node = attr.getNode();
					final Iterable<IGraphEdge> itOutgoing = node.getOutgoingWithType("type");
					int size = 0;
					for (@SuppressWarnings("unused") IGraphEdge e : itOutgoing) {
						size++;
					}
					assertEquals(1, size);

					tx.success();
				}

				return null;
			}
		});
	}

	@Test
	public void stereotypeAsModel() throws Throwable {
		requestFolderIndex(new File(BASE_DIRECTORY, "simpleProfile"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(1, eol("return Stereotype.all.select(s|s.name='special').size;"));
				return null;
			}
		});
	}

	@Test
	public void indexLibraries() throws Throwable {
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(4, eol("return Package.all.size;"));
			assertEquals(4, eol("return ModelLibrary.all.size;"));

			/*
			 * A stereotype application is serialized in XMI like this:
			 * 
			 * <standard:ModelLibrary xmi:id="_jVC18MMDEeCj2YHTjQqCqw" base_Package="_0"/>
			 * 
			 * This means we can find stereotype applications by using
			 * Stereotype.all just fine, and then we can use base_X to see the
			 * instance of the metaclass that was extended.
			 */
			assertEquals(4, eol("return ModelLibrary.all.base_Package.flatten.size;"));

			// Try stereotypes from the Ecore profile (the libraries use a few of those)
			assertEquals(5, eol("return EDataType.all.size;",
					Collections.singletonMap(EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
							"http://www.eclipse.org/uml2/schemas/Ecore/5")));

			tx.success();
		}
	}

	@Test
	public void customProfileV4() throws Throwable {
		indexer.registerMetamodels(new File(BASE_DIRECTORY, "simpleProfile/model.profile.uml"));
		requestFolderIndex(new File(BASE_DIRECTORY, "simpleProfileApplication"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// Check that we support Papyrus profile versioning
				Map<String, Object> ctx = Collections.singletonMap(
					EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
					SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.4");

				assertEquals(1, eol("return special.all.size;", ctx));
				assertEquals(9001, eol("return special.all.first.amount;", ctx));
				assertEquals("Example", eol("return special.all.first.base_Class.name;", ctx));

				// profileApplication is mapped as an ofType edge
				assertEquals(1, eol("return RootElementApplication.all.size;", ctx));
				assertEquals("Example", eol(
					"return RootElementApplication.all.packagedElement.flatten.first.name;", ctx));

				return null;
			}
		});
	}

	@Test
	public void customProfileV5() throws Throwable {
		indexer.registerMetamodels(new File(BASE_DIRECTORY, "simpleProfile/model.profile.uml"));
		requestFolderIndex(new File(BASE_DIRECTORY, "simpleProfileApplicationNewVersion"));
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// Check that we support Papyrus profile versioning
				Map<String, Object> ctx = Collections.singletonMap(
					EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
					SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.5");

				assertEquals(1, eol("return special.all.size;", ctx));
				assertEquals(9002, eol("return special.all.first.amount;", ctx));
				assertEquals("example", eol("return special.all.first.name;", ctx));
				assertEquals("Example", eol("return special.all.first.base_Class.name;", ctx));
				return null;
			}
		});
	}

	@Test
	public void upgradeModelProfileVersion() throws Throwable {
		indexer.registerMetamodels(new File(BASE_DIRECTORY, "simpleProfile/model.profile.uml"));

		final URI destURI = URI.createFileURI(new File(modelFolder.getRoot(), "model.uml").getCanonicalFile().toString());
		final File oldFile = new File(BASE_DIRECTORY, "simpleProfileApplication/model.uml").getCanonicalFile();
		copyResource(destURI, oldFile);
		requestFolderIndex(modelFolder.getRoot());

		final Map<String, Object> ctxOldVersion = Collections.singletonMap(
				EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
				SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.4");
		final Map<String, Object> ctxNewVersion = Collections.singletonMap(
				EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
				SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.5");
		waitForSync(() -> {
			assertEquals(1, eol("return special.all.size;", ctxOldVersion));
			assertEquals(0, eol("return special.all.size;", ctxNewVersion));
			return null;
		});

		final File newFile = new File(BASE_DIRECTORY, "simpleProfileApplicationNewVersion/model.uml").getCanonicalFile();
		copyResource(destURI, newFile);
		indexer.requestImmediateSync();
		waitForSync(() -> {
			assertEquals(0, eol("return special.all.size;", ctxOldVersion));
			assertEquals(1, eol("return special.all.size;", ctxNewVersion));
			return null;
		});
	}

	@Test
	public void modelProfileInsideRepository() throws Throwable {
		indexer.registerMetamodels(new File(BASE_DIRECTORY, "simpleProfile/model.profile.uml"));

		FileUtils.copyDirectory(BASE_DIRECTORY, modelFolder.getRoot());
		FileUtils.deleteDirectory(new File(modelFolder.getRoot(), "simpleProfileApplicationNewVersion"));
		requestFolderIndex(modelFolder.getRoot());

		final Map<String, Object> ctxOldVersion = Collections.singletonMap(
				EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
				SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.4");
		final Map<String, Object> ctxNewVersion = Collections.singletonMap(
				EOLQueryEngine.PROPERTY_DEFAULTNAMESPACES,
				SIMPLE_PROFILE_NSURI_PREFIX + "/0.0.5");
		waitForSync(() -> {
			assertEquals(1, eol("return special.all.size;", ctxOldVersion));
			assertEquals(0, eol("return special.all.size;", ctxNewVersion));
			return null;
		});

		FileUtils.copyFile(new File(BASE_DIRECTORY, "simpleProfileApplicationNewVersion/model.uml"), new File(modelFolder.getRoot(), "simpleProfileApplication/model.uml"));
		indexer.requestImmediateSync();
		waitForSync(() -> {
			assertEquals(0, eol("return special.all.size;", ctxOldVersion));
			assertEquals(1, eol("return special.all.size;", ctxNewVersion));
			return null;
		});
	}

	@Test
	public void localfolderCrosslinks() throws Throwable {
		requestFolderIndex(new File(BASE_DIRECTORY, "crossfile-refs"));
		waitForSync(() -> {
			Map<String, Object> ctx = Collections.singletonMap(CEOLQueryEngine.PROPERTY_FILECONTEXT, "*model.uml");
			assertEquals(3, eol("return Class.all.size;", ctx));
			assertEquals(new HashSet<>(Arrays.asList("Class1", "Class3")),
				eol("return Class.all.selectOne(c|c.name='Class2').generalization.general.name.flatten.asSet;", ctx));
			return false;
		});
	}
}
