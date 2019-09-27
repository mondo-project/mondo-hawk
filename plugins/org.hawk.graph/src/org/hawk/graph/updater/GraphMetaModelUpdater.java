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
package org.hawk.graph.updater;

import java.util.Set;

import org.hawk.core.FailedMetamodelRegistrationException;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.runtime.CompositeGraphChangeListener;

public class GraphMetaModelUpdater implements IMetaModelUpdater {

	@Override
	public void insertMetamodels(Set<IHawkMetaModelResource> set, IModelIndexer indexer)
			throws FailedMetamodelRegistrationException
	{
		try {
			new GraphMetaModelResourceInjector(indexer, set,
				(CompositeGraphChangeListener) indexer
						.getCompositeGraphChangeListener());
		} catch (FailedMetamodelRegistrationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new FailedMetamodelRegistrationException(ex);
		}
	}

	@Override
	public Set<String> removeMetamodels(IModelIndexer indexer, String[] mmuri) {
		GraphMetaModelResourceInjector ret = new GraphMetaModelResourceInjector(
				indexer,
				(CompositeGraphChangeListener) indexer
						.getCompositeGraphChangeListener());
		return ret.removeMetamodels(mmuri);
	}

	@Override
	public boolean addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic, IModelIndexer indexer) {

		return GraphMetaModelResourceInjector.addDerivedAttribute(metamodeluri,
				typename, attributename, isMany, isOrdered, isUnique,
				attributetype, derivationlanguage, derivationlogic,
				indexer.getGraph(), indexer.getCompositeGraphChangeListener());
	}

	@Override
	public void run() {
		// this.console = console;
	}

	@Override
	public boolean addIndexedAttribute(String metamodeluri, String typename,
			String attributename, IModelIndexer indexer) {
		return GraphMetaModelResourceInjector.addIndexedAttribute(metamodeluri,
				typename, attributename, indexer.getGraph(),
				indexer.getCompositeGraphChangeListener());
	}

	@Override
	public String getName() {
		return "Default Hawk GraphMetaModelUpdater (v1.0)";
	}

	@Override
	public boolean removeIndexedAttribute(String metamodelUri, String typename,
			String attributename, IModelIndexer indexer) {
		return GraphMetaModelResourceInjector.removeIndexedAttribute(
				metamodelUri, typename, attributename, indexer.getGraph(),
				indexer.getCompositeGraphChangeListener());
	}

	@Override
	public boolean removeDerivedAttribute(String metamodelUri, String typeName,
			String attributeName, IModelIndexer indexer) {
		return GraphMetaModelResourceInjector.removeDerivedAttribute(
				metamodelUri, typeName, attributeName, indexer.getGraph(),
				indexer.getCompositeGraphChangeListener());
	}
}
