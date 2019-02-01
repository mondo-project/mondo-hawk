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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.remote.thrift;

import org.hawk.core.IMetaModelIntrospector;
import org.hawk.core.IModelIndexer;

/**
 * Introspector factory for remote instances over the Thrift API. It's really
 * just an adapter in this case: the same class acts as indexer and introspector.
 */
public class ThriftRemoteMetaModelIntrospectorFactory implements IMetaModelIntrospector.Factory {

	@Override
	public boolean canIntrospect(IModelIndexer idx) {
		return idx instanceof ThriftRemoteModelIndexer;
	}

	@Override
	public IMetaModelIntrospector createFor(IModelIndexer idx) {
		return (ThriftRemoteModelIndexer) idx;
	}

	@Override
	public String getHumanReadableName() {
		return "Thrift-based remote metamodel introspector factory";
	}
}
