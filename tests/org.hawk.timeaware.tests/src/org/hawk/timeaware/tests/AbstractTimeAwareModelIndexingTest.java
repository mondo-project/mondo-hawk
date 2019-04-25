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

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.IModelIndexer;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.integration.tests.ModelIndexingTest;
import org.hawk.svn.SvnManager;
import org.hawk.svn.tests.rules.TemporarySVNRepository;
import org.hawk.timeaware.graph.TimeAwareIndexer;
import org.hawk.timeaware.graph.TimeAwareModelUpdater;
import org.hawk.timeaware.queries.TimeAwareEOLQueryEngine;
import org.hawk.timeaware.queries.TimelineEOLQueryEngine;
import org.hawk.timeaware.tests.tree.Tree.TreeFactory;
import org.junit.Before;

/**
 * Base class for all time-aware model indexing tests.
 */
public abstract class AbstractTimeAwareModelIndexingTest extends ModelIndexingTest {

	public static final String TREE_MM_PATH = "resources/metamodels/Tree.ecore";

	protected final TreeFactory treeFactory = TreeFactory.eINSTANCE;

	protected ResourceSet rsTree;
	protected TimeAwareEOLQueryEngine timeAwareQueryEngine;
	protected TimelineEOLQueryEngine timelineQueryEngine;

	public AbstractTimeAwareModelIndexingTest(IGraphDatabaseFactory dbFactory, IModelSupportFactory msFactory) {
		super(dbFactory, msFactory);
	}

	@Before
	public void setUp() throws Exception {
		indexer.registerMetamodels(new File("../org.hawk.integration.tests/resources/metamodels/Ecore.ecore"));
		indexer.registerMetamodels(new File("../org.hawk.integration.tests/resources/metamodels/XMLType.ecore"));
		setUpMetamodels();
	
		rsTree = new ResourceSetImpl();
		rsTree.getResourceFactoryRegistry()
			.getExtensionToFactoryMap()
			.put("*", new XMIResourceFactoryImpl());
	
		timeAwareQueryEngine = new TimeAwareEOLQueryEngine();
		indexer.addQueryEngine(timeAwareQueryEngine);
	
		timelineQueryEngine = new TimelineEOLQueryEngine();
		indexer.addQueryEngine(timelineQueryEngine);
	}

	/**
	 * Sets up additional metamodels, beyond the base Ecore and XMLType ones.
	 */
	protected abstract void setUpMetamodels() throws Exception;

	@Override
	protected GraphModelUpdater createModelUpdater() {
		return new TimeAwareModelUpdater();
	}

	@Override
	protected IModelIndexer createIndexer(File indexerFolder, FileBasedCredentialsStore credStore) {
		return new TimeAwareIndexer("test", indexerFolder, credStore, console);
	}

	protected void requestSVNIndex(final TemporarySVNRepository svnRepository) throws Exception {
		final SvnManager vcs = new SvnManager();
		vcs.init(svnRepository.getRepositoryURL().toString(), indexer);
		vcs.run();
		indexer.addVCSManager(vcs, true);
	}

	protected Object timeAwareEOL(final String eolQuery) throws InvalidQueryException, QueryExecutionException {
		return timeAwareEOL(eolQuery, null);
	}

	protected Object timeAwareEOLInRepository(final String eolQuery, TemporarySVNRepository svnRepository) throws InvalidQueryException, QueryExecutionException {
		return timeAwareEOL(eolQuery,
				Collections.singletonMap(
					EOLQueryEngine.PROPERTY_REPOSITORYCONTEXT,
					svnRepository.getRepositoryURL().toString()));
	}

	protected Object timeAwareEOL(final String eolQuery, Map<String, Object> context) throws InvalidQueryException, QueryExecutionException {
		return timeAwareQueryEngine.query(indexer, eolQuery, context);
	}

	protected Object timelineEOL(final String eolQuery) throws InvalidQueryException, QueryExecutionException {
		return timelineQueryEngine.query(indexer, eolQuery, null);
	}

}