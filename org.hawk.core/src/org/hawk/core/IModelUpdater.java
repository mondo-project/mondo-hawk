/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import java.util.Map;

import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.model.IHawkModelResource;

/**
 * 
 * @author kb
 * 
 */
public interface IModelUpdater {

	public boolean caresAboutResources();

	/**
	 * Updates the database according to changed files
	 * 
	 * @param VcsCommitItemtoResourceMap
	 * @return
	 */
	public abstract IGraphChangeDescriptor updateStore(
			Map<VcsCommitItem, IHawkModelResource> VcsCommitItemtoResourceMap);

	void run(IAbstractConsole console, IModelIndexer modelIndexer)
			throws Exception;

	public abstract void shutdown();

	public void deleteAll(String filepath) throws Exception;

	public void updateDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic);

	public void updateIndexedAttribute(String metamodeluri, String typename,
			String attributename);

	public String getName();

}