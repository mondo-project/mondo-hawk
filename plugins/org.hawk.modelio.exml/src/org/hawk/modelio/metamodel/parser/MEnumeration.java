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

public class MEnumeration extends MAttributeType {

	private List<String> values;

	public MEnumeration(String name) {
		super(name);
		values = new ArrayList<String>();
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public void addValue(String valueName) {
		values.add(valueName);
	}
	
	public boolean isEnum() {
		return true;
	}
}
