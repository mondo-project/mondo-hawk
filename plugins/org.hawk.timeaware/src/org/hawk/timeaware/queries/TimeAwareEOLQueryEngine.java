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
package org.hawk.timeaware.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;

/**
 * Variant of {@link EOLQueryEngine} that exposes the time-aware nature of the
 * index through EOL.
 *
 * TODO: integrate a nicer "human time to seconds since epoch UTC" library.
 *
 * TODO: allow for revision-based times as well.
 */
public class TimeAwareEOLQueryEngine extends EOLQueryEngine {

	public static class TimeAwareNodeOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof GraphNodeWrapper && ((GraphNodeWrapper)target).getNode() instanceof ITimeAwareGraphNode;
		}

		public boolean isAlive(String moment) {
			final ITimeAwareGraphNode taNode = getTimeAwareNode();

			long time;
			if ("now".equals(moment)) {
				time = System.currentTimeMillis() / 1_000;
			} else {
				throw new UnsupportedOperationException("Cannot understand '" + moment + "' yet");
			}

			return taNode.travelInTime(time) != null;
		}

		public List<GraphNodeWrapper> getVersions() throws Exception {
			ITimeAwareGraphNode taNode = getTimeAwareNode();

			List<GraphNodeWrapper> versions = new ArrayList<>();
			for (ITimeAwareGraphNode version : taNode.getAllVersions()) {
				versions.add(new GraphNodeWrapper(version, ((GraphNodeWrapper)target).getContainerModel()));
			}
			return versions;
		}

		protected ITimeAwareGraphNode getTimeAwareNode() {
			return (ITimeAwareGraphNode) ((GraphNodeWrapper)target).getNode();
		}
	}

	public static class TypeHistoryOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof EolModelElementType;
		}

		/**
		 * Returns all the instances of a type through history. Each instance will be at its
		 * earliest timepoint.
		 */
		protected void returnInstancesAtAnyTime(final EOLQueryEngine model, Collection<GraphNodeWrapper> ret, ITimeAwareGraphNode taNode) throws Exception {
			// TODO: debug why we have 2 Log objects at all in STORM example.
			for (ITimeAwareGraphNode typeNodeVersion : taNode.getAllVersions()) {
				TypeNode typeNode = new TypeNode(typeNodeVersion);
				for (ModelElementNode instanceNode : typeNode.getAll()) {
					ret.add(wrap(model, instanceNode));
				}
			}
		}

		/**
		 * Returns all the available instance of a type within a certain range. Currently limited to:
		 * <ul>
		 * <li>anytime: any timepoint of the type. Instances will be at their earliest timepoint.</li>
		 * <li>latest: latest timepoint of the type. Instances will be at their earliest timepoints.</li>
		 * </ul>
		 */
		public List<GraphNodeWrapper> allIn(String range) throws Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());

			List<GraphNodeWrapper> ret = new ArrayList<>();
			for (IGraphNode node : typeNodes) {
				ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;

				if ("anytime".equals(range)) {
					returnInstancesAtAnyTime(model, ret, taNode);
				} else if ("latest".equals(range)) {
					final long latestInstant = taNode.getLatestInstant();
					taNode = taNode.travelInTime(latestInstant);
					for (ModelElementNode instanceNode : new TypeNode(taNode).getAll()) {
						ret.add(wrap(model, instanceNode));
					}
				} else {
					throw new UnsupportedOperationException("Cannot understand '" + range + "' yet");
				}
			}

			return ret;
		}

		protected GraphNodeWrapper wrap(final EOLQueryEngine model, ModelElementNode instanceNode) {
			final IGraphNode meNode = instanceNode.getNode();
			final GraphNodeWrapper gnw = new GraphNodeWrapper(meNode, model);
			return gnw;
		}
	}

	public TimeAwareEOLQueryEngine() {
		// nothing to do - only for breakpoints
	}

	@Override
	protected IEolModule createModule() {
		final IEolModule module = super.createModule();

		module.getContext().getOperationContributorRegistry()
			.add(new TypeHistoryOperationContributor());
		module.getContext().getOperationContributorRegistry()
			.add(new TimeAwareNodeOperationContributor());

		return module;
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException
	{
		final HawkState currentState = m.getCompositeStateListener().getCurrentState();
		if (currentState != HawkState.RUNNING) {
			throw new QueryExecutionException(
					String.format("Cannot run the query, as the indexer is not in the RUNNING state: it is %s instead.",
							currentState));
		}

		final long trueStart = System.currentTimeMillis();
		String defaultnamespaces = null;
		if (context != null) {
			defaultnamespaces = (String) context.get(PROPERTY_DEFAULTNAMESPACES);
		}

		final EOLQueryEngine q = new TimeAwareEOLQueryEngine();
		try {
			q.load(m);
			q.setDefaultNamespaces(defaultnamespaces);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}

		final IEolModule module = createModule();
		parseQuery(query, context, q, module);
		return runQuery(trueStart, module);
	}
	
}
