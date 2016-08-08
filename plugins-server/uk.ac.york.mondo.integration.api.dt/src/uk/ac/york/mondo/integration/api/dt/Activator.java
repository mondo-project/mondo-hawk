/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.api.dt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import uk.ac.york.mondo.integration.api.dt.prefs.CredentialsStore;
import uk.ac.york.mondo.integration.api.dt.prefs.ServerStore;

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
