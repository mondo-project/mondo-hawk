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
package org.hawk.timeaware.queries.operations.patterns;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.execute.context.FrameStack;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.timeaware.queries.operations.TimeAwareNodeFirstOrderOperation;

/**
 * First-order operation that returns a Boolean value from evaluating
 * a predicate on the versions of a node. The specific quantifier depends
 * on the {@link IShortCircuitReducer} used.
 */
public class VersionQuantifierOperation extends TimeAwareNodeFirstOrderOperation {

	private final IShortCircuitReducer reducer;

	public VersionQuantifierOperation(Supplier<EOLQueryEngine> containerModelSupplier, IShortCircuitReducer reducer) {
		super(containerModelSupplier);
		this.reducer = reducer;
	}

	protected Object execute(Variable iterator, Expression expression, IEolContext context,
			Function<ITimeAwareGraphNode, Object> versionWrapper, ITimeAwareGraphNode taNode) throws EolInternalException {
		try {
			final List<ITimeAwareGraphNode> versions = taNode.getAllVersions();
			final FrameStack scope = context.getFrameStack();

			for (ITimeAwareGraphNode version : versions) {
				Object listItem = versionWrapper.apply(version);

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
