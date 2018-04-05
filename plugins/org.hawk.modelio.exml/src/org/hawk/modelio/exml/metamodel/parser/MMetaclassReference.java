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
