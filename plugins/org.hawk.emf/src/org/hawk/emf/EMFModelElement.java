/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
