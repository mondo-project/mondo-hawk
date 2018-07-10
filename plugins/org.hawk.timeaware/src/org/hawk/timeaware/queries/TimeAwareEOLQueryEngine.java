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
import org.hawk.epsilon.emc.wrappers.TypeNodeWrapper;
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

	public static class TypeHistoryOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof EolModelElementType;
		}

		/**
		 * Provides the <code>.latest</code> property, which returns the instances of
		 * the type in the latest revision, from most recent to oldest.
		 */
		public List<GraphNodeWrapper> getlatest() throws Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());

			final List<GraphNodeWrapper> ret = new ArrayList<>();
			for (IGraphNode node : typeNodes) {
				ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;
				final long latestInstant = taNode.getLatestInstant();
				taNode = taNode.travelInTime(latestInstant);
				for (ModelElementNode instanceNode : new TypeNode(taNode).getAll()) {
					ret.add(wrap(model, instanceNode));
				}
			}

			return ret;
		}

		/**
		 * Provides the <code>.anytime</code> property, which returns the instances of the type across any revisions.
		 */
		public List<GraphNodeWrapper> getanytime() throws Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());

			List<GraphNodeWrapper> ret = new ArrayList<>();
			for (IGraphNode node : typeNodes) {
				ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;
				for (ITimeAwareGraphNode typeNodeVersion : taNode.getAllVersions()) {
					TypeNode typeNode = new TypeNode(typeNodeVersion);
					for (ModelElementNode instanceNode : typeNode.getAll()) {
						ret.add(wrap(model, instanceNode));
					}
				}
			}

			return ret;
		}

		/**
		 * Provides the <code>.versions</code> property, which returns the versions of the type node.
		 */
		public List<TypeNodeWrapper> getversions() throws Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());

			List<TypeNodeWrapper> ret = new ArrayList<>();
			for (IGraphNode node : typeNodes) {
				ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;
				for (ITimeAwareGraphNode typeNodeVersion : taNode.getAllVersions()) {
					TypeNode typeNode = new TypeNode(typeNodeVersion);
					ret.add(new TypeNodeWrapper(typeNode, model));
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
