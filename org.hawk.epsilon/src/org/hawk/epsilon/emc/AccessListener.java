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
package org.hawk.epsilon.emc;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.query.IAccess;
import org.hawk.core.query.IAccessListener;

public class AccessListener implements IAccessListener {

	private Set<IAccess> accesses = new HashSet<>();

	private String sourceObject;

	public void accessed(String accessObject, String property) {
		accesses.add(new Access(sourceObject, accessObject, property));
	}

	public void setSourceObject(String s) {

		sourceObject = s;

	}

	public Set<IAccess> getAccesses() {
		return accesses;
	}

	public void resetAccesses() {
		accesses.clear();
	}

	@Override
	public void removeAccess(IAccess a) {
		accesses.remove(a);
	}

}
