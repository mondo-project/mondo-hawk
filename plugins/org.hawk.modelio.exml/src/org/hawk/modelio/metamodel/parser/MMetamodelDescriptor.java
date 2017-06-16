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

public class MMetamodelDescriptor {

	private String metamodelFormat;
	private String metamodelDescriptorFormat;

	private HashMap<String, MFragment> fragments;

	public MMetamodelDescriptor() {
		this.fragments = new HashMap<String, MFragment>();
	}

	public HashMap<String, MFragment> getFragments() {
		return fragments;
	}

	public String getMetamodelFormat() {
		return metamodelFormat;
	}

	public void setMetamodelDescriptorFormat(String metamodelDescriptorFormat) {
		this.metamodelDescriptorFormat = metamodelDescriptorFormat;
	}

	public void setFragments(HashMap<String, MFragment> fragments) {
		this.fragments = fragments;
	}

	public String getMetamodelDescriptorFormat() {
		return metamodelDescriptorFormat;
	}

	public void setMetamodelFormat(String metamodelFormat) {
		this.metamodelFormat = metamodelFormat;
	}

	public void clearFragments() {
		this.fragments.clear();
	}

	public void addFragment(MFragment fragment) {
		this.fragments.put(fragment.getName(), fragment);
	}

	public MFragment getFragment(String name) {
		return this.fragments.get(name);
	}

}
