/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Nikolas Matragkas, James Williams, Dimitris Kolovos - initial API and implementation
 *     Konstantinos Barmpis - adaption for use in Hawk
 ******************************************************************************/
package org.hawk.core;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface IVcsManager {

	String getCurrentRevision() throws Exception;

	String getFirstRevision() throws Exception;

	List<VcsCommitItem> getDelta(String startRevision) throws Exception;

	VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception;

	// kostas
	void importFiles(String path, File temp);

	// kostas
	boolean isActive();

	// kostas
	void run(String vcsloc, IConsole c, IModelIndexer indexer) throws Exception;

	// kostas
	void shutdown();

	// kostas
	String getLocation();

	/**
	 * Changes the username and password in one go. Both must be passed at the
	 * same time to be able to support remote instances.
	 */
	void setCredentials(String username, String password);

	// kostas
	String getType();

	// kostas
	String getHumanReadableName();

	/**
	 * Returns <code>true</code> if the implementation supports authentication.
	 */
	boolean isAuthSupported();

	/**
	 * Returns <code>true</code> if the implementation accepts filesystem paths
	 * as locations. It should be OK for an implementation to return
	 * <code>true</code> for this and {@link #isURLLocationAccepted()} at the
	 * same time.
	 */
	boolean isPathLocationAccepted();

	/**
	 * Returns <code>true</code> if the implementation accepts URL-based paths
	 * as locations. It should be OK for an implementation to return
	 * <code>true</code> for this and {@link #isPathLocationAccepted()} at the
	 * same time.
	 */
	boolean isURLLocationAccepted();

	/**
	 * Returns a set of prefixes that should be stripped from any inter-resource
	 * references in the files contained within that repository in order to turn
	 * them into relative paths within the repository.
	 */
	Set<String> getPrefixesToBeStripped();
}
