/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.bpmn;

import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.bpmn.model.BPMNModelResourceFactory;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.integration.tests.ModelIndexingTest.IModelSupportFactory;

/**
 * Factory for the instances needed to parse EMF file-based models.
 */
public class BPMNModelSupportFactory implements IModelSupportFactory {
	@Override
	public IModelResourceFactory createModelResourceFactory() {
		return new BPMNModelResourceFactory();
	}

	@Override
	public IMetaModelResourceFactory createMetaModelResourceFactory() {
		return new BPMNMetaModelResourceFactory();
	}
}