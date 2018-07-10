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
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.pgetters.GraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.epsilon.emc.wrappers.TypeNodeWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variant of {@link EOLQueryEngine} that exposes the time-aware nature of the
 * index through EOL. Both types and model elements have the same set of
 * time-aware properties and references: <code>.time</code>,
 * <code>.earliest</code>, <code>.latest</code>, <code>.versions</code>.
 *
 * TODO: integrate a nicer "human time to seconds since epoch UTC" library.
 *
 * TODO: allow for revision-based times as well.
 */
public class TimeAwareEOLQueryEngine extends EOLQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareEOLQueryEngine.class);

	public interface RiskyFunction<T, U> {
		U call(T t) throws Exception;
	}

	protected class TimeAwareGraphPropertyGetter extends GraphPropertyGetter {
		protected TimeAwareGraphPropertyGetter(IGraphDatabase graph, EOLQueryEngine m) {
			super(graph, m);
		}

		@Override
		protected Object invokePredefined(String property, IGraphNode node) throws EolRuntimeException {
			final Object ret = super.invokePredefined(property, node);
			if (ret != null) {
				return ret;
			}

			final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;
			try {
				switch (property) {
				case "versions":
					return getVersions(taNode);
				case "latest":
					return new GraphNodeWrapper(taNode.getLatest(), TimeAwareEOLQueryEngine.this);
				case "earliest":
					return new GraphNodeWrapper(taNode.getEarliest(), TimeAwareEOLQueryEngine.this);
				case "previous":
				case "prev":
					final ITimeAwareGraphNode taPrevious = taNode.getPrevious();
					if (taPrevious == null) {
						return null;
					} else {
						return new GraphNodeWrapper(taPrevious, TimeAwareEOLQueryEngine.this);
					}
				case "next":
					final ITimeAwareGraphNode taNext = taNode.getNext();
					if (taNext == null) {
						return null;
					} else {
						return new GraphNodeWrapper(taNext, TimeAwareEOLQueryEngine.this);
					}
				case "time":
					return taNode.getTime();
				}
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage(), ex);
				throw new EolRuntimeException(ex.getMessage());
			}

			return null;
		}

		private List<GraphNodeWrapper> getVersions(ITimeAwareGraphNode taNode) throws Exception {
			final List<GraphNodeWrapper> result = new ArrayList<>();
			for (ITimeAwareGraphNode version : taNode.getAllVersions()) {
				result.add(new GraphNodeWrapper(version, TimeAwareEOLQueryEngine.this));
			}
			return result;
		}
	}

	public static class TypeHistoryOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof EolModelElementType;
		}

		/**
		 * Provides the <code>.latest</code> property, which returns the latest revision
		 * of the type node associated to this type.
		 */
		public TypeNodeWrapper getlatest() throws Exception {
			return getTypeNodeVersionWrapper((taNode) -> taNode.getLatest());
		}

		/**
		 * Provides the <code>.earliest</code> property, which returns the latest
		 * revision of the type node associated to this type.
		 */
		public TypeNodeWrapper getearliest() throws Exception {
			return getTypeNodeVersionWrapper((taNode) -> taNode.getEarliest());
		}

		protected TypeNodeWrapper getTypeNodeVersionWrapper(
				final RiskyFunction<ITimeAwareGraphNode, ITimeAwareGraphNode> fn)
				throws EolRuntimeException, Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());

			checkTypeIsPrecise(eolType, typeNodes);
			ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) typeNodes.get(0);
			return new TypeNodeWrapper(new TypeNode(fn.call(taNode)), model);
		}


		protected void checkTypeIsPrecise(final EolModelElementType eolType, final List<IGraphNode> typeNodes)
				throws EolRuntimeException {
			if (typeNodes.size() > 1) {
				throw new EolRuntimeException("Type " + eolType + " is ambiguous");
			} else if (typeNodes.isEmpty()) {
				throw new EolRuntimeException("Type " + eolType + " could not be found");
			}
		}

		/**
		 * Provides the <code>.versions</code> property, which returns the versions of
		 * the type node.
		 */
		public List<TypeNodeWrapper> getversions() throws Exception {
			final EolModelElementType eolType = (EolModelElementType) target;
			final EOLQueryEngine model = (EOLQueryEngine) eolType.getModel();
			final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
			final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());
			checkTypeIsPrecise(eolType, typeNodes);

			List<TypeNodeWrapper> ret = new ArrayList<>();
			ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) typeNodes.get(0);
			for (ITimeAwareGraphNode typeNodeVersion : taNode.getAllVersions()) {
				TypeNode typeNode = new TypeNode(typeNodeVersion);
				ret.add(new TypeNodeWrapper(typeNode, model));
			}

			return ret;
		}

		protected GraphNodeWrapper wrap(final EOLQueryEngine model, ModelElementNode instanceNode) {
			final IGraphNode meNode = instanceNode.getNode();
			final GraphNodeWrapper gnw = new GraphNodeWrapper(meNode, model);
			return gnw;
		}
	}

	@Override
	protected GraphPropertyGetter createContextlessPropertyGetter() {
		return new TimeAwareGraphPropertyGetter(graph, this);
	}

	@Override
	protected IEolModule createModule() {
		final IEolModule module = super.createModule();
		module.getContext().getOperationContributorRegistry().add(new TypeHistoryOperationContributor());
		return module;
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
