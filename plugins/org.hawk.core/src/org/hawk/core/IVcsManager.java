/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 * 	   Nikolas Matragkas, James Williams, Dimitris Kolovos - initial API and implementation
 *     Konstantinos Barmpis - adaptation for use in Hawk
 *     Antonio Garcia-Dominguez - further additions + javadocs
 ******************************************************************************/
package org.hawk.core;

import java.io.File;
import java.util.Collection;

import org.hawk.core.IHawkPlugin.Category;
import org.hawk.core.model.IHawkObject;

public interface IVcsManager extends IHawkPlugin {

	/**
	 * Returns an identifier for the last revision available at this location.
	 */
	String getCurrentRevision() throws Exception;

	/**
	 * Returns an identifier for the first revision available at this location.
	 */
	String getFirstRevision() throws Exception;

	/**
	 * Returns the set of changed items from this revision.
	 */
	Collection<VcsCommitItem> getDelta(String startRevision) throws Exception;

	/**
	 * Returns the set of changed items between these revisions.
	 */
	VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception;

	/**
	 * Places the contents of the resource located at <code>path</code> in a local
	 * file. Hawk will provide a suggested temporary file location through
	 * <code>optionalTemp</code>, but the implementation may use another one (e.g.
	 * the file might already be available on disk somewhere else). The
	 * implementation must return the file that should be read in the end.
	 * Implementations should be careful to preserve relative paths between the
	 * files in the same repository when implementing this method.
	 *
	 * Returns <code>null</code> if the file could not be found.
	 *
	 * @param revision
	 *            Identifier of the desired revision of the specified file.
	 *            Implementations that do not support retrieving past versions of
	 *            the file may ignore this argument.
	 * @param path
	 *            Path within the repository where the file is stored.
	 * @param optionalTemp
	 *            If the file is not available as-is in the local filesystem (e.g.
	 *            needs to be retrieved over the network), this argument provides a
	 *            location where the implementation can temporarily save it.
	 */
	File importFile(String revision, String path, File optionalTemp);

	/**
	 * Returns <code>true</code> if the manager is running correctly,
	 * <code>false</code> otherwise.
	 */
	boolean isActive();

	/** Prepares this manager to be run. Always invoked before {@link #run()}. */
	void init(String vcsloc, IModelIndexer hawk) throws Exception;

	/**
	 * Starts this manager, after it has been initialised with
	 * {@link #init(String, IModelIndexer)}.
	 */
	void run() throws Exception;

	/** Shuts down this manager. */
	void shutdown();

	/**
	 * 
	 * @return returns the canonical and normalised representation of the location
	 *         of this VCSManager, always including a trailing slash
	 */
	String getLocation();

	/**
	 * Returns the current username (if any is used), or <code>null</code>.
	 */
	String getUsername();

	/**
	 * Returns the current password (if any is used), or <code>null</code>.
	 */
	String getPassword();

	/**
	 * Changes the username and password in one go. Both must be passed at the same
	 * time to be able to support remote instances.
	 */
	void setCredentials(String username, String password, ICredentialsStore credStore);

	/**
	 * Returns <code>true</code> if the implementation supports authentication.
	 */
	boolean isAuthSupported();

	/**
	 * Returns <code>true</code> if the implementation accepts filesystem paths as
	 * locations. It should be OK for an implementation to return <code>true</code>
	 * for this and {@link #isURLLocationAccepted()} at the same time.
	 */
	boolean isPathLocationAccepted();

	/**
	 * Returns <code>true</code> if the implementation accepts URL-based paths as
	 * locations. It should be OK for an implementation to return <code>true</code>
	 * for this and {@link #isPathLocationAccepted()} at the same time.
	 */
	boolean isURLLocationAccepted();

	/**
	 * Maps the raw URI given by the {@link IHawkObject} to a relative path within
	 * the repository.
	 */
	String getRepositoryPath(String rawPath);

	/**
	 * Returns <code>true</code> if the repository should be "frozen", ignoring any
	 * changes on the contained files until thawed.
	 */
	boolean isFrozen();

	/**
	 * Changes whether the location is "frozen", ignoring any changes on the
	 * contained files until thawed.
	 */
	void setFrozen(boolean frozen);

	@Override
	default Category getCategory() {
		return Category.VCS_MANAGER;
	}

}
