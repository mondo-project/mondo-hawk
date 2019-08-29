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
package org.hawk.timeaware.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.GraphWrapper;
import org.hawk.integration.tests.emf.EMFModelSupportFactory;
import org.hawk.service.api.ModelElement;
import org.hawk.service.servlet.utils.HawkModelElementEncoder;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.tests.tree.Tree.Tree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the Thrift encoding of time-aware query results.
 */
@RunWith(Parameterized.class)
public class ThriftEncodingTest extends AbstractTimeAwareModelIndexingTest {
	@Rule
	public TemporarySVNRepository svnRepository = new TemporarySVNRepository();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	Object[][] baseParams = BackendTestSuite.timeAwareBackends();

    	Object[][] params = new Object[baseParams.length][];
    	for (int i = 0; i < baseParams.length; i++) {
    		params[i] = new Object[] { baseParams[i][0], new EMFModelSupportFactory() };
    	}

    	return Arrays.asList(params);
    }

	public ThriftEncodingTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory modelSupportFactory) {
		super(dbFactory, modelSupportFactory);
	}

	@Override
	protected void setUpMetamodels() throws Exception {
		indexer.registerMetamodels(new File(TREE_MM_PATH));
	}

	@Test
	public void encodeLatestVersion() throws Throwable {
		keepAddingChildren();
		waitForSync(() -> {
			Collection<GraphNodeWrapper> ret = (Collection<GraphNodeWrapper>) timeAwareEOL("return Model.allInstancesNow;");
			HawkModelElementEncoder enc = new HawkModelElementEncoder(new GraphWrapper(indexer.getGraph()));
			enc.setUseContainment(false);

			List<ModelElement> encoded = new ArrayList<>(ret.size());
			for (GraphNodeWrapper gnw : ret) {
				encoded.add(enc.encode(gnw.getNode()));
			}

			assertEquals(4, encoded.size());
			return null;
		});
	}

	@Test
	public void encodeLatestVersionRoot() throws Throwable {
		keepAddingChildren();
		waitForSync(() -> {
			GraphNodeWrapper ret = (GraphNodeWrapper) timeAwareEOL("return Model.allInstancesNow.selectOne(t|t.eContainer.isUndefined());");
			HawkModelElementEncoder enc = new HawkModelElementEncoder(new GraphWrapper(indexer.getGraph()));
			ModelElement encoded = enc.encode(ret.getNode());

			assertNotNull(encoded);
			assertEquals("Root", encoded.getAttributes().get(0).value.getVString());
			assertEquals(3, encoded.getContainers().get(0).elements.size());
			return null;
		});
	}

	private Tree keepAddingChildren() throws Exception {
		final File fTree = new File(svnRepository.getCheckoutDirectory(), "m.xmi");
		Resource rTree = rsTree.createResource(URI.createFileURI(fTree.getAbsolutePath()));

		Tree tRoot = treeFactory.createTree();
		tRoot.setLabel("Root");
		rTree.getContents().add(tRoot);
		rTree.save(null);
		svnRepository.add(fTree);
		svnRepository.commit("Create root");

		for (String childLabel : Arrays.asList("T1", "T2", "T3")) {
			Tree t1 = treeFactory.createTree();
			t1.setLabel(childLabel);
			tRoot.getChildren().add(t1);
			rTree.save(null);
			svnRepository.commit("Add " + childLabel);
		}

		requestSVNIndex();
		return tRoot;
	}

	private void requestSVNIndex() throws Exception {
		requestSVNIndex(svnRepository);
	}

}
