/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.parser;

/**
 * Reference to a Modelio object.
 */
public class ExmlReference {

	private String name;
	private String mClassName;
	private String uid;

	public ExmlReference() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMClassName() {
		return mClassName;
	}

	public void setMClassName(String mClassName) {
		this.mClassName = mClassName;
	}

	public String getUID() {
		return uid;
	}

	public void setUID(String uid) {
		this.uid = uid;
	}

	@Override
	public String toString() {
		return "ExmlReference [name=" + name + ", mClassName=" + mClassName + ", uid=" + uid + "]";
	}
}