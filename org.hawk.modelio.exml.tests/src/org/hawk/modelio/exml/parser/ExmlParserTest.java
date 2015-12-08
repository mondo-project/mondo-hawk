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
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

/**
 * Tests for the {@link ExmlParser} class.
 */
public class ExmlParserTest {

	private static final String FRAGMENT_PATH = "resources/Zoo/data/fragments/";
	private static final String CLASS_PATH = FRAGMENT_PATH + "Zoo/model/Class/";
	private static final String AREA_CLASS_EXML = CLASS_PATH + "0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf.exml";

	@Test
	public void parseClass() throws Exception {
		try (final FileInputStream fIS = new FileInputStream(new File(AREA_CLASS_EXML))) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(fIS);

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

	/**
	 * For an XML document without Modelio objects, the parser will throw a {@link NoSuchElementException}.
	 */
	@Test(expected=NoSuchElementException.class)
	public void parseNoObjects() throws Exception {
		final ExmlParser parser = new ExmlParser();
		assertNull(parser.getObject(new ByteArrayInputStream("<nothing/>".getBytes())));
	}

	/**
	 * A fully empty stream is not a valid XML document.
	 */
	@Test(expected=XMLStreamException.class)
	public void parseEmpty() throws Exception {
		final ExmlParser parser = new ExmlParser();
		assertNull(parser.getObject(new ByteArrayInputStream(new byte[0])));
	}
}
