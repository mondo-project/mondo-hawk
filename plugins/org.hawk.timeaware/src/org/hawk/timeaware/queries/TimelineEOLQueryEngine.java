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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphDatabase;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.query.IAccessListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variant of the {@link TimeAwareEOLQueryEngine} which runs the same query over
 * the full history of the index, reporting a sequence of timepoint/instant +
 * result pairs.
 *
 * Note that it is still possible to escape the specific timepoint set by the
 * query engine by using the .latest/.earliest properties. This query engine is
 * mostly to allow old (not time-aware) queries to be evaluated over the history
 * of a model.
 *
 * Since this query engine manipulates the shared state of the graph (the
 * current time), we cannot have two timeline queries happen at the same time.
 * Synchronisation is used to enforce this.
 *
 * TODO: right now we can only find points when type nodes changed (instances
 * were created or deleted). We should take advantage of repository nodes
 * instead to compute the various timepoints.
 *
 * TODO: refactor temporal quantifiers / scoping so they are part of the regular
 * time-aware Java API and not just part of the time-aware EOL query engine.
 *
 * TODO: have engine keep its own time, rather than relying on the time set on
 * the backend.
 */
public class TimelineEOLQueryEngine implements IQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimelineEOLQueryEngine.class);

	private String defaultNamespaces;

	@Override
	public IAccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes)
			throws InvalidQueryException, QueryExecutionException {
		throw new QueryExecutionException("Derived attributes are not supported by this engine");
	}

	@Override
	public List<String> validate(String derivationlogic) {
		return new TimeAwareEOLQueryEngine().validate(derivationlogic);
	}

	@Override
	public void setDefaultNamespaces(String defaultNamespaces) {
		this.defaultNamespaces = defaultNamespaces;
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
		if (!(m.getGraph() instanceof ITimeAwareGraphDatabase)) {
			throw new QueryExecutionException(getClass().getName() + " can only be used with time-aware databases");
		}
		final ITimeAwareGraphDatabase taDB = (ITimeAwareGraphDatabase) m.getGraph();

		if (defaultNamespaces != null) {
			if (context == null) {
				context = new HashMap<>();
			}
			if (!context.containsKey(PROPERTY_DEFAULTNAMESPACES)) {
				context.put(PROPERTY_DEFAULTNAMESPACES, defaultNamespaces);
			}
		}

		// Collect all relevant instants from the various type nodes
		final Set<Long> instants = new TreeSet<>();
		try (IGraphTransaction tx = taDB.beginTransaction()) {
			GraphWrapper gW = new GraphWrapper(taDB);
			for (MetamodelNode mm : gW.getMetamodelNodes()) {
				for (TypeNode tn : mm.getTypes()) {
					ITimeAwareGraphNode taTypeNode = (ITimeAwareGraphNode) tn.getNode();
					instants.addAll(taTypeNode.getAllInstants());
				}
			}
			tx.success();
		} catch (Exception e) {
			throw new QueryExecutionException(e);
		}

		// Compute query for each and every instant
		final List<Object> results = new ArrayList<>();
		try {
			int executed = 0;
			for (long instant : instants) {
				if (executed % 10 == 0) {
					LOGGER.info("Ran {}/{} instants", executed, instants.size());
				}
				executed++;

				// Do not allow two timeline queries to take place concurrently
				synchronized (taDB) {
					taDB.setTime(instant);

					final EOLQueryEngine q = new TimeAwareEOLQueryEngine();
					final Object result = q.query(m, query, context);
					results.add(Arrays.asList(instant, result));
				}
			}
		} finally {
			synchronized (taDB) {
				taDB.setTime(0L);
			}
		}

		return results;
	}

	@Override
	public String getHumanReadableName() {
		return "Timeline EOL query engine";
	}

}
