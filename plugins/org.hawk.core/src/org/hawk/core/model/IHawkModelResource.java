/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
