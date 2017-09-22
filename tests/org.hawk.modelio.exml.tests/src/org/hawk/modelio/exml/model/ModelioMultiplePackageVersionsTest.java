/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - 
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.hawk.core.IFileImporter;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ModelioMultiplePackageVersionsTest {
	private final String FRAGMENT_PATH = "resources/Zoo/data/fragments/";
	private final String CLASS34_EXML = FRAGMENT_PATH + "Zoo/model/Class/0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf.exml";

	private final String METAMODEL_PATH = "resources/metamodel/";
	private final static String MMVERSION_PATH = "/admin/mmversion.dat";

	protected static final class DummyFileImporter implements IFileImporter {
		private final String basePath;

		public DummyFileImporter(String basePath) {
			this.basePath = basePath;
		}

		@Override
		public File importFile(String path) {
			return new File(basePath, MMVERSION_PATH);
		}
	}

	@Before
	public void setup() {
		File file = new File(METAMODEL_PATH, "metamodel_descriptor.xml");
		File file2 = new File(METAMODEL_PATH, "metamodel_descriptor_2.xml");

		try {
			ModelioMetaModelResourceFactory factory;
			factory = new ModelioMetaModelResourceFactory();

			/* metamodel with Standard 2.0.00 */
			factory.parse(file);

			factory = new ModelioMetaModelResourceFactory();
			/* metamodel with Standard 1.0.00 */
			factory.parse(file2);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPackageVersion1() throws Exception {
		/* mmversion.dat == modelio.kernel 1.0.00 , Standard 2.0.00 */
		checkVersion("2.0.00",
				new DummyFileImporter("resources/mmversionFiles/mmversion_1"));
	}

	@Test
	public void testPackageVersion2() throws Exception {
		/* mmversion.dat == modelio.kernel 0.1.00, Standard 1.0.00 */
		checkVersion("1.0.00",
				new DummyFileImporter("resources/mmversionFiles/mmversion_2"));
	}

	@Test
	public void testPackageVersion3() throws Exception {
		/* mmversion.dat == modelio.kernel 4.0.00, Standard 4.0.00 */
		checkVersion("2.0.00",
				new DummyFileImporter("resources/mmversionFiles/mmversion_3"));
	}

	@Ignore
	@Test
	public void testPackageVersion4() throws Exception {
		/* mmversion.dat not present - ambiguous at the moment, not sure it's useful to test against - need to check with Softeam. */
		checkVersion("2.0.00",
				new DummyFileImporter("resources/mmversionFiles/mmversion_4"));
	}

	public void checkVersion(String expectedVersion, IFileImporter importer) throws Exception {
		final ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(importer, new File(CLASS34_EXML));

		for (IHawkObject obj : resource.getAllContents()) {
			final ModelioClass mC = ((ModelioObject) obj).getType();
			String version = mC.getPackage().getVersion();
			assertEquals("Package version for " + mC.getPackage().getName() + "::" + mC.getName() + " should be the expected one", expectedVersion, version);
		}
	}

}
