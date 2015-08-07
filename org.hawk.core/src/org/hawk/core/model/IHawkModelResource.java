/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.model;

import java.util.Iterator;
import java.util.Set;

public interface IHawkModelResource extends IHawkResource {

	// type of resource factory used to get this model resource 
	String getType();

	Iterator<IHawkObject> getAllContents();

	Set<IHawkObject> getAllContentsSet();

	int getSignature(IHawkObject o);
}
