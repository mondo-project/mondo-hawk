/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;

import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.model.ModelioModelResource;
import org.hawk.modelio.exml.model.ModelioModelResourceFactory;
import org.hawk.modelio.exml.model.parser.ExmlObject;
import org.hawk.modelio.exml.model.parser.ExmlParser;
import org.hawk.modelio.exml.model.parser.ExmlReference;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link ExmlParser} class.
 */
public class ExmlParserTest {

	private static final String FRAGMENT_PATH = "resources/Zoo/data/fragments/";
	private static final String CLASS_PATH = FRAGMENT_PATH + "Zoo/model/Class/";
	private static final String AREA_CLASS_EXML = CLASS_PATH + "0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf.exml";
	private static final String AREA_CLASS35_EXML = "resources/Zoo35/data/fragments/Zoo/model/Class/09864fe3-abc6-4de6-89c3-dd84c76ea535.exml";
	private static final String ANIMAL_CLASS_EXML = CLASS_PATH + "4ed7f59f-f723-4f88-b6fc-ea6b83eb3108.exml";
	private static final String ELEPHANT_CLASS_EXML = CLASS_PATH + "2d7b2cba-e694-4b33-bd9e-4d2f1db4cc7b.exml";
	private static final String PACKAGE_CLASS_EXML = FRAGMENT_PATH + "Zoo/model/Package/ea878bd2-7ef9-4ce1-a11e-35fa129981bb.exml";

	private final String METAMODEL_PATH = "resources/metamodel/";

	@Before
	public void setup() throws Exception {
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");
		ModelioMetaModelResourceFactory factory;
		factory = new ModelioMetaModelResourceFactory();
		factory.parse(file);
	}
	
	@Test
	public void parseClass() throws Exception {
		final File f = new File(AREA_CLASS_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);

			assertEquals("Area", object.getName());
			assertEquals("Class", object.getMClassName());
			assertEquals("0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf", object.getUID());
			assertEquals("zoo", object.getParentName());
			assertEquals("Package", object.getParentMClassName());
			assertEquals("ea878bd2-7ef9-4ce1-a11e-35fa129981bb", object.getParentUID());
			assertEquals("1970354901745664", object.getAttribute("status"));

			final List<ExmlReference> ownedAttributes = object.getCompositions().get("OwnedAttribute");
			assertEquals(1, ownedAttributes.size());
			final ExmlObject ownedAttribute = (ExmlObject)ownedAttributes.get(0);
			assertEquals("2209c0c9-6ec9-4525-b547-632ab8f662c3", ownedAttribute.getUID());
			assertEquals("name", ownedAttribute.getAttribute("Name"));
			assertEquals("00000004-0000-000d-0000-000000000000", ownedAttribute.getLinks().get("Type").get(0).getUID());

			final ExmlObject ownedEnd = (ExmlObject)object.getCompositions().get("OwnedEnd").get(0);
			final List<ExmlReference> ownedEndAssociation = ownedEnd.getCompositions().get("Association");
			assertEquals("263b2747-a54c-49e6-9b9d-ee3a5968766a", ownedEndAssociation.get(0).getUID());
		}
	}

	@Test
	public void parseClass35() throws Exception {
		final File f = new File(AREA_CLASS35_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);

			assertEquals("Area", object.getName());
			assertEquals("Standard.Class", object.getMClassName());
			assertEquals("zoo", object.getParentName());
			assertEquals("Standard.Package", object.getParentMClassName());
		}
	}

	@Test
	public void parseAnimal() throws Exception {
		final File f = new File(ANIMAL_CLASS_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);

			assertEquals("Animal", object.getName());
			assertEquals("Class", object.getMClassName());
			assertEquals("4ed7f59f-f723-4f88-b6fc-ea6b83eb3108", object.getUID());
		}
	}

	@Test
	public void parseElephant() throws Exception {
		final File f = new File(ELEPHANT_CLASS_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);
			assertEquals("Elephant", object.getName());

			final List<ExmlReference> parent = object.getCompositions().get("Parent");
			assertNotNull(parent);
			
			final ModelioModelResource elephantModel = new ModelioModelResource(object, new ModelioModelResourceFactory());
			assertEquals(3, elephantModel.getAllContentsSet().size());
		}
	}

	@Test
	public void parsePackage() throws Exception {
		final File f = new File(PACKAGE_CLASS_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);
			assertEquals("zoo", object.getName());
			assertEquals(6, object.getCompositions().get("OwnedElement").size());

			final List<ExmlReference> products = object.getCompositions().get("Product");
			assertEquals(1, products.size());
			assertEquals("cf6a3b18-94f9-49ba-b8d9-653cb2f93cfb", products.get(0).getUID());
		}
	}

	/**
	 * For an XML document without Modelio objects, the parser will throw a {@link NoSuchElementException}.
	 */
	@Test(expected=NoSuchElementException.class)
	public void parseNoObjects() throws Exception {
		final ExmlParser parser = new ExmlParser();
		assertNull(parser.getObject(null, new ByteArrayInputStream("<nothing/>".getBytes())));
	}

	/**
	 * A fully empty stream is not a valid XML document.
	 */
	@Test(expected=XMLStreamException.class)
	public void parseEmpty() throws Exception {
		final ExmlParser parser = new ExmlParser();
		assertNull(parser.getObject(null, new ByteArrayInputStream(new byte[0])));
	}
}
