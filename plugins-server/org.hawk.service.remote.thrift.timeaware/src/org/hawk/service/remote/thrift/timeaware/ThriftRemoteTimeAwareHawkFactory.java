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
package org.hawk.service.remote.thrift.timeaware;

import org.hawk.service.remote.thrift.ThriftRemoteHawkFactory;
import org.hawk.timeaware.factory.TimeAwareHawkFactory;

/**
 * Simple tweak to the Thrift remote hawk factory to create time-aware indices remotely.
 * Kept in a separate to avoid putting server-side bits in .timeaware and to avoid putting
 * time-aware bits in service.remote.thrift. 
 */
public class ThriftRemoteTimeAwareHawkFactory extends ThriftRemoteHawkFactory {

	@Override
	protected String getFactoryName() {
		return TimeAwareHawkFactory.class.getCanonicalName();
	}

}
