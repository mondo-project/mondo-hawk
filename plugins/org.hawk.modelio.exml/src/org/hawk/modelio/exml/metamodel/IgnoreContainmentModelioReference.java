/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import org.hawk.modelio.exml.metamodel.mlib.MDependency;

/**
 * Variant of {@link ModelioReference} that ignores the composition flag of the MDependency.
 * Needed in most cases since Modelio has per-instance containment, which is not compatible
 * with EMF. The solution is to add an extra reference that is always containment, and leave
 * the existing ones as non-containment references.
 */
public class IgnoreContainmentModelioReference extends ModelioReference {
	public IgnoreContainmentModelioReference(ModelioClass mc, MDependency mdep) {
		super(mc, mdep);
	}

	@Override
	public boolean isContainment() {
		return false;
	}

	@Override
	public boolean isContainer() {
		return false;
	}
}
