/*******************************************************************************
 * Copyright (c) 2017 Aston University
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
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser 
 ******************************************************************************/

package org.hawk.modelio.exml.metamodel.parser;

import java.util.HashMap;
import java.util.Map;

public class MLinkMetaclass extends MMetaclass { 

	private Map<String, MMetaclassDependency> targetsDeps;
	private Map<String, MMetaclassDependency> sourcesDeps;

	public MLinkMetaclass() {
		targetsDeps = new HashMap<String, MMetaclassDependency>();
		sourcesDeps = new HashMap<String, MMetaclassDependency>();
	}

	public Map<String, MMetaclassDependency> getTargetsDeps() {
		return targetsDeps;
	}

	public void setTargetsDeps(Map<String, MMetaclassDependency> targetsDeps) {
		this.targetsDeps = targetsDeps;
	}

	public Map<String, MMetaclassDependency> getSourcesDeps() {
		return sourcesDeps;
	}

	public void setSourcesDeps(Map<String, MMetaclassDependency> sourcesDeps) {
		this.sourcesDeps = sourcesDeps;
	}

	public void addSource(String name) {
		sourcesDeps.put(name, null);
	}

	public void addTarget(String name) {
		targetsDeps.put(name, null);
	}

}
