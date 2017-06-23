/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser 
 ******************************************************************************/

package org.hawk.modelio.metamodel.parser;

import java.util.HashMap;

public class MLinkMetaclass extends MMetaclass { 

	private HashMap<String, MMetaclassDependency> targetsDeps;
	private HashMap<String, MMetaclassDependency> sourcesDeps;

	public MLinkMetaclass() {
		targetsDeps = new HashMap<String, MMetaclassDependency>();
		sourcesDeps = new HashMap<String, MMetaclassDependency>();
	}

	public HashMap<String, MMetaclassDependency> getTargetsDeps() {
		return targetsDeps;
	}

	public void setTargetsDeps(HashMap<String, MMetaclassDependency> targetsDeps) {
		this.targetsDeps = targetsDeps;
	}

	public HashMap<String, MMetaclassDependency> getSourcesDeps() {
		return sourcesDeps;
	}

	public void setSourcesDeps(HashMap<String, MMetaclassDependency> sourcesDeps) {
		this.sourcesDeps = sourcesDeps;
	}

	public void addSource(String name) {
		sourcesDeps.put(name, null);
	}

	public void addTarget(String name) {
		targetsDeps.put(name, null);
	}

}
