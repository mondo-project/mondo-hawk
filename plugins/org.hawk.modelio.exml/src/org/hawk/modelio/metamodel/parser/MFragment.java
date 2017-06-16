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

public class MFragment {

	private String name;
	private String version;
	private String provider;
	private String providerVersion;

	private List<MFragmentReference> dependencies;

	private List<MEnumeration> enumerations;
	private HashMap<String, MMetaclass> metaclasses;

	public MFragment() {
		dependencies = new ArrayList<MFragmentReference>();
		enumerations = new ArrayList<MEnumeration>();
		metaclasses = new HashMap<String, MMetaclass>();

	}

	public MFragment(String name, String version, String provider,
			String providerVersion) {
		this();
		this.name = name;
		this.version = version;
		this.provider = provider;
		this.providerVersion = providerVersion;
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

	public List<MFragmentReference> getDependecies() {
		return dependencies;
	}

	public void setDependecies(List<MFragmentReference> dependencies) {
		this.dependencies = dependencies;
	}

	public List<MEnumeration> getEnumerations() {
		return enumerations;
	}

	public void setEnumerations(List<MEnumeration> enumerations) {
		this.enumerations = enumerations;
	}

	public HashMap<String, MMetaclass> getMetaclasses() {
		return metaclasses;
	}

	public void setMetaclasses(HashMap<String, MMetaclass> metaclasses) {
		this.metaclasses = metaclasses;
	}

	public void addEnumeration(MEnumeration enumeration) {
		this.enumerations.add(enumeration);
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
}
