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
package org.hawk.timeaware.queries.operations.scopes;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeIndex;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variant of <code>.when</code> which uses a predefined derived Boolean attribute.
 */
public class WhenAnnotatedOperation extends AbstractOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhenAnnotatedOperation.class);

	private final Supplier<EOLQueryEngine> modelSupplier;

	public WhenAnnotatedOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		this.modelSupplier = containerModelSupplier;
	}

	@Override
	public GraphNodeWrapper execute(Object target, NameExpression operationNameExpression, List<Parameter> iterators, List<Expression> expressions, IEolContext context) throws EolRuntimeException {
		if (expressions.isEmpty()) {
			LOGGER.warn("expected to receive the name of the derived attribute, returning null");
			return null;
		}

		ITimeAwareGraphNode taNode;
		if (target instanceof GraphNodeWrapper) {
			final GraphNodeWrapper gnw = (GraphNodeWrapper)target;
			if (gnw.getNode() instanceof ITimeAwareGraphNode) {
				taNode = (ITimeAwareGraphNode) gnw.getNode();
			} else {
				LOGGER.warn("called on non-timeaware node {}, returning null", target.getClass().getName());
				return null;
			}
		} else {
			LOGGER.warn("called on non-node {}, returning null", target.getClass().getName());
			return null;
		}
			
		final Expression labelExpression = expressions.get(0);
		final String derivedAttrName = "" + context.getExecutorFactory().execute(labelExpression, context);
		final WhenNodeWrapper whenWrapper = whenAnnotated(taNode, derivedAttrName);
		if (whenWrapper == null) {
			return null;
		} else {
			return new GraphNodeWrapper(whenWrapper, modelSupplier.get());
		}
	}

	private WhenNodeWrapper whenAnnotated(ITimeAwareGraphNode taNode, String derivedAttrName) {
		final Slot slot = new ModelElementNode(taNode).getTypeNode().getSlot(derivedAttrName);
		if (slot == null) {
			LOGGER.warn("slot does not exist, returning null");
			return null;
		}

		final String idxName = slot.getNodeIndexName();
		final ITimeAwareGraphNodeIndex index = (ITimeAwareGraphNodeIndex) taNode.getGraph().getOrCreateNodeIndex(idxName);

		List<Long> versions = index.getVersions(taNode, derivedAttrName, true);
		return new WhenNodeWrapper(taNode, versions);
	}

	@Override
	public boolean isOverridable() {
		return false;
	}
}
