/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser Test 
 ******************************************************************************/

package org.hawk.modelio.metamodel.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

public class MetamodelDescriptorTest {

	private static final String METAMODEL_PATH = "resources/metamodel/";
	private static MMetamodelDescriptor metamodelDescriptor;
	private static MMetamodelParser metamodelParser;
		
	@BeforeClass
	public static void setup() {
		metamodelParser = new MMetamodelParser();
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");
		InputSource is = null;
		try {
			is = new InputSource(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		metamodelDescriptor = metamodelParser.parse(is);	
	}

	@Test
	public void testNumberOfFragments() {
		// asserts
		assertEquals("Check metamodel has 5 fragments", 5, metamodelDescriptor.getFragments().size());
	}
	
	@Test
	public void testAnalystFragmentAttributes() {	
		testFragmentAttributes("Analyst", "2.0.00", "Modeliosoft", "3.6.00", 2, 19, 0);
	}
	
	@Test
	public void testFragmentDependenciesInAnalystFragment() {
		MFragment fragment = metamodelDescriptor.getFragments().get("Analyst");
		assertEquals("Check 2 Dependencies", 2, fragment.getDependencies().size());
		
		// Check First 
		assertEquals("Check name", "modelio.kernel", fragment.getDependencies().get(0).getName());
		assertEquals("Check version", "1.0.00", fragment.getDependencies().get(0).getVersion());
		
		assertEquals("Check fragment ref", metamodelDescriptor.getFragments().get("modelio.kernel"), fragment.getDependencies().get(0).getFragment());
		
		// Check Second 
		assertEquals("Check name", "Infrastructure", fragment.getDependencies().get(1).getName());
		assertEquals("Check version", "2.0.00", fragment.getDependencies().get(1).getVersion());
		
		assertEquals("Check fragment ref", metamodelDescriptor.getFragments().get("Infrastructure"), fragment.getDependencies().get(1).getFragment());
	}
	
	@Test
	public void testMetaclassesInAnalystFragment() {
		MFragment fragment = metamodelDescriptor.getFragments().get("Analyst");
		assertEquals("Check 19 metaclasses", 19, fragment.getMetaclasses().size());
		
		// Check class "AnalystContainer" 
		MMetaclass currentMetaclass = fragment.getMetaclasses().get("AnalystContainer");
		assertNotEquals("AnalystContainer is not of Type LinkMetaclass", currentMetaclass.getClass().getName(), (MLinkMetaclass.class.getName()));
		assertEquals("AnalystContainer is of Type MMetaclass", currentMetaclass.getClass().getName(), (MMetaclass.class.getName()));
		
		testMetaClassAttributes(currentMetaclass, "AnalystContainer", "0.0.9054", true, false);
		testMetaclassRef(currentMetaclass.getParent(), "Analyst", "AnalystItem");

		//Check class "AnalystItem" 
		currentMetaclass = fragment.getMetaclasses().get("AnalystItem");
	
		testMetaClassAttributes(currentMetaclass, "AnalystItem", "0.0.9054", true, false);
		testMetaclassRef(currentMetaclass.getParent(), "Infrastructure", "ModelElement");
		
		assertEquals("Check number of attributes", 1, currentMetaclass.getAttributes().size());
		testAttributeValues(currentMetaclass.getAttributes().get(0), "Definition", "java.lang.String", fragment);
			
		assertEquals("Check number of dependencies", 1, currentMetaclass.getDependencies().size());
		testMetaclassDependencies(currentMetaclass.getDependencies().get("AnalystProperties"), "AnalystProperties", 1, 1, MAggregationType.Composition, true, "Analyst", "AnalystPropertyTable", "AnalystOwner");
		
		// GoalContainer dependencies
		currentMetaclass = fragment.getMetaclasses().get("GoalContainer");
		assertEquals("Check Number of dependencies of GoalContainer", 4, currentMetaclass.getDependencies().size());
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnedGoal"), "OwnedGoal", 0, -1, MAggregationType.Composition, true, "Analyst", "Goal", "OwnerContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnerContainer"), "OwnerContainer", 0, 1, MAggregationType.None, false, "Analyst", "GoalContainer", "OwnedContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnedContainer"), "OwnedContainer", 0, -1, MAggregationType.Composition, true, "Analyst", "GoalContainer", "OwnerContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnerProject"), "OwnerProject", 0, 1, MAggregationType.None, false, "Analyst", "AnalystProject", "GoalRoot");
	}

	@Test
	public void testAttributeWithEnumType() {
		MFragment fragment = metamodelDescriptor.getFragments().get("Archimate");
		MLinkMetaclass currentMetaclass = (MLinkMetaclass)fragment.getMetaclasses().get("Access");
		testAttributeValues(currentMetaclass.getAttributes().get(0), "Mode", "org.modelio.archimate.metamodel.relationships.dependency.AccessMode", fragment);	
	}
	
	@Test
	public void testEnumerationsInStandardFragment() {

		MFragment fragment = metamodelDescriptor.getFragments().get("Standard");

		testEnumeration(fragment.getDataType("org.modelio.metamodel.bpmn.activities.AdHocOrdering"), "org.modelio.metamodel.bpmn.activities.AdHocOrdering", java.util.Arrays.asList("PARALLELORDERING", "SEQUENTIALORDERING"));
		
		testEnumeration(fragment.getDataType("org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior"), "org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior", java.util.Arrays.asList("NONEBEHAVIOR", "ONEBEHAVIOR", "ALLBEHAVIOR", "COMPLEXBEHAVIOR"));
		
		testEnumeration(fragment.getDataType("org.modelio.metamodel.bpmn.activities.TransactionMethod"), "org.modelio.metamodel.bpmn.activities.TransactionMethod", java.util.Arrays.asList("COMPENSATETRANSACTION", "STORETRANSACTION", "IMAGETRANSACTION"));
	}

	@Test
	public void testMetaclassesInStandardFragment() {
		
		MFragment fragment = metamodelDescriptor.getFragments().get("Standard");
		
		// link_metaclass Abstraction
		MLinkMetaclass currentMetaclass = (MLinkMetaclass)fragment.getMetaclasses().get("Abstraction");
		assertEquals("", currentMetaclass.getClass().getName(), (MLinkMetaclass.class.getName()));
		assertNotEquals("", currentMetaclass.getClass().getName(), (MMetaclass.class.getName()));

		// check sources and targets
		assertEquals("", 1, currentMetaclass.getTargetsDeps().size());
		assertEquals("", currentMetaclass.getTargetsDeps().containsKey("DependsOn"), true);
		
		assertEquals("", 1, currentMetaclass.getSourcesDeps().size());
		assertEquals("", currentMetaclass.getSourcesDeps().containsKey("Impacted"), true);
		
		//ElementImport
		currentMetaclass = (MLinkMetaclass)fragment.getMetaclasses().get("ElementImport");
		// check sources and targets
		assertEquals("", 2, currentMetaclass.getSourcesDeps().size());
		assertEquals("", currentMetaclass.getSourcesDeps().containsKey("ImportingNameSpace"), true);
		assertEquals("", currentMetaclass.getSourcesDeps().containsKey("ImportingOperation"), true);
	}

	private void testFragmentAttributes(String fragmentName, String version,
			String provider, String providerVersion, int numDeps, int numMetaclasses, int numEnums) {
		MFragment fragment = metamodelDescriptor.getFragments().get(fragmentName);
		assertEquals("Check fragment version", version, fragment.getVersion());
		assertEquals("Check fragment provider", provider, fragment.getProvider());
		assertEquals("Check fragment providerVersion", providerVersion, fragment.getProviderVersion());

		assertEquals("Check fragment number of Dependencies", numDeps, fragment.getDependencies().size());
		assertEquals("Check fragment number of metaclasses", numMetaclasses, fragment.getMetaclasses().size());
	}
 
	private void testEnumeration(MAttributeType mAttributeType, String name,
			List<String> list) {
		assertEquals("Check attribute name", name, mAttributeType.getName());
		assertEquals("Check eumeration values size", list.size(), ((MEnumeration) mAttributeType).getValues().size());
		for(int i = 0; i < list.size(); i++) {
			assertEquals("Check enumeration value", list.get(i), ((MEnumeration) mAttributeType).getValues().get(i));
		}
	}

	private void testMetaclassDependencies(
			MMetaclassDependency mMetaclassDependency, String name, int min,
			int max, MAggregationType aggregation, boolean navigate, String targetFragment, String targetName,
			String opposite) {
		
		assertEquals("Check Metaclass Dependency name", name, mMetaclassDependency.getName());
		assertEquals("Check Metaclass Dependency min", min, mMetaclassDependency.getMin());	
		assertEquals("Check Metaclass Dependency max", max, mMetaclassDependency.getMax());	
		assertEquals("Check Metaclass Dependency aggregation", aggregation, mMetaclassDependency.getAggregation());
		assertEquals("Check Metaclass Dependency navigate", navigate, mMetaclassDependency.isNavigate());	
		assertEquals("Check Metaclass Dependency opposite name", opposite, mMetaclassDependency.getOppositeName());	
		
		// check target
		testMetaclassRef(mMetaclassDependency.getTarget(), targetFragment, targetName);
		
		// check opposite
		testOppsiteDependency(mMetaclassDependency);
	}

	private void testOppsiteDependency(MMetaclassDependency mMetaclassDependency) {
		MFragment fragment = metamodelDescriptor.getFragments().get(mMetaclassDependency.getTarget().getFragmentName());
		MMetaclass currentMetaclass = fragment.getMetaclasses().get(mMetaclassDependency.getTarget().getName());
		
		MMetaclassDependency dependency = currentMetaclass.getDependency(mMetaclassDependency.getOppositeName());
		assertEquals(dependency, mMetaclassDependency.getOppositeDependency());
	}

	private void testMetaclassRef(MMetaclassReference ref, String fragment,
			String name) {
		
		assertEquals("check metaclass ref name", name, ref.getName());
		assertEquals("check metaclass ref fragment name", fragment, ref.getFragmentName());
		
		MMetaclass metaclass = metamodelDescriptor.getFragments().get(fragment).getMetaclasses().get(name);
		assertEquals("check metaclass ref metaclass", metaclass, ref.getMetaclass());
	}

	private void testMetaClassAttributes(MMetaclass currentMetaclass, String name, String version,
			boolean isAbstract, boolean cmsNode) {
		
		assertEquals("Check attribute name", name, currentMetaclass.getName());
		assertEquals("Check attribute version", version, currentMetaclass.getVersion());
		assertEquals("Check attribute abstract", isAbstract, currentMetaclass.isAbstract());
		assertEquals("Check attribute cmsNode", cmsNode, currentMetaclass.isCmsNode());
	}

	private void testAttributeValues(MMetaclassAttribute mAttribute, String name,
			String typeName, MFragment fragment) {
		assertEquals("Check attribute name", name, mAttribute.getName());
		assertEquals("Check attribute type", fragment.getDataType(typeName), mAttribute.getType());
	}
}
