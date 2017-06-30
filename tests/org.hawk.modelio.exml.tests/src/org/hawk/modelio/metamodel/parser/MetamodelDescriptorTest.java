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

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.hawk.modelio.metamodel.parser.MMetamodelDescriptor;
import org.hawk.modelio.metamodel.parser.MMetamodelParser;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetamodelDescriptorTest {

	private static final String METAMODEL_PATH = "resources/metamodel/";
	static MMetamodelDescriptor metamodeldescriptor;
	static MMetamodelParser metamodelParser;
		
	@BeforeClass
	public static void setup() {
		metamodelParser = new MMetamodelParser();
		File file = new File( METAMODEL_PATH + "metamodel_descriptor.xml");

		metamodeldescriptor = metamodelParser.parse(file);	
	}

	@Test
	public void testNumberOfFragments() {
		// asserts
		assertEquals("5 Fragments", 5, metamodeldescriptor.getFragments().size());
	}
	
	@Test
	public void testAnalystFragmentAttributes() {	
		testFragmentAttributes("Analyst", "2.0.00", "Modeliosoft", "3.6.00", 2, 19, 0);
	}
	
	@Test
	public void testDependenciesInAnalystFragment() {
		MFragment fragment = metamodeldescriptor.getFragments().get("Analyst");
		assertEquals("Check 2 Dependencies", 2, fragment.getDependencies().size());
		
		// Check First 
		assertEquals("Check name", "modelio.kernel", fragment.getDependencies().get(0).getName());
		assertEquals("Check version", "1.0.00", fragment.getDependencies().get(0).getVersion());
		
		assertEquals("Check fragment ref", metamodeldescriptor.getFragments().get("modelio.kernel"), fragment.getDependencies().get(0).getFragment());
		
		// Check Second 
		assertEquals("Check name", "Infrastructure", fragment.getDependencies().get(1).getName());
		assertEquals("Check version", "2.0.00", fragment.getDependencies().get(1).getVersion());
		
		assertEquals("Check fragment ref", metamodeldescriptor.getFragments().get("Infrastructure"), fragment.getDependencies().get(1).getFragment());
	}
	
	@Test
	public void testMetaclassesInAnalystFragment() {
		MFragment fragment = metamodeldescriptor.getFragments().get("Analyst");
		assertEquals("Check 19 metaclasses", 19, fragment.getMetaclasses().size());
		
		// Check class "AnalystContainer" 
		MMetaclass currentMetaclass = fragment.getMetaclasses().get("AnalystContainer");
		assertNotEquals("", currentMetaclass.getClass().getName(), (MLinkMetaclass.class.getName()));
		assertEquals("", currentMetaclass.getClass().getName(), (MMetaclass.class.getName()));
		
		testMetaClassAttributes(currentMetaclass, "AnalystContainer", "0.0.9054", true, false);
		testMetaclassRef(currentMetaclass.getParent(), "Analyst", "AnalystItem");

		//Check class "AnalystItem" 
		currentMetaclass = fragment.getMetaclasses().get("AnalystItem");
	
		testMetaClassAttributes(currentMetaclass, "AnalystItem", "0.0.9054", true, false);
		testMetaclassRef(currentMetaclass.getParent(), "Infrastructure", "ModelElement");
		
		assertEquals("Check attributes", 1, currentMetaclass.getAttributes().size());
		testAttributeValues(currentMetaclass.getAttributes().get(0), "Definition", "java.lang.String");
			
		assertEquals("Check dependencies", 1, currentMetaclass.getDependencies().size());
		testMetaclassDependencies(currentMetaclass.getDependencies().get("AnalystProperties"), "AnalystProperties", 1, 1, "Composition", true, "Analyst", "AnalystPropertyTable", "AnalystOwner");
		
		// GoalContainer dependencies
		currentMetaclass = fragment.getMetaclasses().get("GoalContainer");
		assertEquals("Check dependencies", 4, currentMetaclass.getDependencies().size());
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnedGoal"), "OwnedGoal", 0, -1, "Composition", true, "Analyst", "Goal", "OwnerContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnerContainer"), "OwnerContainer", 0, 1, null, false, "Analyst", "GoalContainer", "OwnedContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnedContainer"), "OwnedContainer", 0, -1, "Composition", true, "Analyst", "GoalContainer", "OwnerContainer");
		testMetaclassDependencies(currentMetaclass.getDependencies().get("OwnerProject"), "OwnerProject", 0, 1, null, false, "Analyst", "AnalystProject", "GoalRoot");
	}

	@Test
	public void testAttributeWithEnumType() {
		MFragment fragment = metamodeldescriptor.getFragments().get("Archimate");
		MLinkMetaclass currentMetaclass = (MLinkMetaclass)fragment.getMetaclasses().get("Access");
		testAttributeValuesEnum(fragment, currentMetaclass.getAttributes().get(0), "Mode", "org.modelio.archimate.metamodel.relationships.dependency.AccessMode");	
	}
	
	private void testAttributeValuesEnum(MFragment fragment,
			MMetaclassAttribute mAttribute, String name,
			String typeName) {
		assertEquals("Check Attribute Values", name, mAttribute.getName());
		assertEquals("Check Attribute Values", fragment.getEnumeration(typeName), mAttribute.getType());		
	}

	@Test
	public void testEnumerationsInStandardFragment() {

//		testEnumeration((MEnumeration)metamodeldescriptor.getDataType("org.modelio.metamodel.bpmn.activities.AdHocOrdering"), "org.modelio.metamodel.bpmn.activities.AdHocOrdering", java.util.Arrays.asList("PARALLELORDERING", "SEQUENTIALORDERING"));
//		
//		testEnumeration((MEnumeration)metamodeldescriptor.getDataType("org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior"), "org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior", java.util.Arrays.asList("NONEBEHAVIOR", "ONEBEHAVIOR", "ALLBEHAVIOR", "COMPLEXBEHAVIOR"));
//		
//		testEnumeration((MEnumeration)metamodeldescriptor.getDataType("org.modelio.metamodel.bpmn.activities.TransactionMethod"), "org.modelio.metamodel.bpmn.activities.TransactionMethod", java.util.Arrays.asList("COMPENSATETRANSACTION", "STORETRANSACTION", "IMAGETRANSACTION"));

		
		MFragment fragment = metamodeldescriptor.getFragments().get("Standard");

		assertEquals("Check num metaclasses", 25, fragment.getEnumerations().size());

		testEnumeration(fragment.getEnumeration("org.modelio.metamodel.bpmn.activities.AdHocOrdering"), "org.modelio.metamodel.bpmn.activities.AdHocOrdering", java.util.Arrays.asList("PARALLELORDERING", "SEQUENTIALORDERING"));
		
		testEnumeration(fragment.getEnumeration("org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior"), "org.modelio.metamodel.bpmn.activities.MultiInstanceBehavior", java.util.Arrays.asList("NONEBEHAVIOR", "ONEBEHAVIOR", "ALLBEHAVIOR", "COMPLEXBEHAVIOR"));
		
		testEnumeration(fragment.getEnumeration("org.modelio.metamodel.bpmn.activities.TransactionMethod"), "org.modelio.metamodel.bpmn.activities.TransactionMethod", java.util.Arrays.asList("COMPENSATETRANSACTION", "STORETRANSACTION", "IMAGETRANSACTION"));
	}

	@Test
	public void testMetaclassesInStandardFragment() {
		
		MFragment fragment = metamodeldescriptor.getFragments().get("Standard");
		
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
		MFragment fragment = metamodeldescriptor.getFragments().get(fragmentName);
		assertEquals("Check Version", version, fragment.getVersion());
		assertEquals("Check provider", provider, fragment.getProvider());
		assertEquals("Check providerVersion", providerVersion, fragment.getProviderVersion());

		assertEquals("Check num Dependencies", numDeps, fragment.getDependencies().size());
		assertEquals("Check num metaclasses", numMetaclasses, fragment.getMetaclasses().size());
		//assertEquals("Check num metaclasses", numEnums, fragment.getEnumerations().size());
	}
 
	private void testEnumeration(MEnumeration mEnumeration, String name,
			List<String> list) {
		assertEquals("Check name", name, mEnumeration.getName());
		assertEquals("Check name", list.size(), mEnumeration.getValues().size());
		
		for(int i = 0; i < list.size(); i++) {
			assertEquals("Check values", list.get(i), mEnumeration.getValues().get(i));
		}
	}

	private void testMetaclassDependencies(
			MMetaclassDependency mMetaclassDependency, String name, int min,
			int max, String aggregation, boolean navigate, String targetFragment, String targetName,
			String opposite) {
		
		assertEquals("Check name", name, mMetaclassDependency.getName());
		assertEquals("Check name", min, mMetaclassDependency.getMin());	
		assertEquals("Check name", max, mMetaclassDependency.getMax());	
		assertEquals("Check name", aggregation, mMetaclassDependency.getAggregation());
		assertEquals("Check name", navigate, mMetaclassDependency.isNavigate());	
		assertEquals("Check name", opposite, mMetaclassDependency.getOppositeName());	
		
		// check target
		testMetaclassRef(mMetaclassDependency.getTarget(), targetFragment, targetName);
		
		// check opposite
		testOppsiteDependency(mMetaclassDependency);
	}

	private void testOppsiteDependency(MMetaclassDependency mMetaclassDependency) {
		MFragment fragment = metamodeldescriptor.getFragments().get(mMetaclassDependency.getTarget().getFragmentName());
		MMetaclass currentMetaclass = fragment.getMetaclasses().get(mMetaclassDependency.getTarget().getName());
		
		MMetaclassDependency dependency = currentMetaclass.getDependency(mMetaclassDependency.getOppositeName());
		assertEquals(dependency, mMetaclassDependency.getOppositeDependency());
	}

	private void testMetaclassRef(MMetaclassReference ref, String fragment,
			String name) {
		
		assertEquals("check metaclass ref name", name, ref.getName());
		assertEquals("check metaclass ref fragment name", fragment, ref.getFragmentName());
		
		MMetaclass metaclass = metamodeldescriptor.getFragments().get(fragment).getMetaclasses().get(name);
		assertEquals("check metaclass ref metaclass", metaclass, ref.getMetaclass());
	}

	private void testMetaClassAttributes(MMetaclass currentMetaclass, String name, String version,
			boolean isAbstract, boolean cmsNode) {
		
		assertEquals("Check name", name, currentMetaclass.getName());
		assertEquals("Check version", version, currentMetaclass.getVersion());
		assertEquals("Check abstract", isAbstract, currentMetaclass.isAbstract());
		assertEquals("Check cmsNode", cmsNode, currentMetaclass.isCmsNode());
	}

	private void testAttributeValues(MMetaclassAttribute mAttribute, String name,
			String typeName) {
		assertEquals("Check Attribute Values", name, mAttribute.getName());
		assertEquals("Check Attribute Values", metamodeldescriptor.getDataType(typeName), mAttribute.getType());
	}

}
