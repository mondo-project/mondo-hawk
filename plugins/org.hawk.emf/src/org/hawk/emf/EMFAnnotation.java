/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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
