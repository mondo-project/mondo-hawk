/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup and use covariant return types
 ******************************************************************************/
package org.hawk.emf;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;

public class EMFPackage extends EMFModelElement implements IHawkPackage {

	private EPackage ep;
	private IHawkMetaModelResource r;

	public EMFPackage(EPackage e, EMFWrapperFactory wf, IHawkMetaModelResource res) {
		super(e, wf);
		ep = e;
		r = res;
	}

	@Override
	public String getName() {
		return ep.getName();
	}

	@Override
	public EPackage getEObject() {
		return ep;
	}

	@Override
	public IHawkClass getClassifier(String string) {
		EClassifier e = ep.getEClassifier(string);
		if (e instanceof EClass) {
			return wf.createClass((EClass) e);
		} else {
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
	public Set<IHawkClassifier> getClasses() {
		Set<IHawkClassifier> ret = new HashSet<>();

		for (EClassifier e : ep.getEClassifiers()) {
			if (e instanceof EClass) {
				ret.add(wf.createClass((EClass) e));
			} else if (e instanceof EDataType) {
				ret.add(wf.createDataType((EDataType) e));
			}
		}

		return ret;
	}

	@Override
	public IHawkMetaModelResource getResource() {
		return r;
	}

	@Override
	public int hashCode() {
		return ep.hashCode();
	}
}
