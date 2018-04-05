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
 *     Orjuwan Al-Wadeai - move to Modelio metamodel descriptors
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IFileImporter;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.junit.Before;
import org.junit.Test;

public class ModelioModelResourceFactoryTest {

	protected static final class DummyFileImporter implements IFileImporter {
		@Override
		public File importFile(String path) {
			return new File("resources", path);
		}
	}

	private static final String RAMC_PATH = "resources/jenkins/jenkins_1.540.0.ramc";
	private static final String ICONTAINMENT_PATH = "resources/implicitContainment/example.exml";
	private static final String BPMNCATCH_PATH = "resources/bpmn/bpmnCatchEvent.exml";

	private final String METAMODEL_PATH = "resources/metamodel/";
		
	@Before
	public void setup() {
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");
		try {
			ModelioMetaModelResourceFactory factory = new ModelioMetaModelResourceFactory();
			factory.parse(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ramc() throws Exception {
		final ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(
				new DummyFileImporter(),
				new File(RAMC_PATH));

		final Map<String, IHawkObject> elements = new HashMap<>();
		final Set<String> rootOrContained = new HashSet<>();
		for (IHawkObject o : resource.getAllContents()) {
			// Note: due to the way Associations work, the "same" (e.g. with the
			// same UID) ExmlObject might be repeated multiple times in a .ramc.
			// This can happen for Associations, since the same ExmlObject might
			// be on the two sides of it (but only truly contained in one).
			// This is OK: each copy will have the same state, and Modelio objects
			// are treated as singleton objects anyway.
			elements.put(o.getUriFragment(), o);

			if (o.isRoot()) {
				rootOrContained.add(o.getUriFragment());
			}
			if (o.getType() instanceof IHawkClass) {
				for (IHawkReference ref : ((IHawkClass) o.getType()).getAllReferences()) {
					if (ref.isContainment()) {
						Object value = o.get(ref, true);
						if (value instanceof Iterable) {
							for (IHawkObject target : (Iterable<IHawkObject>) value) {
								rootOrContained.add(target.getUriFragment());
							}
						} else if (value instanceof IHawkObject) {
							rootOrContained.add(((IHawkObject) value).getUriFragment());
						}
					} else if (ref.isContainer()) {
						rootOrContained.add(o.getUriFragment());
					}
				}
			}
		}

		HashSet<String> unreachable = new HashSet<>(elements.keySet());
		unreachable.removeAll(rootOrContained);
		if (!unreachable.isEmpty()) {
			System.err.println(String.format("Found %d unreachable objects among %d", unreachable.size(), resource.getAllContentsSet().size()));
			for (String o : unreachable) {
				System.err.println(elements.get(o));
			}
			fail("There should be no unreachable objects from the roots");
		}
	}

	@Test
	public void implicitContainment() throws Exception {
		final String grandparentUID = "25801104-0000-1069-0000-000000000000";
		final String parentUID = "00d01054-0001-1627-0000-000000000000";
		final String childUID = "00d011d0-0000-041f-0000-000000000000";

		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new DummyFileImporter(), new File(ICONTAINMENT_PATH));

		for (IHawkObject ob : resource.getAllContents()) {
			final ModelioObject mob = (ModelioObject)ob;
			final ModelioReference refParent = (ModelioReference)mob.getType().getStructuralFeature(ModelioClass.REF_PARENT);
			final ModelioProxy refValue = (ModelioProxy) mob.get(refParent, true);

			switch (mob.getUriFragment()) {
			case parentUID:
				assertNotNull("Parent of the parent should be set", refValue);
				assertEquals("Parent of the parent should be " + grandparentUID, grandparentUID, refValue.getUriFragment());
				break;
			case childUID:
				assertNotNull("Parent of the child should be set", refValue);
				assertEquals("Parent of the child should be the parent", parentUID, refValue.getUriFragment());
				break;
			default:
				fail("Unexpected object " + mob.getUriFragment());
			}
		}
	}

	@Test
	public void bpmnCatchEvent() throws Exception {
		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new DummyFileImporter(), new File(BPMNCATCH_PATH));
		assertEquals(5, resource.getAllContentsSet().size());

		boolean found = false;
		for (IHawkObject element : resource.getAllContentsSet()) {
			final IHawkClass type = (IHawkClass) element.getType();
			final IHawkAttribute attrName = (IHawkAttribute)type.getStructuralFeature("Name");
			if (attrName != null) {
				final String name = element.get(attrName) + "";
				if ("BpmnCatchEvent".equals(name)) {
					assertEquals("BpmnCatchEvent should be a Class", "Class", element.getType().getName());
					found = true;
				}
			}
		}
		assertTrue("The .exml file should contain a BpmnCatchEvent element", found);
	}
}
