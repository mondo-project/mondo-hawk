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
package org.hawk.timeaware.tests.annotators;

import static org.hawk.timeaware.tests.annotators.TestUtils.assertHasNodes;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndexFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.integration.tests.mm.Tree.TreePackage;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.graph.TimeAwareMetaModelUpdater;
import org.hawk.timeaware.graph.annotators.VersionAnnotatorSpec;
import org.hawk.timeaware.tests.AbstractTimeAwareModelIndexingTest;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for annotation of past history in the {@link TimeAwareMetaModelUpdater}.
 */
@RunWith(Parameterized.class)
public class PastHistoryAnnotationTest extends AbstractTimeAwareModelIndexingTest {

	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	private ITimeAwareGraphNodeVersionIndexFactory nviFactory;

	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		Object[][] baseParams = BackendTestSuite.timeAwareBackends();

		Object[][] params = new Object[baseParams.length][];
		for (int i = 0; i < baseParams.length; i++) {
			params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
		}

		return Arrays.asList(params);
	}

	public PastHistoryAnnotationTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	public void setup() throws Throwable {
		super.setup();

		assumeTrue(db instanceof ITimeAwareGraphNodeVersionIndexFactory);
		this.nviFactory = (ITimeAwareGraphNodeVersionIndexFactory) db;
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void firstVersionOutOfTwo() throws Exception {
		final File fTree = new File(svnRepository.getCheckoutDirectory(), "root.xmi");
		Resource rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree t = treeFactory.createTree();
		t.setLabel("NowYouSeeMe");
		rTree.getContents().add(t);
		rTree.save(null);

		svnRepository.add(fTree);
		svnRepository.commit("First commit");

		t.setLabel("NowYouDoNot");
		rTree.save(null);
		svnRepository.commit("Second commit - should not be visible in version index");
		requestSVNIndex(svnRepository);
		
		final VersionAnnotatorSpec annotator = new VersionAnnotatorSpec.Builder()
			.metamodelURI(TreePackage.eNS_URI)
			.typeName("Tree")
			.label("Important")
			.language(new EOLQueryEngine().getType())
			.expression("return self.label = 'NowYouSeeMe';")
			.build();

		assertFalse(nviFactory.versionIndexExists(annotator.getVersionLabel()));
		tammUpdater.addVersionAnnotator(indexer, annotator);
		assertTrue(nviFactory.versionIndexExists(annotator.getVersionLabel()));

		GraphNodeWrapper gnw = (GraphNodeWrapper) timeAwareEOL("return Tree.latest.all.first;");
		ITimeAwareGraphNode node = (ITimeAwareGraphNode) gnw.getNode();
		ITimeAwareGraphNodeVersionIndex idx = nviFactory.getOrCreateVersionIndex(annotator.getVersionLabel());
		assertHasNodes(idx.getAllVersions(node), node);
	}
	
}
