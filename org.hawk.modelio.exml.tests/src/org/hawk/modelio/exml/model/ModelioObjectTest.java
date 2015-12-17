package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlParser;
import org.junit.Test;

public class ModelioObjectTest {

	private static final String FRAGMENT_PATH = "resources/Zoo/data/fragments/";
	private static final String CLASSDIAG_EXML = FRAGMENT_PATH + "Zoo/model/ClassDiagram/cf6a3b18-94f9-49ba-b8d9-653cb2f93cfb.exml";

	@Test
	public void accessClassDiagram() throws Exception {
		final File f = new File(CLASSDIAG_EXML);
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(f, fIS);

			final ModelioMetaModelResource metamodel = new ModelioMetaModelResource(null);
			final ModelioClass mC = metamodel.getModelioClass(object.getMClassName());
			final ModelioObject mO = new ModelioObject(mC, object);

			final Map<String, ModelioAttribute> attrs = mC.getAttributes();
			assertTrue(((String)mO.get(attrs.get("UiData"))).startsWith("eJztXWtz27"));
			assertEquals(88, mO.get(attrs.get("UiDataVersion")));
		}
	}
}
