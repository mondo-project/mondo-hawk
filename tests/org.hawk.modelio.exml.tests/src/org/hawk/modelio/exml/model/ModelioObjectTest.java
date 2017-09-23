package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

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

	@Test
	public void accessClassModelio35() throws Exception {
		final ModelioObject mO = parse(new File(AREA_CLASS35_EXML));

		final ModelioClass mC = mO.getType();
		final Map<String, ModelioAttribute> attrs = mC.getAllAttributesMap();
		assertEquals((String) mO.get(attrs.get("Name")), "Area");
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
