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

import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
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
 * Base class for first-order operations that take a time-aware node (whether a
 * model element or a type) as a target element.
 */
public abstract class TimeAwareNodeFirstOrderOperation extends FirstOrderOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareNodeFirstOrderOperation.class);

	protected final Supplier<EOLQueryEngine> containerModelSupplier;

	public TimeAwareNodeFirstOrderOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		this.containerModelSupplier = containerModelSupplier;
	}

	protected abstract Object execute(Variable iterator, Expression expression, IEolContext context,
			Function<ITimeAwareGraphNode, Object> versionWrapper, ITimeAwareGraphNode taNode)
			throws EolInternalException;

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
		
		return execute(iterator, expression, context, varWrapper, taNode);
	}

}