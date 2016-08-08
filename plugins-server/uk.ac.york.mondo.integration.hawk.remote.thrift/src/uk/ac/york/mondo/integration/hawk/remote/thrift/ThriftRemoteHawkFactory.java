/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.remote.thrift;

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

import uk.ac.york.mondo.integration.api.Hawk;
import uk.ac.york.mondo.integration.api.HawkInstance;
import uk.ac.york.mondo.integration.api.dt.http.LazyCredentials;
import uk.ac.york.mondo.integration.api.utils.APIUtils;
import uk.ac.york.mondo.integration.api.utils.APIUtils.ThriftProtocol;

public class ThriftRemoteHawkFactory implements IHawkFactory {

	@Override
	public IHawk create(String name, File parentFolder, String location, ICredentialsStore credStore, IConsole console, List<String> enabledPlugins) throws Exception {
		return new ThriftRemoteHawk(name, location, parentFolder, credStore, console, ThriftProtocol.guessFromURL(location), enabledPlugins);
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
