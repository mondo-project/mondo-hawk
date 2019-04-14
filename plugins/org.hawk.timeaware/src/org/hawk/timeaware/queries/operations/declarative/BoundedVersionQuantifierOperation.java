/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolNoType;
import org.eclipse.epsilon.eol.types.EolType;
import org.hawk.epsilon.emc.EOLQueryEngine;

public class BoundedVersionQuantifierOperation extends AbstractOperation {

	private final Function<Integer, IShortCircuitReducer> reducerLambda;
	private final EOLQueryEngine model;

	public BoundedVersionQuantifierOperation(EOLQueryEngine containerModel, Function<Integer, IShortCircuitReducer> reducerLambda) {
		this.model = containerModel;
		this.reducerLambda = reducerLambda;
	}

	@Override
	public Object execute(Object target, NameExpression operationNameExpression, List<Parameter> iterators,
			List<Expression> expressions, IEolContext context) throws EolRuntimeException {

		if (expressions.size() != 2) {
			throw new EolRuntimeException("Bounded version quantifiers require an "
					+ "element predicate lambda expression and a count expression");
		}

		final Parameter iterator = iterators.get(0);
		final EolType iteratorType = iterator.getType(context);
		if (target == EolNoType.Instance && iteratorType instanceof EolModelElementType) {
			target = ((EolModelElementType) iteratorType).getAllOfKind();
		}

		final Object countResult = context.getExecutorFactory().execute(expressions.get(1), context);
		if (!(countResult instanceof Number)) {
			throw new EolRuntimeException("Count expression should return a number");
		}
		final int count = ((Number)countResult).intValue();

		return new VersionQuantifierOperation(model, reducerLambda.apply(count))
			.execute(target, operationNameExpression, iterators, expressions, context);
	}

}
