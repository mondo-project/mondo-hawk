/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.io.File;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IModelIndexer;

public class LocalHawk implements IHawk {

	private File location;
	private IModelIndexer indexer;
	private String dbtype;

	public LocalHawk(String name, File storageFolder, ICredentialsStore credStore, IConsole c) throws Exception {
		location = storageFolder;
		indexer = new ModelIndexerImpl(name, location, credStore, c);
	}

	@Override
	public IModelIndexer getModelIndexer() {
		return indexer;
	}

	@Override
	public String getDatabaseType() {
		return dbtype;
	}

	@Override
	public void setDatabaseType(String dbtype) {
		this.dbtype = dbtype;
	}

	@Override
	public boolean exists() {
		return location.exists();
	}

}
