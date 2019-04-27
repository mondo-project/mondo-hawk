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
package org.hawk.timeaware.queries.operations.scopes.predicates;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.hawk.timeaware.queries.operations.scopes.WhenNodeWrapper;

/**
 * <p>
 * Generic EOL declarative operation which will return a modified version of the
 * target node at the earliest timepoint since the current timepoint that
 * matches a certain predicate. The modified version of the target node will
 * only report the versions of the target node since the current timepoint that
 * match that predicate.
 * </p>
 */
public class WhenOperation extends TimeAwareNodeFirstOrderOperation {

	public WhenOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		super(containerModelSupplier);
	}

	@Override
	protected Object execute(Variable iterator, Expression expression, IEolContext context,
			Function<ITimeAwareGraphNode, Object> versionWrapper, ITimeAwareGraphNode taNode)
			throws EolInternalException {
 
		try {
			final long latestInstant = taNode.getLatestInstant();
			final List<ITimeAwareGraphNode> versions = taNode.getVersionsBetween(taNode.getTime(), latestInstant);
			final FrameStack scope = context.getFrameStack();

			final List<Long> matchingVersions = new ArrayList<>();
			
			for (Iterator<ITimeAwareGraphNode> itVersion = versions.iterator(); itVersion.hasNext(); ) {
				final ITimeAwareGraphNode version = itVersion.next();
				final Object listItem = versionWrapper.apply(version);

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
				final Long oldestMatchingVersion = matchingVersions.get(matchingVersions.size() - 1);
				final ITimeAwareGraphNode oldestMatchingNode = taNode.travelInTime(oldestMatchingVersion);
				final WhenNodeWrapper scopedNode = new WhenNodeWrapper(oldestMatchingNode, matchingVersions);
				return versionWrapper.apply(scopedNode);
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
