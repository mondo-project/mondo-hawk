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
package org.hawk.modelio.exml.metamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;
import org.hawk.modelio.model.util.RegisterMeta;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the {@link ModelioMetaModelResource} class.
 */
public class ModelioMetaModelResourceTest {

	private static ModelioMetaModelResource r;
	static ModelioMetaModelResourceFactory factory;

	private static final String METAMODEL_PATH = "resources/metamodel/";

	@BeforeClass
	public static void setup() {
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");
		try {
			factory = new ModelioMetaModelResourceFactory();
			
			r = (ModelioMetaModelResource) factory.parse(file);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void countPackages() {
		List<IHawkPackage> rootPackages = new ArrayList<>();
		for (IHawkObject o : r.getAllContents()) {
			if (o instanceof IHawkPackage && ((IHawkPackage)o).isRoot()) {
				rootPackages.add((IHawkPackage)o);
			}
		}
		assertEquals("There should be 1 IHawkPackage per root MPackage + 1 meta package", 6, rootPackages.size());
	}

	@Test
	public void allClassesHaveParentRef() {
		for (IHawkObject o : r.getAllContents()) {
			if (o instanceof IHawkClass) {
				IHawkClass hawkClass = (IHawkClass)o;

				boolean bHasPIDReference = false;
				for (IHawkReference ref : hawkClass.getAllReferences()) {
					if (ref.getName().equals(ModelioClass.REF_PARENT)) {
						assertTrue("OID-PID reference should be a container reference", ref.isContainer());
						assertFalse("OID-PID reference should not be a containment reference", ref.isContainment());
						bHasPIDReference = true;
					} else {
						assertFalse("Other references should not be container nor containment", ref.isContainer() || ref.isContainment());
					}
				}
				assertTrue("Class " + hawkClass.getName() + " should have an OID-PID reference", bHasPIDReference);
			}
		}
	}

	@Test
	public void expectedRootMClasses() {
		final List<String> rootClasses = new ArrayList<>();
		for (IHawkObject o : r.getAllContents()) {
			if (o instanceof IHawkClass) {
				IHawkClass hc = (IHawkClass)o;
				if (hc.getAllSuperTypes().isEmpty()) {
					rootClasses.add(hc.getName());
				}
			}
		}
		//assertTrue("Should contain the Element root MClass", rootClasses.contains("Element"));
		//assertTrue("Should contain the InteractionNavigationServices MClass", rootClasses.contains("InteractionNavigationServices"));
		assertTrue("Should contain the meta type", rootClasses.contains(ModelioMetaModelResource.META_TYPE_NAME));
		assertTrue("Should contain the meta type", rootClasses.contains("SmObject"));
		assertEquals("There should be exactly three root ModelioClasses", 2, rootClasses.size());
	}

	@Test
	public void checkModuleComponentHierarchy() {
		final Set<String> names = new HashSet<>();
		for (IHawkClass hc : RegisterMeta.getModelioClass("ModuleComponent", null).getAllSuperTypes()) {
			names.add(hc.getName());
		}
	//	assertTrue(names.contains("Class"));
	//	assertTrue(names.contains("Component"));
		assertTrue(names.contains("AbstractProject"));
		assertTrue(names.contains("ModelElement"));
		assertTrue(names.contains("Element"));
		assertTrue(names.contains("SmObject"));
	}

}
