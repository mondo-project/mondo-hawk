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
import org.hawk.timeaware.queries.operations.declarative.AlwaysOperation;

/**
 * Extended version of the EOL operation factory, adding a new set of first-order
 * operations over the versions of types and nodes.
 */
public class TimeAwareEOLOperationFactory extends EolOperationFactory {

	private final EOLQueryEngine containerModel;

	public TimeAwareEOLOperationFactory(EOLQueryEngine model) {
		this.containerModel = model;
	}

	@Override
	protected void createCache() {
		super.createCache();
		operationCache.put("always", new AlwaysOperation(containerModel));
	}

}
