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
package org.hawk.localfolder;

import org.hawk.core.VcsRepository;

public class LocalFolderRepository extends VcsRepository {
	public LocalFolderRepository(String url) {
		super(url);
	}

	@Override
	public String toString() {
		return "LocalFolderRepository [getUrl()=" + getUrl() + "]";
	}
}