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
 *     Antonio Garcia-Dominguez - cleanup and use covariant return types
 ******************************************************************************/
package org.hawk.emf;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EAnnotation;
import org.hawk.core.model.IHawkAnnotation;



public class EMFAnnotation implements IHawkAnnotation {
	private EAnnotation ann;

	public EMFAnnotation(EAnnotation a) {
		ann = a;
	}

	@Override
	public String getSource() {
		return ann.getSource();
	}

	@Override
	public Map<String, String> getDetails() {
		final Map<String, String> m = new HashMap<>();
		for (Entry<String, String> entry : ann.getDetails().entrySet()) {
			m.put(entry.getKey(), entry.getValue());
		}
		return m;
	}
}
