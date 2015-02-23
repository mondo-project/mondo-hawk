/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.updater;

import java.util.Set;

import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.model.IHawkMetaModelResource;

public class GraphMetaModelUpdater implements IMetaModelUpdater {

	// private IAbstractConsole console;

	@Override
	public IGraphChangeDescriptor insertMetamodels(
			Set<IHawkMetaModelResource> set, IModelIndexer indexer) {

		//
		GraphMetaModelResourceInjector ret = new GraphMetaModelResourceInjector(
				indexer.getGraph(), set);

		GraphChangeDescriptorImpl desc = new GraphChangeDescriptorImpl(
				"Default Hawk GraphMetaModelUpdater");
		desc.setErrorState(false);
		desc.setUnresolvedDerivedProperties(-1);
		desc.setUnresolvedReferences(-1);
		desc.addChanges(ret.getChanges());
		ret.clearChanges();

		return desc;

	}

	@Override
	public IGraphChangeDescriptor removeMetamodels(
			Set<IHawkMetaModelResource> set, IModelIndexer indexer) {
		//
		GraphMetaModelResourceInjector ret = new GraphMetaModelResourceInjector(
				indexer.getGraph());
		ret.removeMetamodels(set);

		GraphChangeDescriptorImpl desc = new GraphChangeDescriptorImpl(
				"Default Hawk GraphMetaModelUpdater");
		desc.setErrorState(false);
		desc.setUnresolvedDerivedProperties(-1);
		desc.setUnresolvedReferences(-1);
		desc.addChanges(ret.getChanges());
		ret.clearChanges();

		return desc;

	}

	@Override
	public boolean addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic, IModelIndexer indexer) {

		return GraphMetaModelResourceInjector.addDerivedAttribute(metamodeluri,
				typename, attributename, isMany, isOrdered, isUnique,
				attributetype, derivationlanguage, derivationlogic,
				indexer.getGraph());

	}

	@Override
	public void run(// IAbstractConsole console
	) {
		// this.console = console;

	}

	@Override
	public boolean addIndexedAttribute(String metamodeluri, String typename,
			String attributename, IModelIndexer indexer) {

		return GraphMetaModelResourceInjector.addIndexedAttribute(metamodeluri,
				typename, attributename, indexer.getGraph());

	}

}
