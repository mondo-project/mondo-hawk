/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import java.util.Set;

import org.hawk.core.model.IHawkModelResource;

/**
 * 
 * @author kb
 * 
 */
public interface IModelUpdater extends IHawkPlugin {

	public boolean caresAboutResources();

	/**
	 * Updates the database according to changed files
	 * 
	 * @param VcsCommitItemtoResourceMap
	 * @return
	 */
	public abstract boolean updateStore(VcsCommitItem vcscommititem,
			IHawkModelResource resource);

	void run(IConsole console, IModelIndexer modelIndexer) throws Exception;

	public abstract void shutdown();

	public boolean deleteAll(VcsCommitItem c) throws Exception;

	public void updateDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic);

	public void updateIndexedAttribute(String metamodeluri, String typename,
			String attributename);

	@Override
	default String getHumanReadableName() {
		return getName();
	}

	@Deprecated
	public String getName();

	public Set<VcsCommitItem> compareWithLocalFiles(
			Set<VcsCommitItem> interestingfiles);

	void updateProxies();

	public boolean deleteAll(IVcsManager vcs) throws Exception;

}