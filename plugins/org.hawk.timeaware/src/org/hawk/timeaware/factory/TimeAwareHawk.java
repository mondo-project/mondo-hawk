/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.timeaware.factory;

import java.io.File;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IModelIndexer;
import org.hawk.timeaware.graph.TimeAwareIndexer;

public class TimeAwareHawk implements IHawk {

	private File location;
	private IModelIndexer indexer;
	private String dbType;

	public TimeAwareHawk(String name, File storageFolder, ICredentialsStore credStore, IConsole c) throws Exception {
		location = storageFolder;
		indexer = new TimeAwareIndexer(name, location, credStore, c);
	}

	@Override
	public IModelIndexer getModelIndexer() {
		return indexer;
	}

	@Override
	public String getDatabaseType() {
		return dbType;
	}

	@Override
	public void setDatabaseType(String dbType) {
		this.dbType = dbType;
	}

	@Override
	public boolean exists() {
		return location.exists();
	}

}
