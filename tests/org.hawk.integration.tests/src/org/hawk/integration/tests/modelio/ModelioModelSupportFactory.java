/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.modelio;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.model.ModelioModelResourceFactory;

public class ModelioModelSupportFactory implements IModelSupportFactory {

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new ModelioMetaModelResourceFactory();
	}

	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new ModelioModelResourceFactory();
	}

}
