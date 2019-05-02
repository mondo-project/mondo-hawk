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
package org.hawk.timeaware.graph.annotators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndexFactory;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.timeaware.graph.TimeAwareMetaModelUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the actual implementations of the version annotation algorithms,
 * unlike {@link VersionAnnotatorSpec} which is only used to describe the
 * annotator for basic CRUD.
 *
 * This class does not handle persistence of {@link VersionAnnotatorSpec}: that
 * is up to the {@link TimeAwareMetaModelUpdater}.
 */
public class VersionAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionAnnotator.class);
	private static final String ANNOTATION_PREFIX = "ann_";
	private static final Map<String, EolModule> CACHED_MODULES = new HashMap<>();

	private final IModelIndexer indexer;
	private final ITimeAwareGraphNodeVersionIndexFactory factory;

	public VersionAnnotator(IModelIndexer indexer) throws EolModelLoadingException {
		this.indexer = indexer;
		this.factory = (ITimeAwareGraphNodeVersionIndexFactory) indexer.getGraph();
	}

	/**
	 * Annotates the full existing history of every instance of the type in the
	 * <code>spec</code>: since we no longer have incremental access to changes, we
	 * will simply go through every of the type at each version of the entire model
	 * and compute away. This can be potentially slow: it would be better to add the
	 * annotators before any models are indexed.
	 *
	 * NOTE: this will not create any new versions in the graph - e.g. if element B
	 * changed between two versions of element A and that intermediate version would
	 * cause the first version of element A to be annotated from then on, we would
	 * simply annotate the second version of A rather than create a new intermediate
	 * version of A and annotate it. This would work differently if the annotator
	 * had been registered from the start, in which case we could use property
	 * access logs to find and annotate this intermediate timepoint.
	 *
	 * TODO: optimize by detecting changes through history. Derived attribute access
	 * logs could be made history-less, perhaps?
	 */
	public void annotateFullHistory(VersionAnnotatorSpec spec) throws Exception {
		final IGraphDatabase graph = indexer.getGraph();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(graph);
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(spec.getMetamodelURI());
			final TypeNode tNode = mmNode.getTypeNode(spec.getTypeName());
			final ITimeAwareGraphNode taTNode = (ITimeAwareGraphNode) tNode.getNode();

			for (ITimeAwareGraphNode typeVersion : taTNode.getAllVersions()) {
				final TypeNode tnVersion = new TypeNode(typeVersion);
				final String propName = ANNOTATION_PREFIX + spec.getVersionLabel();
				LOGGER.info("Annotating version {} of type {}::{}", typeVersion.getTime(), spec.getMetamodelURI(),
						spec.getTypeName());

				instances: for (ModelElementNode instance : tnVersion.getAll()) {
					ITimeAwareGraphNode taInstanceNode = (ITimeAwareGraphNode) instance.getNode();

					for (ITimeAwareGraphNode instanceVersion : taInstanceNode.getAllVersions()) {
						if (instanceVersion.getPropertyKeys().contains(propName)) {
							// This instance has already been visited
							continue instances;
						}

						annotateVersion(spec, instanceVersion);
					}
				}
			}

			tx.success();
		}
	}

	public void annotateVersion(VersionAnnotatorSpec spec, ITimeAwareGraphNode instanceVersion)
			throws InvalidQueryException, QueryExecutionException {
		try {
			LOGGER.info("Annotating version {} of node {} (type {})", instanceVersion.getTime(),
					instanceVersion.getId(), new ModelElementNode(instanceVersion).getTypeNode().getTypeName());

			final EolModule currentModule = new EolModule();
			final EOLQueryEngine model = new EOLQueryEngine();
			model.load(indexer);
			currentModule.getContext().getModelRepository().addModel(model);

			currentModule.parse(spec.getExpression());
			List<ParseProblem> pps = currentModule.getParseProblems();
			for (ParseProblem p : pps) {
				LOGGER.error("Parsing problem: {}", p);
			}
			if (!pps.isEmpty()) {
				return;
			}

			GraphNodeWrapper gnw = new GraphNodeWrapper(instanceVersion, model);
			currentModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("self", gnw));
			final Object result = currentModule.execute();

			if (result instanceof Boolean) {
				final String propName = ANNOTATION_PREFIX + spec.getVersionLabel();
				final ITimeAwareGraphNodeVersionIndex idx = getVersionIndex(spec);

				final boolean bResult = (boolean) result;
				instanceVersion.setProperty(propName, bResult);
				if (bResult) {
					idx.addVersion(instanceVersion);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while annotating with label " + spec.getVersionLabel(), e);
		}
	}

	protected ITimeAwareGraphNodeVersionIndex getVersionIndex(VersionAnnotatorSpec spec) {
		return factory.getOrCreateVersionIndex(
				String.format("%s##%s##%s", spec.getMetamodelURI(), spec.getTypeName(), spec.getVersionLabel()));
	}

}
