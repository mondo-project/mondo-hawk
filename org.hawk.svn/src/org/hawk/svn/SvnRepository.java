/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Ossmeter team (https://opensourceprojects.eu/p/ossmeter) - SVN delta computation algorithm
 ******************************************************************************/
package org.hawk.svn;

import org.hawk.core.VcsRepository;

public class SvnRepository extends VcsRepository {

	private String un;
	private String pw;

	public SvnRepository(String url) {
		super(url);
	}

	public String getUsername() {
		return un;
	}

	public void setUsername(String username) {
		un = username;
	}

	public String getPassword() {
		return pw;
	}

	public void setPassword(String password) {
		pw = password;
	}

}