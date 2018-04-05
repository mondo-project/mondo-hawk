/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.manifests;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;
import org.hawk.manifest.metamodel.ManifestMetaModelResourceFactory;
import org.hawk.manifest.model.ManifestModelResourceFactory;

/**
 * Factory for the instances needed to parse Eclipse MANIFEST.MF files.
 */
public class ManifestModelSupportFactory implements IModelSupportFactory {
	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new ManifestModelResourceFactory();
	}

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new ManifestMetaModelResourceFactory();
	}
}