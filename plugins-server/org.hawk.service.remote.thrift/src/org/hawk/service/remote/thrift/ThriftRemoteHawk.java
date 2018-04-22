/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.remote.thrift;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.thrift.transport.TTransportException;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IModelIndexer;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.Hawk.Client;
import org.hawk.service.api.dt.http.LazyCredentials;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;

public class ThriftRemoteHawk implements IHawk {

	private final Client client;
	private ThriftRemoteModelIndexer indexer;
	private File folder;

	public ThriftRemoteHawk(String name, String location, File parentFolder, ICredentialsStore credStore, IConsole console, ThriftProtocol thriftProtocol, List<String> enabledPlugins) throws TTransportException, IOException, URISyntaxException {
		this.client = APIUtils.connectTo(Hawk.Client.class, location, thriftProtocol, new LazyCredentials(location));
		this.folder = parentFolder;
		this.indexer = new ThriftRemoteModelIndexer(name, location, parentFolder, client, credStore, console, enabledPlugins);
	}

	@Override
	public IModelIndexer getModelIndexer() {
		return indexer;
	}

	@Override
	public String getDatabaseType() {
		return indexer.getDBType();
	}

	@Override
	public void setDatabaseType(String dbtype) {
		indexer.setDBType(dbtype);
	}

	@Override
	public boolean exists() {
		return folder != null && folder.exists();
	}

}
