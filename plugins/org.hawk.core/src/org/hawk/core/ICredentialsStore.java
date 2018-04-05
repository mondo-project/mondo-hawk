/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

/**
 * Storage area for the credentials needed to log into a version control system.
 * Credential stores should implement their own lifecycle management, but they
 * should postpone any user prompts until they are absolutely required (e.g.
 * asking for the Eclipse secure storage password).
 *
 * For this reason, this interface does not provide an initialization method:
 * this should be done only after the first call to {@link #get(String)},
 * {@link #remove(String} or {@link #put(String, Credentials)}. A shutdown
 * method is provided for clean closing of the storage, if it had been opened at
 * all during Hawk's execution.
 *
 * Most of the time, only specific {@link IVcsManager} implementations should
 * need to use {@link #put(String, Credentials)} and {@link #remove(String)}.
 * {@link #remove(String)} and {@link #shutdown()} will be normally used by
 * instances of {@link IModelIndexer}.
 */
public interface ICredentialsStore {

	/**
	 * Immutable set of credentials to log into a VCS.
	 */
	public final class Credentials {
		private final String username;
		private final String password;

		public Credentials(String u, String p) {
			this.username = u;
			this.password = p;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
	}

	/**
	 * Stores the credentials associated with the specified key. Opens the
	 * store if needed.
	 */
	void put(String repositoryKey, Credentials creds) throws Exception;

	/**
	 * Retrieves the credentials associated with the specified key, or returns
	 * <code>null</code> if none have been stored yet. Opens the store if
	 * needed.
	 */
	Credentials get(String repositoryKey) throws Exception;

	/**
	 * Removes the credentials associated with the specifed key, if any were
	 * stored. Opens the store if needed.
	 */
	void remove(String repositoryKey) throws Exception;

	/**
	 * Shuts down the store, ensuring that all changes are persisted for the
	 * next time Hawk is run. Does not open the store if it was not open
	 * already.
	 */
	void shutdown() throws Exception;
}
