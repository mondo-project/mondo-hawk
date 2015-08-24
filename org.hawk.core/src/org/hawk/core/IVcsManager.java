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

public interface IVcsManager {

	public String getCurrentRevision(VcsRepository repository) throws Exception;

	public String getFirstRevision(VcsRepository repository) throws Exception;

	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision) throws Exception;

	public VcsRepositoryDelta getDelta(VcsRepository repository,
			String startRevision, String endRevision) throws Exception;

	// kostas
	public abstract void importFiles(String path, File temp);

	// kostas
	public abstract boolean isActive();

	// kostas
	void run(String vcsloc, String un, String pw, IAbstractConsole c)
			throws Exception;

	// kostas
	public void shutdown();

	// kostas
	public abstract String getLocation();

	// kostas
	public String getUn();

	// kostas
	public String getPw();

	// kostas
	public abstract String getType();

	// kostas
	public abstract String getHumanReadableName();

	// kostas
	public String getCurrentRevision() throws Exception;

	// kostas
	public List<VcsCommitItem> getDelta(String string) throws Exception;

	/**
	 * Returns <code>true</code> if the implementation supports authentication.
	 */
	public boolean isAuthSupported();

	/**
	 * Returns <code>true</code> if the implementation accepts filesystem paths
	 * as locations. It should be OK for an implementation to return
	 * <code>true</code> for this and {@link #isURLLocationAccepted()} at the
	 * same time.
	 */
	public boolean isPathLocationAccepted();

	/**
	 * Returns <code>true</code> if the implementation accepts URL-based paths
	 * as locations. It should be OK for an implementation to return
	 * <code>true</code> for this and {@link #isPathLocationAccepted()} at the
	 * same time.
	 */
	public boolean isURLLocationAccepted();
}
