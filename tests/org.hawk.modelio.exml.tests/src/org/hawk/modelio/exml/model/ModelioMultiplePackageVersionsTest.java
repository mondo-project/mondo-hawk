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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.hawk.core.IFileImporter;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioAttribute;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlParser;
import org.hawk.modelio.model.util.RegisterMeta;
import org.junit.Before;
import org.junit.Test;

public class ModelioMultiplePackageVersionsTest {
	private final String FRAGMENT_PATH = "resources/Zoo/data/fragments/";
	private final String CLASS_EXML = FRAGMENT_PATH + "Zoo/model/Class/0a4ac84f-75a3-4b5b-bbad-d0e67857b4cf.exml";

	private final String METAMODEL_PATH = "resources/metamodel/";
	private final static String MMVERSION_PATH = "/admin/mmversion.dat";

	protected static final class DummyFileImporter1 implements IFileImporter {
		@Override
		public File importFile(String path) {
			return new File("resources/mmversionFiles/mmversion_1", MMVERSION_PATH);
		}
	}

	protected static final class DummyFileImporter2 implements IFileImporter {
		@Override
		public File importFile(String path) {
			return new File("resources/mmversionFiles/mmversion_2", MMVERSION_PATH);
		}
	}

	protected static final class DummyFileImporter3 implements IFileImporter {
		@Override
		public File importFile(String path) {
			return new File("resources/mmversionFiles/mmversion_3", MMVERSION_PATH);
		}
	}
	
	
	protected static final class DummyFileImporter4 implements IFileImporter {
		@Override
		public File importFile(String path) {
			return new File("resources/mmversionFiles/mmversion_4", MMVERSION_PATH);
		}
	}

	@Before
	public void setup() {
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");
		File file2 = new File( METAMODEL_PATH + "metamodel_descriptor_2.xml");

		try {
			ModelioMetaModelResourceFactory factory;
			factory = new ModelioMetaModelResourceFactory();

			/*	metamodel with Standard 2.0.00*/
			factory.parse(file);


			factory = new ModelioMetaModelResourceFactory();
			/*	metamodel with Standard 1.0.00*/
			factory.parse(file2);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void TestPackageVersion1() throws Exception {
		/* mmversion.dat == modelio.kernel 1.0.00 ,  Standard 2.0.00*/
		checkVersion(
				"Required version is  2.0.00, expected to use  2.0.00",
				"2.0.00",  new DummyFileImporter1());
	}

	@Test
	public void TestPackageVersion2() throws Exception {
		/* mmversion.dat == modelio.kernel 0.1.00, Standard 1.0.00 */
		checkVersion(
				"Required version is 1.0.00, expected to use 1.0.00",
				"1.0.00",  new DummyFileImporter2());
	}

	@Test
	public void TestPackageVersion3() throws Exception {
		/* mmversion.dat == modelio.kernel 4.0.00, Standard 4.0.00 */
		checkVersion(
				"Required version is 4.0.00 (Not Available), expected to use Latest (2.0.00)",
				"2.0.00",  new DummyFileImporter3());
	}

	
	
	@Test
	public void TestPackageVersion4() throws Exception {
		/* mmversion.dat not present */
		checkVersion(
				"Required version is None, expected to use Latest (2.0.00)",
				"2.0.00",  new DummyFileImporter4());
	}
	public void checkVersion(String msg, String expectedVersion, IFileImporter importer) throws Exception {

		final ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(
				importer,
				new File(CLASS_EXML));

		for( IHawkObject obj : resource.getAllContents()) {
			final ModelioClass mC = ((ModelioObject) obj).getType();
			String version = mC.getPackage().getVersion();
			// assert Version
			System.out.println("## Test :  " +  msg);
			assertEquals(msg, expectedVersion, version);
		}

	}

}

