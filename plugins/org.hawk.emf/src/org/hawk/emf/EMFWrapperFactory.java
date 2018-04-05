/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
package org.hawk.emf;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.hawk.core.model.IHawkMetaModelResource;

/**
 * Class with various factory methods for creating wrappers over EMF concepts.
 * Should be useful for creating new EMF-based drivers with slight changes by
 * using extension, rather than copying and pasting the same code.
 *
 * With this, we can create subclasses of the wrappers to cover for any
 * specifics in that flavour of EMF.
 */
public class EMFWrapperFactory {
	public EMFAnnotation createAnnotation(EAnnotation ann) {
		return new EMFAnnotation(ann);
	}

	public EMFAttribute createAttribute(EAttribute attr) {
		return new EMFAttribute(attr, this);
	}

	public EMFClass createClass(EClass cls) {
		return new EMFClass(cls, this);
	}

	public EMFDataType createDataType(EDataType dt) {
		return new EMFDataType(dt, this);
	}

	public EMFFeatureMapReference createFeatureMapReference(EAttribute attr) {
		return new EMFFeatureMapReference(attr, this);
	}

	public EMFPackage createPackage(EPackage pkg, IHawkMetaModelResource res) {
		return new EMFPackage(pkg, this, res);
	}

	public EMFObject createObject(EObject obj) {
		return new EMFObject(obj, this);
	}

	public EMFReference createReference(EReference ref) {
		return new EMFReference(ref, this);
	}
}
