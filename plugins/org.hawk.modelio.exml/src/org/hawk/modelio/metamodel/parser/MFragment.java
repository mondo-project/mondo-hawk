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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MFragment {

	private String name;
	private String version;
	private String provider;
	private String providerVersion;

	private Map<String, MMetaclass> metaclasses;

	private List<MFragmentReference> dependencies;

	private Map<String, MAttributeType> dataTypes;

	private String xmlString;
	
	public MFragment() {
		dependencies = new ArrayList<MFragmentReference>();
		metaclasses = new HashMap<String, MMetaclass>();
		dataTypes = new HashMap<String, MAttributeType>();
	}

	public MFragment(String name, String version, String provider,
			String providerVersion) {
		this();
		this.name = name;
		this.version = version;
		this.provider = provider;
		this.providerVersion = providerVersion;
	}

	public MFragment(String name) {
		this();
		this.name = name;
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

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderVersion() {
		return providerVersion;
	}

	public void setProviderVersion(String providerVersion) {
		this.providerVersion = providerVersion;
	}

	public List<MFragmentReference> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<MFragmentReference> dependencies) {
		this.dependencies = dependencies;
	}

	public Map<String, MMetaclass> getMetaclasses() {
		return metaclasses;
	}

	public void setMetaclasses(Map<String, MMetaclass> metaclasses) {
		this.metaclasses = metaclasses;
	}

	public void addDependency(MFragmentReference fragmentRef) {
		this.dependencies.add(fragmentRef);
	}

	public void addMetaclass(MMetaclass metaclass) {
		this.metaclasses.put(metaclass.getName(), metaclass);
	}

	public MMetaclass getMetaclass(String name) {
		return this.metaclasses.get(name);
	}

	public Map<String, MAttributeType> getDataTypes() {
		return dataTypes;
	}

	public void setDataTypes(Map<String, MAttributeType> dataTypes) {
		this.dataTypes = dataTypes;
	}
	
	public void addDataType(MAttributeType dataType) {
		this.dataTypes.put(dataType.getName(), dataType);
	}
	
	public MAttributeType getDataType(String dataTypeName) {
		return this.dataTypes.get(dataTypeName);
	}

	public String getXmlString() {
		return xmlString;
	}

	public void setXmlString(String fragmentXmlString) {
		this.xmlString = fragmentXmlString;
	}
}
