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
import java.util.List;

public class MMetaclass {
	private String name;
	private String version;
	private boolean isAbstract;
	private boolean isCmsNode;

	private MMetaclassReference parent;

	private String opposite;

	private List<MAttribute> attributes;
	private List<MMetaclassDependency> dependecies;

	public MMetaclass() {
		attributes = new ArrayList<MAttribute>();
		dependecies = new ArrayList<MMetaclassDependency>();
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

	public String getOpposite() {
		return opposite;
	}

	public void setOpposite(String opposite) {
		this.opposite = opposite;
	}

	public List<MAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<MAttribute> attributes) {
		this.attributes = attributes;
	}

	public List<MMetaclassDependency> getDependecies() {
		return dependecies;
	}

	public void setDependecies(List<MMetaclassDependency> dependecies) {
		this.dependecies = dependecies;
	}

	public void addAttribute(MAttribute attribute) {
		attributes.add(attribute);
	}

	public void addDependency(MMetaclassDependency dependency) {
		dependecies.add(dependency);
	}

}
