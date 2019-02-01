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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.pgetters.GraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;

/**
 * Variant of {@link EOLQueryEngine} that exposes the time-aware nature of the
 * index through EOL. Both types and model elements have the same set of
 * time-aware properties and references: <code>.time</code>,
 * <code>.earliest</code>, <code>.latest</code>, <code>.versions</code>.
 *
 * TODO: integrate a "human time to seconds since epoch UTC" library.
 *
 * TODO: allow for revision-based times as well.
 */
public class TimeAwareEOLQueryEngine extends EOLQueryEngine {

	@Override
	protected GraphPropertyGetter createContextlessPropertyGetter() {
		return new TimeAwareGraphPropertyGetter(graph, this);
	}

	/**
	 * Extends "allInstances" with the concept of time. It is likely to be used
	 * through the {@link #allInstancesNow()} convenience function.
	 * 
	 * The regular "allInstances" can still be used in timeline queries, where
	 * the global timepoint is changed.
	 */
	public Collection<?> allInstancesAt(long timepoint) {
		final Set<Object> allContents = new HashSet<Object>();
		final ITimeAwareGraphDatabase taGraph = (ITimeAwareGraphDatabase) graph;
		for (IGraphNode node : taGraph.allNodes(ModelElementNode.OBJECT_VERTEX_LABEL, timepoint)) {
			GraphNodeWrapper wrapper = new GraphNodeWrapper(node, this);
			allContents.add(wrapper);
		}
		return allContents;
	}

	/**
	 * Returns all the instances in the model at this current moment, by visiting
	 * the type nodes at the current point in time.
	 */
	public Collection<?> allInstancesNow() {
		return allInstancesAt(System.currentTimeMillis());
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException {
		final HawkState currentState = m.getCompositeStateListener().getCurrentState();
		if (currentState != HawkState.RUNNING) {
			throw new QueryExecutionException(
					String.format("Cannot run the query, as the indexer is not in the RUNNING state: it is %s instead.",
							currentState));
		}
		if (!(m.getGraph() instanceof ITimeAwareGraphDatabase)) {
			throw new QueryExecutionException(getClass().getName() + " can only be used with time-aware backends");
		}

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
		module.getContext().getOperationContributorRegistry().add(new TimeAwareNodeHistoryOperationContributor(q));
		module.getContext().getOperationContributorRegistry().add(new TypeHistoryOperationContributor(q));
		parseQuery(query, context, q, module);
		return runQuery(module);
	}

	@Override
	public String getHumanReadableName() {
		return "Time-aware EOL query engine";
	}
}
