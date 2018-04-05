/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
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
package org.hawk.service.api.dt.prefs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

public class ServerStore {
	private static final String HAWK_SERVERS_PREFERENCE = "hawkServerURLs";
	private static final String URL_SEPARATOR = ",";

	private final IPreferenceStore prefStore;

	public ServerStore(IPreferenceStore prefStore) {
		this.prefStore = prefStore;
	}

	public List<Server> readAllServers() {
		final String sValue = prefStore.getString(HAWK_SERVERS_PREFERENCE);

		final ArrayList<Server> servers = new ArrayList<>();
		if (!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(sValue)) {
			String[] urls = sValue.split(URL_SEPARATOR);
			for (String url : urls) {
				servers.add(new Server(url));
			}
		}

		return servers;
	}

	public void saveAllServers(List<Server> servers) {
		StringBuffer sbuf = new StringBuffer();
		boolean first = true;
		for (Server server : servers) {
			if (first) {
				first = false;
			} else {
				sbuf.append(URL_SEPARATOR);
			}
			sbuf.append(server.getBaseURL());
		}
		prefStore.putValue(HAWK_SERVERS_PREFERENCE, sbuf.toString());
	}
}
