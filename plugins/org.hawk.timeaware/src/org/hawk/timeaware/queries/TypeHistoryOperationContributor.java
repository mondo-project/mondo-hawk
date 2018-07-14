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
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.TypeNodeWrapper;
import org.hawk.graph.TypeNode;

public class TypeHistoryOperationContributor extends OperationContributor {
	private EOLQueryEngine model;

	public TypeHistoryOperationContributor(EOLQueryEngine q) {
		this.model = q;
	}

	@Override
	public boolean contributesTo(Object target) {
		return target instanceof EolModelElementType;
	}

	/**
	 * Provides the <code>.latest</code> property, which returns the latest revision
	 * of the type node associated to this type.
	 */
	public TypeNodeWrapper getlatest() throws Exception {
		return getTypeNodeVersionWrappers(
			(taNode) -> Collections.singletonList(taNode.getLatest())
		).get(0);
	}

	/**
	 * Provides the <code>.earliest</code> property, which returns the latest
	 * revision of the type node associated to this type.
	 */
	public TypeNodeWrapper getearliest() throws Exception {
		return getTypeNodeVersionWrappers(
			(taNode) -> Collections.singletonList(taNode.getEarliest())
		).get(0);
	}

	/**
	 * Provides the <code>.versions</code> property, which returns the versions of
	 * the type node.
	 */
	public List<TypeNodeWrapper> getversions() throws Exception {
		return getTypeNodeVersionWrappers((taNode) -> taNode.getAllVersions());
	}

	public List<TypeNodeWrapper> getVersionsBetween(long fromInclusive, long toInclusive) throws Exception {
		return getTypeNodeVersionWrappers((taNode) -> taNode.getVersionsBetween(fromInclusive, toInclusive));
	}

	public List<TypeNodeWrapper> getVersionsFrom(long fromInclusive) throws Exception {
		return getTypeNodeVersionWrappers((taNode) -> taNode.getVersionsFrom(fromInclusive));
	}

	public List<TypeNodeWrapper> getVersionsUpTo(long toInclusive) throws Exception {
		return getTypeNodeVersionWrappers((taNode) -> taNode.getVersionsUpTo(toInclusive));
	}
	
	private List<TypeNodeWrapper> getTypeNodeVersionWrappers(RiskyFunction<ITimeAwareGraphNode, List<ITimeAwareGraphNode>> lambda)
		throws EolRuntimeException, Exception
	{
		final EolModelElementType eolType = (EolModelElementType) target;
		final TimeAwareEOLQueryEngine queryEngine = (TimeAwareEOLQueryEngine) eolType.getModel();
		final List<IGraphNode> typeNodes = queryEngine.getTypeNodes(eolType.getTypeName());
		checkTypeIsPrecise(eolType, typeNodes);
		
		List<TypeNodeWrapper> ret = new ArrayList<>();
		ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) typeNodes.get(0);
		for (ITimeAwareGraphNode typeNodeVersion : lambda.call(taNode)) {
			TypeNode typeNode = new TypeNode(typeNodeVersion);
			ret.add(new TypeNodeWrapper(typeNode, model));
		}

		return ret;
	}

	private void checkTypeIsPrecise(final EolModelElementType eolType, final List<IGraphNode> typeNodes)
			throws EolRuntimeException {
		if (typeNodes.size() > 1) {
			throw new EolRuntimeException("Type " + eolType + " is ambiguous");
		} else if (typeNodes.isEmpty()) {
			throw new EolRuntimeException("Type " + eolType + " could not be found");
		}
	}
}