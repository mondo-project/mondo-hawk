/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
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
package org.hawk.emf;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EModelElement;
import org.hawk.core.model.IHawkAnnotation;

/**
 * Adds support for annotations to all subclasses that allow them.
 */
public class EMFModelElement extends EMFObject {

	public EMFModelElement(EModelElement o, EMFWrapperFactory wf) {
		super(o, wf);
	}

	@Override
	public EModelElement getEObject() {
		return (EModelElement) super.getEObject();
	}

	@Override
	public Set<IHawkAnnotation> getAnnotations() {
		return getEObject().getEAnnotations().stream()
			.map(ann -> new EMFAnnotation(ann))
			.collect(Collectors.toSet());
	}

}
