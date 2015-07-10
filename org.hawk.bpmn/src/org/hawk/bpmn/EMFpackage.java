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
package org.hawk.bpmn;

import java.util.HashSet;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.core.model.*;

public class EMFpackage extends EMFobject implements IHawkPackage {

	private EPackage ep;
	private IHawkMetaModelResource r;

	public EMFpackage(EPackage e, IHawkMetaModelResource res) {

		super(e);
		ep = e;
		r = res;

	}

	@Override
	public String getName() {

		return ep.getName();
	}

	@Override
	public IHawkClass getClassifier(String string) {
		EClassifier e = ep.getEClassifier(string);
		if (e instanceof EClass)
			return new EMFclass(((EClass) e));
		else {
			System.err
					.println("attempt to call getEClassifier(String string) on a non-eclass, debug");
			return null;
		}
	}

	@Override
	public String getNsURI() {
		return ep.getNsURI();
	}

	@Override
	public HashSet<IHawkClassifier> getClasses() {

		HashSet<IHawkClassifier> ret = new HashSet<>();

		for (EClassifier e : ep.getEClassifiers()) {
			if (e instanceof EClass)
				ret.add(new EMFclass(((EClass) e)));
			else if (e instanceof EDataType)
				ret.add(new EMFDataType((EDataType) e));
		}

		return ret;
	}

	@Override
	public IHawkMetaModelResource getResource() {
		return r;
	}

	// @Override
	// public HawkMetaModelResource getResource() {
	// return new EMFMetaModelResource(ep.eResource(), p);
	// }
	@Override
	public int hashCode() {
		return ep.hashCode();
	}
}
