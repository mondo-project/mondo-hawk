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

public class IndexedAttributeParameters {

	String metamodelUri;
	String typeName;
	String attributeName;

	public IndexedAttributeParameters(String metamodelUri, String typeName,
			String attributeName) {
		super();
		this.metamodelUri = metamodelUri;
		this.typeName = typeName;
		this.attributeName = attributeName;
	}

	public IndexedAttributeParameters() {
		// TODO Auto-generated constructor stub
	}

	public String getMetamodelUri() {
		return metamodelUri;
	}
	public void setMetamodelUri(String metamodelUri) {
		this.metamodelUri = metamodelUri;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
}

