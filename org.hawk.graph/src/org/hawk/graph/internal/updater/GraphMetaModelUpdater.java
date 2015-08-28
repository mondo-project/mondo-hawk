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
package org.hawk.graph.internal.updater;

import java.util.Set;

import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.graph.listener.CompositeGraphChangeListener;
import org.hawk.graph.listener.IGraphChangeListener;

public class GraphMetaModelUpdater implements IMetaModelUpdater {

	private static IGraphChangeListener listener = new CompositeGraphChangeListener();

	@Override
	public IGraphChangeDescriptor insertMetamodels(Set<IHawkMetaModelResource> set, IModelIndexer indexer) {
		// TODO: do we really need IGraphChangeDescriptors now?
		new GraphMetaModelResourceInjector(indexer.getGraph(), set, listener);

		GraphChangeDescriptorImpl desc = new GraphChangeDescriptorImpl("Default Hawk GraphMetaModelUpdater");
		desc.setErrorState(false);
		desc.setUnresolvedDerivedProperties(-1);
		desc.setUnresolvedReferences(-1);

		return desc;
	}

	@Override
	public IGraphChangeDescriptor removeMetamodels(Set<IHawkMetaModelResource> set, IModelIndexer indexer) {
		// TODO: do we really need IGraphChangeDescriptors now?
		GraphMetaModelResourceInjector ret = new GraphMetaModelResourceInjector(indexer.getGraph(), listener);
		ret.removeMetamodels(set);

		GraphChangeDescriptorImpl desc = new GraphChangeDescriptorImpl(
				"Default Hawk GraphMetaModelUpdater");
		desc.setErrorState(false);
		desc.setUnresolvedDerivedProperties(-1);
		desc.setUnresolvedReferences(-1);

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
				indexer.getGraph(), listener);
	}

	@Override
	public void run() {
		// this.console = console;
	}

	@Override
	public boolean addIndexedAttribute(String metamodeluri, String typename,
			String attributename, IModelIndexer indexer) {

		return GraphMetaModelResourceInjector.addIndexedAttribute(metamodeluri,
				typename, attributename, indexer.getGraph(), listener);

	}

	@Override
	public String getName() {
		return "Default Hawk GraphMetaModelUpdater (v1.0)";
	}

	public static IGraphChangeListener getListener() {
		return listener;
	}

	public static void setListener(IGraphChangeListener listener) {
		GraphMetaModelUpdater.listener = listener;
	}

}
