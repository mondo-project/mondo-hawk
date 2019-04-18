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
import java.util.function.Function;
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
import org.hawk.epsilon.emc.wrappers.TypeNodeWrapper;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * First-order operation that returns a Boolean value from evaluating
 * a predicate on the versions of a node. The specific quantifier depends
 * on the {@link IShortCircuitReducer} used.
 */
public class VersionQuantifierOperation extends FirstOrderOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionQuantifierOperation.class);

	private final Supplier<EOLQueryEngine> containerModelSupplier;
	private final IShortCircuitReducer reducer;

	public VersionQuantifierOperation(Supplier<EOLQueryEngine> containerModelSupplier, IShortCircuitReducer reducer) {
		this.containerModelSupplier = containerModelSupplier;
		this.reducer = reducer;
	}

	@Override
	public Object execute(Object target, Variable iterator, Expression expression, IEolContext context) throws EolRuntimeException {
		if (target == null) {
			LOGGER.warn("always called on null value, returning false");
			return false;
		}

		Function<ITimeAwareGraphNode, Object> varWrapper;
		ITimeAwareGraphNode taNode;
		if (target instanceof GraphNodeWrapper) {
			final GraphNodeWrapper gnw = (GraphNodeWrapper)target;
			if (gnw.getNode() instanceof ITimeAwareGraphNode) {
				taNode = (ITimeAwareGraphNode) gnw.getNode();
				varWrapper = (n) -> new GraphNodeWrapper(n, containerModelSupplier.get());
			} else {
				LOGGER.warn("always called on non-timeaware node {}, returning false", target.getClass().getName());
				return false;
			}
		} else if (target instanceof TypeNodeWrapper) {
			final TypeNodeWrapper tnw = (TypeNodeWrapper) target;
			if (tnw.getNode() instanceof ITimeAwareGraphNode) {
				taNode = (ITimeAwareGraphNode) tnw.getNode();
				varWrapper = (n) -> new TypeNodeWrapper(new TypeNode(n), containerModelSupplier.get());
			} else {
				LOGGER.warn("always called on non-timeaware node {}, returning false", target.getClass().getName());
				return false;
			}
		} else {
			LOGGER.warn("always called on non-node {}, returning false", target.getClass().getName());
			return false;
		}
		
		try {
			final List<ITimeAwareGraphNode> versions = taNode.getAllVersions();
			final FrameStack scope = context.getFrameStack();

			for (ITimeAwareGraphNode version : versions) {
				Object listItem = varWrapper.apply(version);

				if (iterator.getType()==null || iterator.getType().isKind(listItem)){
					scope.enterLocal(FrameType.UNPROTECTED, expression);

					scope.put(Variable.createReadOnlyVariable(iterator.getName(), listItem));
					Object bodyResult = context.getExecutorFactory().execute(expression, context);
					scope.leaveLocal(expression);

					if (bodyResult instanceof Boolean) {
						Boolean bResult = reducer.reduce((boolean) bodyResult);
						if (bResult != null) {
							return bResult;
						}
					}
				}
			}
		} catch (Exception ex) {
			throw new EolInternalException(ex, expression);
		}

		return reducer.reduce();
	}

	@Override
	public boolean isOverridable() {
		return false;
	}

}
