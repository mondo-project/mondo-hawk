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
import java.util.List;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeAwareNodeHistoryOperationContributor extends OperationContributor {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareNodeHistoryOperationContributor.class);

	private EOLQueryEngine model;

	public TimeAwareNodeHistoryOperationContributor(EOLQueryEngine q) {
		this.model = q;
	}

	@Override
	public boolean contributesTo(Object target) {
		return target instanceof GraphNodeWrapper && ((GraphNodeWrapper)target).getNode() instanceof ITimeAwareGraphNode;
	}

	public List<GraphNodeWrapper> getVersionsBetween(long fromInclusive, long toInclusive) throws EolRuntimeException {
		return getRelatedVersions((taNode) -> taNode.getVersionsBetween(fromInclusive, toInclusive));
	}

	public List<GraphNodeWrapper> getVersionsFrom(long fromInclusive) throws EolRuntimeException {
		return getRelatedVersions((taNode) -> taNode.getVersionsFrom(fromInclusive));
	}

	public List<GraphNodeWrapper> getVersionsUpTo(long toInclusive) throws EolRuntimeException {
		return getRelatedVersions((taNode) -> taNode.getVersionsUpTo(toInclusive));
	}

	private List<GraphNodeWrapper> getRelatedVersions(
			final RiskyFunction<ITimeAwareGraphNode, List<ITimeAwareGraphNode>> f) throws EolRuntimeException {
		final ITimeAwareGraphNode taNode = castTarget(target);
		final List<GraphNodeWrapper> results = new ArrayList<>();
		try {
			for (ITimeAwareGraphNode version : f.call(taNode)) {
				results.add(new GraphNodeWrapper(version, model));
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EolRuntimeException(e.getMessage());
		}
		return results;
	}
	
	private ITimeAwareGraphNode castTarget(Object target) {
		return (ITimeAwareGraphNode) ((GraphNodeWrapper)target).getNode();
	}
}