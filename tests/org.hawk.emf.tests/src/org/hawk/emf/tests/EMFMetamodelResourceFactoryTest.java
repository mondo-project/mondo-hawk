/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.emf.tests;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.junit.Test;

/**
 * Tests for the {@link EMFMetaModelResourceFactory} class.
 */
public class EMFMetamodelResourceFactoryTest {

	@Test
	public void separateJDTAST() throws Exception {
		EMFMetaModelResourceFactory mmf = new EMFMetaModelResourceFactory();
		IHawkMetaModelResource resource = mmf.parse(new File("resources/JDTAST.ecore"));

		for (IHawkObject obj : resource.getAllContents()) {
			if (obj instanceof IHawkPackage) {
				IHawkPackage pkg = (IHawkPackage)obj;
				String sContent = mmf.dumpPackageToString(pkg);
				assertFalse("Should not contain references to original file", sContent.contains("JDTAST.ecore"));
				assertFalse("Should not contain references to itself as external: " + pkg.getNsURI(), sContent.contains("resource_from_epackage_" + pkg.getNsURI()));
			}
		}
	}
}
