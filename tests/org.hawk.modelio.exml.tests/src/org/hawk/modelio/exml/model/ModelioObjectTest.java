/*******************************************************************************
 * Copyright (c) 2017 Aston University
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
 *     Orjuwan Al-Wadeai - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.hawk.core.model.IHawkReference;
import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.exml.metamodel.register.MetamodelRegister;
import org.hawk.modelio.exml.model.parser.ExmlObject;
import org.hawk.modelio.exml.model.parser.ExmlParser;
import org.junit.Before;
import org.junit.Test;

public class ModelioObjectTest {
	private static final String FRAGMENT34_PATH = "resources/Zoo/data/fragments/";
	private static final String FRAGMENT35_PATH = "resources/Zoo35/data/fragments/";
	private static final String CLASSDIAG_EXML = FRAGMENT34_PATH + "Zoo/model/ClassDiagram/cf6a3b18-94f9-49ba-b8d9-653cb2f93cfb.exml";
	private static final String AREA_CLASS34_EXML = FRAGMENT34_PATH + "Zoo/model/Class/0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf.exml";
	private static final String AREA_CLASS35_EXML = FRAGMENT35_PATH + "Zoo/model/Class/09864fe3-abc6-4de6-89c3-dd84c76ea535.exml";

	private static final String METAMODEL_PATH = "resources/metamodel/";

	@Before
	public void setup() {
		File file = new File(METAMODEL_PATH + "metamodel_descriptor.xml");
		try {
			ModelioMetaModelResourceFactory factory;
			factory = new ModelioMetaModelResourceFactory();
			factory.parse(file);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void accessClassDiagram() throws Exception {
		final ModelioObject mO = parse(new File(CLASSDIAG_EXML));

		final ModelioClass mC = mO.getType();
		final Map<String, ModelioAttribute> attrs = mC.getAllAttributesMap();
		assertTrue(((String) mO.get(attrs.get("UiData"))).startsWith("eJztXWtz27"));
		assertEquals(88, mO.get(attrs.get("UiDataVersion")));

		ModelioProxy value = (ModelioProxy) mO.get((ModelioReference) mC.getStructuralFeature(ModelioClass.REF_PARENT),	false);
		assertNotNull("Parent should be a ModelioProxy", value);
		assertEquals("Parent ModelioProxy should point to the right object", "ea878bd2-7ef9-4ce1-a11e-35fa129981bb", value.getUriFragment());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void accessClassModelio34() throws Exception {
		final ModelioObject mO = parse(new File(AREA_CLASS34_EXML));

		final ModelioClass mC = mO.getType();
		final Map<String, ModelioAttribute> attrs = mC.getAllAttributesMap();
		assertEquals("Area", (String) mO.get(attrs.get("Name")));
		assertEquals("Standard", mC.getPackage().getName());

		IHawkReference refOwnedEnd = (IHawkReference) mC.getStructuralFeature("OwnedEnd");
		ModelioObject mOwnedEnd = ((List<ModelioObject>) mO.get(refOwnedEnd, false)).get(0);
		ModelioClass cAssociationEnd = mOwnedEnd.getType();
		IHawkReference refAssociation = (IHawkReference)cAssociationEnd.getStructuralFeature("Association");

		ModelioProxy mAssociation = (ModelioProxy) mOwnedEnd.get(refAssociation, false);
		assertEquals("Standard", mAssociation.getType().getPackage().getName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void accessClassModelio35() throws Exception {
		final ModelioObject mO = parse(new File(AREA_CLASS35_EXML));

		final ModelioClass mC = mO.getType();
		final Map<String, ModelioAttribute> attrs = mC.getAllAttributesMap();
		assertEquals((String) mO.get(attrs.get("Name")), "Area");

		IHawkReference refOwnedEnd = (IHawkReference) mC.getStructuralFeature("OwnedEnd");
		ModelioObject mOwnedEnd = ((List<ModelioObject>) mO.get(refOwnedEnd, false)).get(0);
		ModelioClass cAssociationEnd = mOwnedEnd.getType();
		IHawkReference refAssociation = (IHawkReference)cAssociationEnd.getStructuralFeature("Association");

		ModelioProxy mAssociation = (ModelioProxy) mOwnedEnd.get(refAssociation, false);
		assertEquals("Standard", mAssociation.getType().getPackage().getName());
	}

	protected ModelioObject parse(File f) throws Exception {
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);

			final ModelioClass mC = MetamodelRegister.INSTANCE.getModelioClass(object.getMClassName(), null);
			final ModelioObject mO = new ModelioObject(mC, object, null);

			return mO;
		}
	}

}
