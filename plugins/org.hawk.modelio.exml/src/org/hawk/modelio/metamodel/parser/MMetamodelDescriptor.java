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
	private HashMap<String, MDataType> dataTypes;

	public MMetamodelDescriptor() {
		this.fragments = new HashMap<String, MFragment>();
		dataTypes = new HashMap<String, MDataType>();
	}

	public String getMetamodelFormat() {
		return metamodelFormat;
	}

	public void setMetamodelDescriptorFormat(String metamodelDescriptorFormat) {
		this.metamodelDescriptorFormat = metamodelDescriptorFormat;
	}

	public String getMetamodelDescriptorFormat() {
		return metamodelDescriptorFormat;
	}

	public void setMetamodelFormat(String metamodelFormat) {
		this.metamodelFormat = metamodelFormat;
	}
	
	public void setFragments(HashMap<String, MFragment> fragments) {
		this.fragments = fragments;
	}
	
	public HashMap<String, MFragment> getFragments() {
		return fragments;
	}

	public void addFragment(MFragment fragment) {
		this.fragments.put(fragment.getName(), fragment);
	}

	public MFragment getFragment(String name) {
		return this.fragments.get(name);
	}

	public void setDataTypes(HashMap<String, MDataType> dataTypes) {
		this.dataTypes = dataTypes;
	}
	
	public MDataType getDataType(String name) {
		return this.dataTypes.get(name);
	}
	
	public void addDataType(MDataType dataType) {
		this.dataTypes.put(dataType.getName(), dataType);
	}
	
	public HashMap<String, MDataType> getDataTypes() {
		return dataTypes;
	}
	
	public void reset() {
		this.fragments.clear();
		this.dataTypes.clear();
	}

}
