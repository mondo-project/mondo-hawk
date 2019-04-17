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
package org.hawk.timeaware.queries.operations.declarative;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.FrameStack;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.operations.declarative.FirstOrderOperation;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Generic EOL declarative operation which will return a modified version of the
 * target node at the earliest timepoint since the current timepoint that
 * matches a certain predicate. The modified version of the target node will
 * only report the versions of the target node since the current timepoint that
 * match that predicate.
 * </p>
 */
public class WhenOperation extends FirstOrderOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhenOperation.class);

	private final Supplier<EOLQueryEngine> containerModelSupplier;

	public WhenOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		this.containerModelSupplier = containerModelSupplier;
	}

	@Override
	public Object execute(Object target, Variable iterator, Expression expression, IEolContext context) throws EolRuntimeException {
		if (target == null) {
			LOGGER.warn("always called on null value, returning false");
			return false;
		}
		if (!(target instanceof GraphNodeWrapper)) {
			LOGGER.warn("always called on non-node {}, returning false", target.getClass().getName());
			return false;
		}
		final GraphNodeWrapper gnw = (GraphNodeWrapper)target;

		if (!(gnw.getNode() instanceof ITimeAwareGraphNode)) {
			LOGGER.warn("always called on non-timeaware node {}, returning false", target.getClass().getName());
			return false;
		}
		final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) gnw.getNode();
 
		try {
			final long latestInstant = taNode.getLatestInstant();
			final List<ITimeAwareGraphNode> versions = taNode.getVersionsBetween(taNode.getTime(), latestInstant);
			final FrameStack scope = context.getFrameStack();

			final List<Long> matchingVersions = new ArrayList<>();
			
			for (Iterator<ITimeAwareGraphNode> itVersion = versions.iterator(); itVersion.hasNext(); ) {
				final ITimeAwareGraphNode version = itVersion.next();
				final GraphNodeWrapper listItem = new GraphNodeWrapper(version, containerModelSupplier.get());

				if (iterator.getType()==null || iterator.getType().isKind(listItem)){
					scope.enterLocal(FrameType.UNPROTECTED, expression);

					scope.put(Variable.createReadOnlyVariable(iterator.getName(), listItem));
					Object bodyResult = context.getExecutorFactory().execute(expression, context);
					scope.leaveLocal(expression);

					if (bodyResult instanceof Boolean && (boolean)bodyResult) {
						matchingVersions.add(version.getTime());
					}
				}
			}

			if (!matchingVersions.isEmpty()) {
				return new GraphNodeWrapper(
					new WhenNodeWrapper(taNode.travelInTime(matchingVersions.get(matchingVersions.size() - 1)), matchingVersions),
					containerModelSupplier.get());
			}
		} catch (Exception ex) {
			throw new EolInternalException(ex, expression);
		}

		return null;
	}

	@Override
	public boolean isOverridable() {
		return false;
	}

}
