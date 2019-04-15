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
import org.hawk.timeaware.queries.operations.declarative.SinceOperation;
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

		operationCache.put("since", new SinceOperation(this::getContainerModel));
	}

}
