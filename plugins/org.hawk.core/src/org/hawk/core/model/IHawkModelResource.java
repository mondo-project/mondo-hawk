/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.model;

import java.util.Set;

public interface IHawkModelResource extends IHawkResource {

	// type of resource factory used to get this model resource
	String getType();

	Iterable<IHawkObject> getAllContents();

	Set<IHawkObject> getAllContentsSet();

	/**
	 * If true, model elements in this resource with globally unique fragments (
	 * <code>IHawkObject.isFragmentUnique()</code>) are treated as singleton
	 * elements (at most one copy of them will be present in Hawk)
	 * 
	 * @return
	 */
	boolean providesSingletonElements();

}
