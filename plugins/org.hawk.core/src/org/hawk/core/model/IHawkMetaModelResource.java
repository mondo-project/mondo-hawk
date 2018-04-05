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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;

public interface IHawkMetaModelResource extends IHawkResource {

	Set<IHawkObject> getAllContents();

	// returns the factory used to create this resource
	IMetaModelResourceFactory getMetaModelResourceFactory();

	// saves the resource to an output stream (such as a string writer), with
	// optional options "options" - used to persist the metamodel into the
	// database for re-creation upon rebooting of hawk
	void save(OutputStream output, Map<Object, Object> options)
			throws IOException;

}
