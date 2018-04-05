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
package org.hawk.core.query;

import java.util.Set;

public interface IAccessListener {

	public void setSourceObject(String s);

	public void accessed(String accessObject, String property);

	public Set<IAccess> getAccesses();

	public void resetAccesses();

	public void removeAccess(IAccess a);

}
