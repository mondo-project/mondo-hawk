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

import java.io.File;

/**
 * Reference to a Modelio object.
 */
public class ExmlReference {

	private final File srcFile;
	private String name;
	private String mClassName;
	private String uid;

	public ExmlReference(File f) {
		this.srcFile = f;
	}

	public File getFile() {
		return srcFile;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mClassName == null) ? 0 : mClassName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((srcFile == null) ? 0 : srcFile.hashCode());
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExmlReference other = (ExmlReference) obj;
		if (mClassName == null) {
			if (other.mClassName != null)
				return false;
		} else if (!mClassName.equals(other.mClassName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (srcFile == null) {
			if (other.srcFile != null)
				return false;
		} else if (!srcFile.equals(other.srcFile))
			return false;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}
}