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
package org.hawk.core.security;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("credentials")
public class CredentialsFile {

	@XStreamAlias("salt")
	protected String base64Salt;

	@XStreamAlias("entries")
	protected Collection<CredentialsFileEntry> entries;

	public String getBase64Salt() {
		return base64Salt;
	}

	public void setBase64Salt(String base64Salt) {
		this.base64Salt = base64Salt;
	}

	public Collection<CredentialsFileEntry> getEntries() {
		return entries;
	}

	public void setEntries(Collection<CredentialsFileEntry> entries) {
		this.entries = entries;
	}

	public CredentialsFile() {
		// nothing to do
	}

	public CredentialsFile(String base64Salt, Collection<CredentialsFileEntry> entries) {
		super();
		this.base64Salt = base64Salt;
		this.entries = entries;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base64Salt == null) ? 0 : base64Salt.hashCode());
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
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
		CredentialsFile other = (CredentialsFile) obj;
		if (base64Salt == null) {
			if (other.base64Salt != null)
				return false;
		} else if (!base64Salt.equals(other.base64Salt))
			return false;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		return true;
	}
}
