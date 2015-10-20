/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public interface HawkResourceChangeListener {
	void featureInserted(final EObject source, final EStructuralFeature eAttr, final Object o);

	void featureDeleted(final EObject eob, final EStructuralFeature eAttr, final Object oldValue);

	void instanceDeleted(final EClass eClass, final EObject eob);

	void instanceInserted(final EClass eClass, final EObject eob);

	void dataTypeDeleted(final EClassifier eType, final Object oldValue);

	void dataTypeInserted(final EClassifier eType, final Object newValue);
}
