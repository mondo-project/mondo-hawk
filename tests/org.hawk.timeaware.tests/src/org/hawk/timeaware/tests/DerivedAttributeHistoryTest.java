/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.hawk.timeaware.tests.tree.Tree.TreePackage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.tmatesoft.svn.core.SVNException;

/**
 * Tests for derived attributes over the history of a node.
 */
@RunWith(Parameterized.class)
public class DerivedAttributeHistoryTest extends AbstractTimeAwareModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();
	private Resource rTree;

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	Object[][] baseParams = BackendTestSuite.timeAwareBackends();

    	Object[][] params = new Object[baseParams.length][];
    	for (int i = 0; i < baseParams.length; i++) {
    		params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
    	}

    	return Arrays.asList(params);
    }

	public DerivedAttributeHistoryTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void computedForAllVersions() throws Throwable {
		oneDerivedAttribute();
		requestSVNIndex(svnRepository);
		
		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// .all works on revision 0
				assertTrue((boolean)
					timeAwareEOL("return Tree.latest.all.first.always(v|v.Important.isDefined());"));
				assertTrue((boolean)
						timeAwareEOL("return Tree.latest.all.first.earliest.Important;"));
				assertFalse((boolean)
						timeAwareEOL("return Tree.latest.all.first.latest.Important;"));

				assertEquals(1,
					timeAwareEOL("return Tree.latest.all.first.whenAnnotated('Important').versions.size;"));

				return null;
			}
		});
	}

	@Test
	public void whenComposability() throws Throwable {
		twoDerivedAttributes();

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				// whenAnnotated
				assertEquals(2,
					timeAwareEOL("return Tree.earliest.next.all.first.whenAnnotated('Important').versions.size;"));
				assertEquals(2,
						timeAwareEOL("return Tree.earliest.next.all.first.whenAnnotated('HasChildren').versions.size;"));
				assertEquals(1,
						timeAwareEOL("return Tree.earliest.next.all.first.whenAnnotated('Important').whenAnnotated('HasChildren').versions.size;"));
				assertEquals(1,
						timeAwareEOL("return Tree.earliest.next.all.first.whenAnnotated('HasChildren').whenAnnotated('Important').versions.size;"));

				return null;
			}
		});
	}

	@Test
	public void sinceAnnotated() throws Throwable {
		twoDerivedAttributes();

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(4, timeAwareEOL("return Tree.earliest.next.all.first.earliest.sinceAnnotated('Important').versions.size;"));
				assertEquals(1, timeAwareEOL("return Tree.earliest.next.all.first.earliest.next.sinceAnnotated('Important').versions.size;"));
				assertEquals(2, timeAwareEOL("return Tree.earliest.next.all.first.earliest.sinceAnnotated('HasChildren').versions.size;"));

				return null;
			}
		});
	}

	@Test
	public void afterAnnotated() throws Throwable {
		twoDerivedAttributes();

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(3, timeAwareEOL("return Tree.earliest.next.all.first.earliest.afterAnnotated('Important').versions.size;"));
				assertFalse((boolean) timeAwareEOL("return Tree.earliest.next.all.first.earliest.next.afterAnnotated('Important').isDefined();"));
				assertEquals(1, timeAwareEOL("return Tree.earliest.next.all.first.earliest.afterAnnotated('HasChildren').versions.size;"));

				return null;
			}
		});
	}

	@Test
	public void untilAnnotated() throws Throwable {
		twoDerivedAttributes();

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertEquals(1, timeAwareEOL("return Tree.earliest.next.all.first.earliest.untilAnnotated('Important').versions.size;"));
				assertEquals(3, timeAwareEOL("return Tree.earliest.next.all.first.earliest.next.sinceThen.untilAnnotated('Important').versions.size;"));
				assertEquals(3, timeAwareEOL("return Tree.earliest.next.all.first.earliest.untilAnnotated('HasChildren').versions.size;"));

				assertEquals(1, timeAwareEOL("return Tree.earliest.next.all.first.earliest.untilAnnotated('HasChildren').untilAnnotated('Important').versions.size;"));
				assertFalse((boolean) timeAwareEOL("return Tree.earliest.next.all.first.earliest.untilAnnotated('Important').untilAnnotated('HasChildren').isDefined();"));

				return null;
			}
		});
	}

	@Test
	public void beforeAnnotated() throws Throwable {
		twoDerivedAttributes();

		waitForSync(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				assertFalse((boolean) timeAwareEOL("return Tree.earliest.next.all.first.earliest.beforeAnnotated('Important').isDefined();"));
				assertEquals(2, timeAwareEOL("return Tree.earliest.next.all.first.earliest.next.sinceThen.beforeAnnotated('Important').versions.size;"));
				assertEquals(2, timeAwareEOL("return Tree.earliest.next.all.first.earliest.beforeAnnotated('HasChildren').versions.size;"));
				return null;
			}
		});
	}
	
	private void twoDerivedAttributes() throws Exception, IOException, SVNException {
		indexer.getMetaModelUpdater().addDerivedAttribute(
			TreePackage.eNS_URI, "Tree", "HasChildren", "Boolean",
			false, false, false,
			EOLQueryEngine.TYPE,
			"return self.children.size > 0;",
			indexer
		);

		Tree tRoot = oneDerivedAttribute();
		Tree tChild = TreeFactory.eINSTANCE.createTree();
		tChild.setLabel("Child");
		tRoot.getChildren().add(tChild);
		rTree.save(null);
		svnRepository.commit("Add child node");

		tRoot.setLabel("NowYouSeeMe");
		rTree.save(null);
		svnRepository.commit("Change label back");

		requestSVNIndex(svnRepository);
	}
	
	private Tree oneDerivedAttribute() throws Exception {
		indexer.getMetaModelUpdater().addDerivedAttribute(
			TreePackage.eNS_URI, "Tree", "Important", "Boolean",
			false, false, false,
			EOLQueryEngine.TYPE,
			"return self.label = 'NowYouSeeMe';",
			indexer
		);

		final File fTree = new File(svnRepository.getCheckoutDirectory(), "root.xmi");
		rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree t = treeFactory.createTree();
		t.setLabel("NowYouSeeMe");
		rTree.getContents().add(t);
		rTree.save(null);
		svnRepository.add(fTree);
		svnRepository.commit("First commit");

		t.setLabel("NowYouDoNot");
		rTree.save(null);
		svnRepository.commit("Second commit - change label");

		return t;
	}

}
