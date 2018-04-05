/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
package org.hawk.service.emf.impl;

import org.eclipse.emf.common.util.URI;

/**
 * Resource factory that forces Hawk resources to not split their contents
 * across surrogate fields. This is needed for transformation tools such
 * as CloudATL, for instance.
 */
public class UnsplitHawkResourceFactoryImpl extends HawkResourceFactoryImpl {

	@Override
	public HawkResourceImpl createResource(URI uri) {
		HawkResourceImpl r = super.createResource(uri);
		r.getDescriptor().setSplit(false);
		return r;
	}

}
