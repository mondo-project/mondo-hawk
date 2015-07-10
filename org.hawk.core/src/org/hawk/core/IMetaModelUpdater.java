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

import java.util.Set;

import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.model.IHawkMetaModelResource;

public interface IMetaModelUpdater {

	public abstract IGraphChangeDescriptor insertMetamodels(
			Set<IHawkMetaModelResource> set, IModelIndexer indexer);

	public abstract void run(// IAbstractConsole console
	);

	boolean addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic, IModelIndexer indexer);

	boolean addIndexedAttribute(String metamodeluri, String typename,
			String attributename, IModelIndexer indexer);

	public abstract IGraphChangeDescriptor removeMetamodels(
			Set<IHawkMetaModelResource> set, IModelIndexer indexer);

	public abstract String getName();

}
