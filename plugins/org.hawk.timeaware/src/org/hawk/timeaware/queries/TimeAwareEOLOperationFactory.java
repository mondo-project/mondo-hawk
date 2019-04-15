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
package org.hawk.timeaware.queries;

import org.eclipse.epsilon.eol.execute.operations.EolOperationFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.timeaware.queries.operations.declarative.AlwaysReducer;
import org.hawk.timeaware.queries.operations.declarative.BoundedVersionQuantifierOperation;
import org.hawk.timeaware.queries.operations.declarative.EventuallyAtLeastReducer;
import org.hawk.timeaware.queries.operations.declarative.EventuallyAtMostReducer;
import org.hawk.timeaware.queries.operations.declarative.EventuallyReducer;
import org.hawk.timeaware.queries.operations.declarative.NeverReducer;
import org.hawk.timeaware.queries.operations.declarative.VersionRangeOperation;
import org.hawk.timeaware.queries.operations.declarative.StartingTimeAwareNodeWrapper;
import org.hawk.timeaware.queries.operations.declarative.VersionQuantifierOperation;

/**
 * Extended version of the EOL operation factory, adding a new set of first-order
 * operations over the versions of types and nodes.
 */
public class TimeAwareEOLOperationFactory extends EolOperationFactory {

	private final EOLQueryEngine containerModel;

	public TimeAwareEOLOperationFactory(EOLQueryEngine model) {
		this.containerModel = model;
	}

	public EOLQueryEngine getContainerModel() {
		return containerModel;
	}

	@Override
	protected void createCache() {
		super.createCache();
		operationCache.put("always",
			new VersionQuantifierOperation(this::getContainerModel, new AlwaysReducer()));
		operationCache.put("never",
			new VersionQuantifierOperation(this::getContainerModel, new NeverReducer()));
		operationCache.put("eventually",
			new VersionQuantifierOperation(this::getContainerModel, new EventuallyReducer()));
		operationCache.put("eventuallyAtMost",
			new BoundedVersionQuantifierOperation(this::getContainerModel, (count -> new EventuallyAtMostReducer(count))));
		operationCache.put("eventuallyAtLeast",
			new BoundedVersionQuantifierOperation(this::getContainerModel, (count -> new EventuallyAtLeastReducer(count))));

		/*
		 * First-order operation that makes the target time-aware node travel to the
		 * first timepoint since the current timepoint where a particular predicate was
		 * true, if it exists. The returned time-aware node will only report the
		 * versions since that moment: any past history will be ignored.
		 *
		 * This means that if you want to search through the entire history of a node,
		 * you will have to use <code>node.earliest.since(...)</code>. This is done to
		 * make it easier to restrict the scope of the search in long histories.
		 *
		 * If no such timepoint exists, the operation will return an undefined value,
		 * which can be checked against with <code>.isDefined()</code>.
		 */
		operationCache.put("since",
			new VersionRangeOperation(this::getContainerModel,
				(original, match) -> new StartingTimeAwareNodeWrapper(match)));
	}

}
