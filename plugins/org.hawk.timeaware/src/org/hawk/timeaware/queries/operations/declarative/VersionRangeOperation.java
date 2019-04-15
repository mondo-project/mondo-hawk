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

import java.util.List;
import java.util.ListIterator;
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
 * Generic EOL declarative operation which tries to find a node matching
 * a predicate from the current timepoint of the target node, and passes
 * the original and matching nodes to a wrapper for decoration. The wrapper
 * will usually limit the range of versions available to the returned node.
 * </p>
 * 
 * <p>
 * If no such timepoint exists, the operation will return an undefined value,
 * which can be checked against with <code>.isDefined()</code>.
 * </p>
 */
public class VersionRangeOperation extends FirstOrderOperation {

	@FunctionalInterface
	public interface IRangeBasedNodeWrapper {
		ITimeAwareGraphNode wrap(ITimeAwareGraphNode original, ITimeAwareGraphNode predicateMatch);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionRangeOperation.class);

	private final Supplier<EOLQueryEngine> containerModelSupplier;
	private final IRangeBasedNodeWrapper nodeWrapper;

	public VersionRangeOperation(Supplier<EOLQueryEngine> containerModelSupplier, IRangeBasedNodeWrapper nodeWrapper) {
		this.containerModelSupplier = containerModelSupplier;
		this.nodeWrapper = nodeWrapper;
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

			for (ListIterator<ITimeAwareGraphNode> itVersion = versions.listIterator(versions.size()); itVersion.hasPrevious(); ) {
				final ITimeAwareGraphNode version = itVersion.previous();
				final GraphNodeWrapper listItem = new GraphNodeWrapper(version, containerModelSupplier.get());

				if (iterator.getType()==null || iterator.getType().isKind(listItem)){
					scope.enterLocal(FrameType.UNPROTECTED, expression);

					scope.put(Variable.createReadOnlyVariable(iterator.getName(), listItem));
					Object bodyResult = context.getExecutorFactory().execute(expression, context);
					scope.leaveLocal(expression);

					if (bodyResult instanceof Boolean && (boolean)bodyResult) {
						return new GraphNodeWrapper(nodeWrapper.wrap(taNode, version), containerModelSupplier.get());
					}
				}
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
