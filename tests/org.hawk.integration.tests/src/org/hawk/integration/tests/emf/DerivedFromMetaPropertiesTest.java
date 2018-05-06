/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.integration.tests.mm.Tree.Tree;
import org.hawk.integration.tests.mm.Tree.TreeFactory;
import org.hawk.integration.tests.mm.Tree.TreePackage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

/**
 * Checks that derived features based on meta-level properties (e.g. eContainer
 * or eContainers) are recomputed when models change.
 */
public class DerivedFromMetaPropertiesTest extends ModelIndexingTest {

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	private ResourceSetImpl rs;
	private Tree tRoot;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public DerivedFromMetaPropertiesTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	@Before
	public void setUp() throws Throwable {
		// Create a tree of 4 nodes, each in its own file
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		rs.getPackageRegistry().put(XMLTypePackage.eNS_URI, XMLTypePackage.eINSTANCE);
		rs.getPackageRegistry().put(TreePackage.eNS_URI, TreePackage.eINSTANCE);

		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodels(new File("resources/metamodels/XMLType.ecore"));
		indexer.registerMetamodels(new File("resources/metamodels/Tree.ecore"));

		tRoot = tn("root", tn("childA", tn("childAA")), tn("childB"));
		for (TreeIterator<EObject> itTree = tRoot.eAllContents(); itTree.hasNext(); ) {
			save((Tree) itTree.next());
		}
		save(tRoot);
		for (Resource r : rs.getResources()) {
			r.save(null);
		}

		requestFolderIndex(modelFolder.getRoot());

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				return null;
			}
		});
	}

	protected void save(Tree tree) {
		Resource r = rs.createResource(URI.createFileURI(new File(
			modelFolder.getRoot(), tree.getLabel() + ".xmi").getAbsolutePath()));
		r.getContents().add(tree);
	}

	@Test
	public void eContentsIsUpdated() throws Throwable {
		indexer.addDerivedAttribute(TreePackage.eNS_URI, "Tree", "allContainers",
				"Tree", true, false, true,
				EOLQueryEngine.TYPE, "return self.closure(e|e.eContainers);");

		assertEquals(2, eol("return Tree.all.selectOne(t|t.label='childAA').allContainers.size;"));
		assertEquals(3, eol("return Tree.all.selectOne(t|t.label='root').revRefNav_allContainers.size;"));

		tRoot.setLabel("changed");
		tRoot.eResource().save(null);
		indexer.requestImmediateSync();

		waitForSync(() -> {
			assertEquals(new HashSet<>(Arrays.asList("changed", "childA")),
				eol("return Tree.all.selectOne(t|t.label='childAA').allContainers.collect(c|c.label).asSet;"));
			assertEquals(new HashSet<>(Arrays.asList("childA", "childAA", "childB")),
				eol("return Tree.all.selectOne(t|t.label='changed').revRefNav_allContainers.collect(c|c.label).asSet;"));
			return null;
		});
	}

	protected Tree tn(final String label, Tree... children) {
		Tree tNode = TreeFactory.eINSTANCE.createTree();
		tNode.setLabel(label);
		for (Tree child : children) {
			tNode.getChildren().add(child);
		}
		return tNode;
	}

}
