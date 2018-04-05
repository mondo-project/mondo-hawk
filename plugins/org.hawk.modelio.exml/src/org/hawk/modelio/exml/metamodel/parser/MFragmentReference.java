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

public class MFragmentReference {

	private String name;
	private String version;
	private MFragment fragment;

	public MFragmentReference() {
	}

	public MFragmentReference(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public MFragment getFragment() {
		return fragment;
	}

	public void setFragment(MFragment fragment) {
		this.fragment = fragment;
	}

}
