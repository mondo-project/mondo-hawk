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
 * First-order operation that returns a Boolean value. Returns true
 * if the predicate is true for all versions in scope.
 */
public class AlwaysOperation extends FirstOrderOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlwaysOperation.class);
	private final EOLQueryEngine containerModel;

	public AlwaysOperation(EOLQueryEngine containerModel) {
		this.containerModel = containerModel;
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
			final List<ITimeAwareGraphNode> versions = taNode.getAllVersions();
			final FrameStack scope = context.getFrameStack();

			for (ITimeAwareGraphNode version : versions) {
				GraphNodeWrapper listItem = new GraphNodeWrapper(version, containerModel);
				
				if (iterator.getType()==null || iterator.getType().isKind(listItem)){
					scope.enterLocal(FrameType.UNPROTECTED, expression);

					scope.put(Variable.createReadOnlyVariable(iterator.getName(), listItem));
					Object bodyResult = context.getExecutorFactory().execute(expression, context);
					scope.leaveLocal(expression);

					if (bodyResult instanceof Boolean && !((boolean) bodyResult)){
						return false;
					}
				}
			}
		} catch (Exception ex) {
			throw new EolInternalException(ex, expression);
		}
		
		return true;
	}

	@Override
	public boolean isOverridable() {
		return false;
	}

}
