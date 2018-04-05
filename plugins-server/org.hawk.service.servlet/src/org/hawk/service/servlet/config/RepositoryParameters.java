/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Orjuwan Al-Wadeai - Initial Implementation of Hawk Server Configuration
 ******************************************************************************/
package org.hawk.service.servlet.config;

public class RepositoryParameters {
	
	private String type;
	private String location;
	private String user;
	private String pass;
	private boolean isFrozen;
	
	public RepositoryParameters(String type, String location, String user,
			String pass, boolean isFrozen) {
		this.type = type;
		this.location = location;
		this.user = user;
		this.pass = pass;
		this.isFrozen = isFrozen;
	}
	
	public RepositoryParameters() {
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPass() {
		return pass;
	}
	public void setPass(String pass) {
		this.pass = pass;
	}
	public boolean isFrozen() {
		return isFrozen;
	}
	public void setFrozen(boolean isFrozen) {
		this.isFrozen = isFrozen;
	}

}

