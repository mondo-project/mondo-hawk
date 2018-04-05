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
package org.hawk.integration.tests.emf;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;

/**
 * Factory for the instances needed to parse EMF file-based models.
 */
public class EMFModelSupportFactory implements IModelSupportFactory {
	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new EMFModelResourceFactory();
	}

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new EMFMetaModelResourceFactory();
	}
}