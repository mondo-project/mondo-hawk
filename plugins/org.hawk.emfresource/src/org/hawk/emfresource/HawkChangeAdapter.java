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
package org.hawk.emfresource;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class HawkChangeAdapter implements HawkResourceChangeListener {

	@Override
	public void featureInserted(final EObject source, final EStructuralFeature eAttr, final Object o) {
		// empty by default
	}

	@Override
	public void featureDeleted(final EObject eob, final EStructuralFeature eAttr, final Object oldValue) {
		// empty by default
	}

	@Override
	public void instanceDeleted(final EClass eClass, final EObject eob) {
		// empty by default
	}

	@Override
	public void instanceInserted(final EClass eClass, final EObject eob) {
		// empty by default
	}

	@Override
	public void dataTypeDeleted(final EClassifier eType, final Object oldValue) {
		// empty by default
	}

	@Override
	public void dataTypeInserted(final EClassifier eType, final Object newValue) {
		// empty by default
	}

}
