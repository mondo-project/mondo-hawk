/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.service.api.dt.prefs;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CredentialsStore {

	private static final String PASSWORD_SUFFIX = ".password";
	private static final String USERNAME_SUFFIX = ".username";

	private ISecurePreferences preferences;

	public static class Credentials {
		private final String username, password;

		public Credentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
	}

	public void put(String repositoryKey, Credentials creds) throws Exception {
		checkOpen();

		final String encodedPrefix = EncodingUtils.encodeSlashes(repositoryKey);
		preferences.put(encodedPrefix + USERNAME_SUFFIX, creds.getUsername(), true);
		preferences.put(encodedPrefix + PASSWORD_SUFFIX, creds.getPassword(), true);
		preferences.flush();
	}

	public Credentials get(String repositoryKey) throws Exception {
		checkOpen();

		final String encodedPrefix = EncodingUtils.encodeSlashes(repositoryKey);
		String username = preferences.get(encodedPrefix + USERNAME_SUFFIX, null);
		String password = preferences.get(encodedPrefix + PASSWORD_SUFFIX, null);
		if (username != null && password != null) {
			return new Credentials(username, password);
		}
		return null;
	}

	public void remove(String repositoryKey) throws Exception {
		checkOpen();

		final String encodedPrefix = EncodingUtils.encodeSlashes(repositoryKey);
		preferences.remove(encodedPrefix + USERNAME_SUFFIX);
		preferences.remove(encodedPrefix + PASSWORD_SUFFIX);
	}

	public void flush() throws Exception {
		if (preferences != null) {
			preferences.flush();
		}
	}

	private void checkOpen() {
		if (preferences != null) return;

		Bundle bundle = FrameworkUtil.getBundle(CredentialsStore.class);
		preferences = SecurePreferencesFactory.getDefault().node(bundle.getSymbolicName());
	}
}
