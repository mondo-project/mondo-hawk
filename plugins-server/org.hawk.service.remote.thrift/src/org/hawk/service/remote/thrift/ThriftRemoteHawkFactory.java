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
import java.net.URISyntaxException;
import java.util.List;

import org.apache.thrift.transport.TTransportException;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IStateListener;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.HawkInstance;
import org.hawk.service.api.dt.http.LazyCredentials;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;

public class ThriftRemoteHawkFactory implements IHawkFactory {

	@Override
	public IHawk create(String name, File parentFolder, String location, ICredentialsStore credStore, IConsole console, List<String> enabledPlugins) throws Exception {
		return new ThriftRemoteHawk(
			name, location, getFactoryName(),
			parentFolder, credStore, console,
			ThriftProtocol.guessFromURL(location), enabledPlugins);
	}

	protected String getFactoryName() {
		return null;
	}

	@Override
	public boolean instancesAreExtensible() {
		return false;
	}

	@Override
	public boolean instancesCreateGraph() {
		return false;
	}

	@Override
	public boolean instancesUseLocation() {
		return true;
	}

	@Override
	public InstanceInfo[] listInstances(String location) throws Exception {
		final Hawk.Client client = getClient(location);

		final List<HawkInstance> instances = client.listInstances();
		final InstanceInfo[] infos = new InstanceInfo[instances.size()];
		for (int iInfo = 0; iInfo < instances.size(); ++iInfo) {
			HawkInstance instance = instances.get(iInfo);

			IStateListener.HawkState hawkState = HawkState.STOPPED;
			switch (instance.state) {
			case RUNNING:
				hawkState = HawkState.RUNNING;
				break;
			case STOPPED:
				hawkState = HawkState.STOPPED;
				break;
			case UPDATING:
				hawkState = HawkState.UPDATING;
				break;
			}
			infos[iInfo] = new InstanceInfo(instance.name, null, hawkState);
		}

		return infos;
	}

	protected Hawk.Client getClient(String location) throws TTransportException, URISyntaxException {
		ThriftProtocol proto = ThriftProtocol.guessFromURL(location);
		Hawk.Client client = APIUtils.connectTo(Hawk.Client.class, location, proto, new LazyCredentials(location));
		return client;
	}

	@Override
	public List<String> listBackends(String location) throws Exception {
		final Hawk.Client client = getClient(location);
		return client.listBackends();
	}

	@Override
	public List<String> listPlugins(String location) throws Exception {
		final Hawk.Client client = getClient(location);
		return client.listPlugins();
	}

}
