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
package org.hawk.bpmn;

import java.util.HashSet;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.hawk.core.model.*;

public class BPMNPackage extends BPMNObject implements IHawkPackage {

	private EPackage ep;
	private IHawkMetaModelResource r;

	public BPMNPackage(EPackage e, IHawkMetaModelResource res) {

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
			return new BPMNClass(((EClass) e));
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
				ret.add(new BPMNClass(((EClass) e)));
			else if (e instanceof EDataType)
				ret.add(new BPMNDataType((EDataType) e));
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
