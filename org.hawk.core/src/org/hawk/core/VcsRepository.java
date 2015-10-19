/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Nikolas Matragkas, James Williams, Dimitris Kolovos - initial API and implementation
 *     Konstantinos Barmpis - adaption for use in Hawk
 ******************************************************************************/
package org.hawk.core;

public abstract class VcsRepository {

	private final String url;
	private final IVcsManager manager;

	public VcsRepository(String url, IVcsManager manager) {
		this.url = url;
		this.manager = manager;
	}

	public String getUrl() {
		return url;
	}

	public IVcsManager getManager() {
		return manager;
	}
}