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

public class MMetaclassReference {
	private String name;
	private String fragmentName;
	private MMetaclass metaclass;
	
	public MMetaclassReference() {

	}

	public MMetaclassReference(String fragment, String name) {
		this.name = name;
		this.fragmentName = fragment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFragmentName() {
		return fragmentName;
	}

	public void setFragmentName(String fragment) {
		this.fragmentName = fragment;
	}

	public MMetaclass getMetaclass() {
		return metaclass;
	}

	public void setMetaclass(MMetaclass metaclass) {
		this.metaclass = metaclass;
	}

	
}
