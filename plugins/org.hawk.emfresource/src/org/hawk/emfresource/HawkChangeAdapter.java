/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
