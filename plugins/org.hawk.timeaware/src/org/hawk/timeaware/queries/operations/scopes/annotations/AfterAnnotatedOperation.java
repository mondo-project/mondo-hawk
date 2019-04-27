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
package org.hawk.timeaware.queries.operations.scopes.annotations;

import java.util.function.Supplier;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeIndex;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.timeaware.queries.operations.scopes.StartingTimeAwareNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variant of <code>after</code> which uses a predefined derived Boolean attribute.
 */
public class AfterAnnotatedOperation extends AbstractAnnotatedOperation {

	private static final Logger LOGGER = LoggerFactory.getLogger(AfterAnnotatedOperation.class);

	public AfterAnnotatedOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		super(containerModelSupplier);
	}

	@Override
	protected ITimeAwareGraphNode useAnnotations(ITimeAwareGraphNodeIndex index, ITimeAwareGraphNode taNode, String derivedAttrName) {
		final Long firstVersion = index.getFirstVersionSince(taNode, derivedAttrName, true);
		if (firstVersion == null) {
			return null;
		}

		final ITimeAwareGraphNode node = taNode.travelInTime(firstVersion);
		try {
			ITimeAwareGraphNode next = node.getNext();
			if (next != null) {
				return new StartingTimeAwareNodeWrapper(next);
			}
		} catch (Exception e) {
			LOGGER.error("Could not fetch next version", e);
		}

		return null;
	}

}
