/*******************************************************************************
 * Copyright (c) 2017 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.uml;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;
import org.hawk.uml.metamodel.UMLMetaModelResourceFactory;
import org.hawk.uml.model.UMLModelResourceFactory;

public class UMLModelSupportFactory implements IModelSupportFactory {

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new UMLMetaModelResourceFactory();
	}

	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new UMLModelResourceFactory();
	}

}
