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
