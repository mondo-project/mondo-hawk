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

public class DerivedAttributeParameters extends IndexedAttributeParameters{
	
	private String attributeType;
	private boolean isMany;
	private boolean isOrdered;
	private boolean isUnique;
	private String derivationLanguage;
	private String derivationLogic;
	
	public DerivedAttributeParameters() {
		super();
	}

	public DerivedAttributeParameters(String metamodelUri, String typeName,
			String attributeName) {
		super(metamodelUri, typeName, attributeName);
		
	}
	
	public DerivedAttributeParameters(String metamodelUri, String typeName,
			String attributeName, String attributeType, boolean isMany,
			boolean isOrdered, boolean isUnique) {
		super(metamodelUri, typeName, attributeName);
		this.attributeType = attributeType;
		this.isMany = isMany;
		this.isOrdered = isOrdered;
		this.isUnique = isUnique;

	}
	

	public String getAttributeType() {
		return attributeType;
	}
	public void setAttributeType(String attributeType) {
		this.attributeType = attributeType;
	}
	public boolean isMany() {
		return isMany;
	}
	public void setMany(boolean isMany) {
		this.isMany = isMany;
	}
	public boolean isOrdered() {
		return isOrdered;
	}
	public void setOrdered(boolean isOrdered) {
		this.isOrdered = isOrdered;
	}
	public boolean isUnique() {
		return isUnique;
	}
	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}
	public String getDerivationLanguage() {
		return derivationLanguage;
	}
	public void setDerivationLanguage(String derivationLanguage) {
		this.derivationLanguage = derivationLanguage;
	}
	public String getDerivationLogic() {
		return derivationLogic;
	}
	public void setDerivationLogic(String derivationLogic) {
		this.derivationLogic = derivationLogic;
	}
	
	
}

