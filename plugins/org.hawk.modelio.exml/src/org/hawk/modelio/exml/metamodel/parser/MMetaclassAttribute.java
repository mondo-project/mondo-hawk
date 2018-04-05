/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser 
 ******************************************************************************/

package org.hawk.modelio.exml.metamodel.parser;

public class MMetaclassAttribute {

	private String name;
	private MAttributeType type;

	public MMetaclassAttribute() {
	}

	public MMetaclassAttribute(String name, MAttributeType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MAttributeType getType() {
		return type;
	}

	public void setType(MAttributeType type) {
		this.type = type;
	}

}
