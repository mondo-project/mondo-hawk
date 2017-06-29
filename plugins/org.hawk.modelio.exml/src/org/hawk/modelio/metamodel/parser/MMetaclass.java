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

public class MMetaclass {

	private String name;
	private String version;
	private boolean isAbstract;
	private boolean isCmsNode;

	private MMetaclassReference parent;

	private List<MMetaclassAttribute> attributes;
	private List<MMetaclassReference> children;

	private Map<String, MMetaclassDependency> dependencies;

	public MMetaclass() {
		attributes = new ArrayList<MMetaclassAttribute>();
		dependencies = new HashMap<String, MMetaclassDependency>();
		children = new ArrayList<MMetaclassReference>();
				
	}

	public MMetaclass(String name) {
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

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isCmsNode() {
		return isCmsNode;
	}

	public void setCmsNode(boolean isCmsNode) {
		this.isCmsNode = isCmsNode;
	}

	public MMetaclassReference getParent() {
		return parent;
	}

	public void setParent(MMetaclassReference parent) {
		this.parent = parent;
	}

	public List<MMetaclassAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<MMetaclassAttribute> attributes) {
		this.attributes = attributes;
	}

	public Map<String, MMetaclassDependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(Map<String, MMetaclassDependency> dependencies) {
		this.dependencies = dependencies;
	}

	public void addAttribute(MMetaclassAttribute attribute) {
		attributes.add(attribute);
	}

	public void addDependency(MMetaclassDependency dependency) {
		dependencies.put(dependency.getName(), dependency);
	}

	public MMetaclassDependency getDependency(String name) {
		return dependencies.get(name);
	}

	public void addChild(MMetaclassReference metaclassRef) {
		this.children.add(metaclassRef);		
	}

	public List<MMetaclassReference> getChildren() {
		return children;
	}

	public void setChildren(List<MMetaclassReference> children) {
		this.children = children;
	}

}
