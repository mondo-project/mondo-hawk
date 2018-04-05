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
package org.hawk.service.api.dt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hawk.service.api.dt.prefs.CredentialsStore;
import org.hawk.service.api.dt.prefs.ServerStore;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	private static Activator plugin;

	private CredentialsStore credsStore;
	private ServerStore serverStore;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		this.credsStore = new CredentialsStore();
		this.serverStore = new ServerStore(getPreferenceStore());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		credsStore = null;
		serverStore = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	public void logError(Throwable ex) {
		getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), ex.getMessage(), ex));
	}

	public CredentialsStore getCredentialsStore() {
		return credsStore;
	}

	public ServerStore getServerStore() {
		return serverStore;
	}
}
