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

import java.util.ArrayList;
import java.util.List;

public class MLinkMetaclass extends MMetaclass { // might extend Metaclass

	private List<String> targetsDeps;
	private List<String> sourcesDeps;

	public MLinkMetaclass() {
		targetsDeps = new ArrayList<String>();
		sourcesDeps = new ArrayList<String>();
	}

	public List<String> getTargetsDeps() {
		return targetsDeps;
	}

	public void setTargetsDeps(List<String> targetsDeps) {
		this.targetsDeps = targetsDeps;
	}

	public List<String> getSourcesDeps() {
		return sourcesDeps;
	}

	public void setSourcesDeps(List<String> sourcesDeps) {
		this.sourcesDeps = sourcesDeps;
	}

	public void addSource(String name) {
		sourcesDeps.add(name);
	}

	public void addTarget(String name) {
		targetsDeps.add(name);
	}

}
