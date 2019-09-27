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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - minor cleanup
 ******************************************************************************/
package org.hawk.core;

import java.util.Set;

import org.hawk.core.model.IHawkMetaModelResource;

public interface IMetaModelUpdater extends IHawkPlugin {

	/**
	 * @return <code>true</code> if insertion was successful, <code>false</code> otherwise.
	 */
	void insertMetamodels(Set<IHawkMetaModelResource> set,
			IModelIndexer indexer) throws FailedMetamodelRegistrationException;

	void run();

	boolean addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic, IModelIndexer indexer);

	boolean addIndexedAttribute(String metamodeluri, String typename,
			String attributename, IModelIndexer indexer);

	@Deprecated
	String getName();

	@Override
	default String getHumanReadableName() {
		return getName();
	}

	Set<String> removeMetamodels(IModelIndexer indexer,
			String[] mmuris);

	boolean removeIndexedAttribute(String metamodelUri,
			String typename, String attributename,
			IModelIndexer modelIndexerImpl);

	boolean removeDerivedAttribute(String metamodelUri,
			String typeName, String attributeName,
			IModelIndexer modelIndexerImpl);

	@Override
	default Category getCategory() {
		return Category.METAMODEL_UPDATER;
	}
	
}
