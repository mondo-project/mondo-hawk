/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Initial Implementation of Hawk Server Configuration
 ******************************************************************************/
package org.hawk.service.servlet.config;

import java.util.ArrayList;
import java.util.List;

public class HawkInstanceConfig {

	private String fileName;
	
	private String name;

	private String backend;
	
	private int delayMax;
	
	private int delayMin;
	
	private boolean isModified;
	
	private List<String> plugins;
	
	//private List<String> metamodels;
	private List<MetamodelParameters> metamodels;

	private List<RepositoryParameters> repositories;

	private List<DerivedAttributeParameters> derivedAttributes;

	private List<IndexedAttributeParameters> indexedAttributes;

	
	public HawkInstanceConfig() {
		this.plugins = new ArrayList<String>();
		this.metamodels = new ArrayList<MetamodelParameters>();
		this.repositories = new ArrayList<RepositoryParameters>();
		this.derivedAttributes = new ArrayList<DerivedAttributeParameters>();
		this.indexedAttributes = new ArrayList<IndexedAttributeParameters>();
	}
	
	public HawkInstanceConfig(String fileName) {
		this();
		this.fileName = fileName;
		
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getBackend() {
		return backend;
	}


	public void setBackend(String backend) {
		this.backend = backend;
	}


	public int getDelayMax() {
		return delayMax;
	}


	public void setDelayMax(int delayMax) {
		this.delayMax = delayMax;
	}


	public int getDelayMin() {
		return delayMin;
	}


	public void setDelayMin(int delayMin) {
		this.delayMin = delayMin;
	}


	public List<String> getPlugins() {
		return plugins;
	}


	public void setPlugins(List<String> plugins) {
		this.plugins = plugins;
	}


	public List<MetamodelParameters> getMetamodels() {
		return metamodels;
	}


	public void setMetamodels(List<MetamodelParameters> metamodels) {
		this.metamodels = metamodels;
	}


	public List<RepositoryParameters> getRepositories() {
		return repositories;
	}


	public void setRepositories(List<RepositoryParameters> repositories) {
		this.repositories = repositories;
	}


	public List<DerivedAttributeParameters> getDerivedAttributes() {
		return derivedAttributes;
	}


	public void setDerivedAttributes(
			List<DerivedAttributeParameters> derivedAttributes) {
		this.derivedAttributes = derivedAttributes;
	}


	public List<IndexedAttributeParameters> getIndexedAttributes() {
		return indexedAttributes;
	}


	public void setIndexedAttributes(
			List<IndexedAttributeParameters> indexedAttributes) {
		this.indexedAttributes = indexedAttributes;
	}


	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}
	
	
	

}

