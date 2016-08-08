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
package uk.ac.york.mondo.integration.api.dt.http;

import java.security.Principal;
import java.util.List;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.auth.Credentials;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import uk.ac.york.mondo.integration.api.dt.Activator;
import uk.ac.york.mondo.integration.api.dt.prefs.CredentialsStore;
import uk.ac.york.mondo.integration.api.dt.prefs.Server;
import uk.ac.york.mondo.integration.api.dt.prefs.ServerStore;
import uk.ac.york.mondo.integration.api.dt.ui.ServerConfigurationDialog;

/**
 * Uses the Eclipse secure store to provide auth credentials. If needed, it will
 * show a server config dialog for entering the username/password.
 */
public class LazyCredentials implements Credentials {

	private final class CredentialsPrompter implements Runnable {
		private final Display display;
		private CredentialsStore.Credentials creds;

		private CredentialsPrompter(Display display) {
			this.display = display;
		}

		@Override
		public void run() {
			ServerConfigurationDialog dlg = new ServerConfigurationDialog(display.getActiveShell(), "Authentication required", url);
			if (dlg.open() == Dialog.OK) {
				final ServerStore serverStore = Activator.getDefault().getServerStore();
				List<Server> servers = serverStore.readAllServers();
				servers.add(new Server(dlg.getLocation()));
				serverStore.saveAllServers(servers);

				creds = new CredentialsStore.Credentials(dlg.getUsername(), dlg.getPassword());
				try {
					final CredentialsStore credentialsStore = Activator.getDefault().getCredentialsStore();
					credentialsStore.put(url, creds);
					credentialsStore.flush();
				} catch (Exception e) {
					Activator.getDefault().logError(e);
				}
			}
		}

		public CredentialsStore.Credentials getCredentials() {
			return creds;
		}
	}

	private final String url;
	private Principal principal;
	private String password;

	public LazyCredentials(String url) {
		this.url = url;
	}

	@Override
	public Principal getUserPrincipal() {
		if (principal == null) {
			getCredentials();
			if (principal == null) {
				return new BasicUserPrincipal("");
			}
		}
		return principal;
	}

	@Override
	public String getPassword() {
		if (password == null) {
			getCredentials();
			if (password == null) {
				return "";
			}
		}
		return password;
	}

	/**
	 * Returns the principal if it has been fetched by a previous call of
	 * {@link #getUserPrincipal()}, otherwise returns <code>null</code>.
	 */
	public Principal getRawUserPrincipal() {
		return principal;
	}

	/**
	 * Returns the password if it has been fetched by a previous call of
	 * {@link #getPassword()}, otherwise returns <code>null</code>.
	 */
	public String getRawPassword() {
		return password;
	}

	protected void getCredentials() {
		try {
			// Search within the registered servers by prefix
			final List<Server> servers = Activator.getDefault().getServerStore().readAllServers();
			String storeKey = url;
			for (Server server : servers) {
				if (url.startsWith(server.getBaseURL())) {
					storeKey = server.getBaseURL();
					break;
				}
			}

			CredentialsStore.Credentials creds = Activator.getDefault().getCredentialsStore().get(storeKey);
			if (creds == null && PlatformUI.isWorkbenchRunning()) {
				final Display display = PlatformUI.getWorkbench().getDisplay();
				final CredentialsPrompter prompter = new CredentialsPrompter(display);
				display.syncExec(prompter);
				creds = prompter.getCredentials();
			}
			if (creds != null) {
				principal = new BasicUserPrincipal(creds.getUsername());
				password = creds.getPassword();
			}
		} catch (Exception e) {
			Activator.getDefault().logError(e);
		}
	}

}
