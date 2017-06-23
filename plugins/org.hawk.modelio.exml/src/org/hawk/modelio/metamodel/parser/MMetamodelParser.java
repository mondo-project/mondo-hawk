/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser
 ******************************************************************************/

package org.hawk.modelio.metamodel.parser;

import java.io.File;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MMetamodelParser {
	private MMetamodelDescriptor metamodelDescriptor;
	private MFragment currentFragment;

	public MMetamodelParser() {
		metamodelDescriptor = new MMetamodelDescriptor();
	}

	public MMetamodelDescriptor parse(File file) {

		this.metamodelDescriptor.reset();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document;
			document = builder.parse(file);
			Element element = document.getDocumentElement();

			parseMetamodel(element);

		} catch (Exception e) {
			System.err.print("error in parse(File f): ");
			System.err.println(e.getCause());
			e.printStackTrace();
			return null;
		}

		return this.metamodelDescriptor;
	}

	private void parseMetamodel(Node node) {

		if (isElement(node, "metamodel")) {
			this.metamodelDescriptor.setMetamodelFormat(getNodeNamedAttribute(node, "format"));
			this.metamodelDescriptor.setMetamodelDescriptorFormat(getNodeNamedAttribute(node, "MetamodelDescriptor.format"));

			NodeList childNodes = ((Element) node).getElementsByTagName("fragment");
			
			for (int i = 0; i < childNodes.getLength(); i++) {
				addFragment(childNodes.item(i));
			}
		}
		
		resolveReferences();
	}

	private void addFragment(Node node) {
		currentFragment = new MFragment(); // current fragment being parsed (needed for enumeration resolution)

		currentFragment.setName(getNodeNamedAttribute(node, "name"));
		currentFragment.setVersion(getNodeNamedAttribute(node, "version"));
		currentFragment.setProvider(getNodeNamedAttribute(node, "provider"));
		currentFragment.setProviderVersion(getNodeNamedAttribute(node, "providerVersion"));

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {

			if (isElement(childNodes.item(i), "metaclasses")) {

				NodeList metaclasses = ((Element) childNodes.item(i)).getElementsByTagName("metaclass");

				for (int j = 0; j < metaclasses.getLength(); j++) {
					currentFragment.addMetaclass(parseMetaclass(metaclasses.item(j)));
				}

				metaclasses = ((Element) childNodes.item(i)).getElementsByTagName("link_metaclass");

				for (int j = 0; j < metaclasses.getLength(); j++) {
					currentFragment.addMetaclass(parseLinkMetaclass(metaclasses.item(j)));
				}
			} else if (isElement(childNodes.item(i), "dependencies")) {

				NodeList dependencies = ((Element) childNodes.item(i)).getElementsByTagName("metamodel_fragment");

				for (int j = 0; j < dependencies.getLength(); j++) {

					MFragmentReference fragmentRef = new MFragmentReference(
							getNodeNamedAttribute(dependencies.item(j), "name"),
							getNodeNamedAttribute(dependencies.item(j), "version"));

					currentFragment.addDependency(fragmentRef);
				}

			} else if (isElement(childNodes.item(i), "enumerations")) {

				NodeList enumerations = ((Element) childNodes.item(i)).getElementsByTagName("enumeration");

				for (int j = 0; j < enumerations.getLength(); j++) {
					currentFragment.updateEnumeration(parseEnumeration(enumerations.item(j)));
				}
			}
		}

		if (currentFragment != null) {
			this.metamodelDescriptor.addFragment(currentFragment);
		}
	}

	private MEnumeration parseEnumeration(Node node) {
		MEnumeration enumeration = new MEnumeration(getNodeNamedAttribute(node, "name"));

		NodeList values = ((Element) node).getElementsByTagName("value");
		for (int i = 0; i < values.getLength(); i++) {
			enumeration.addValue(getNodeNamedAttribute(values.item(i), "name"));
		}

		return enumeration;
	}

	private MMetaclass parseMetaclass(Node node) {
		MMetaclass metaclass = new MMetaclass();

		parseMetaclassAttributes(metaclass, node);

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {

			parseMetaclassChildren(metaclass, childNodes.item(i));
		}

		return metaclass;
	}

	private MLinkMetaclass parseLinkMetaclass(Node node) {
		MLinkMetaclass metaclass = new MLinkMetaclass();

		parseMetaclassAttributes(metaclass, node);

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			parseLinkMetaclassChildren(metaclass, childNodes.item(i));
		}

		return metaclass;
	}

	private void parseMetaclassAttributes(MMetaclass metaclass, Node node) {
		metaclass.setName(getNodeNamedAttribute(node, "name"));
		metaclass.setVersion(getNodeNamedAttribute(node, "version"));
		metaclass.setAbstract(getBoolean(getNodeNamedAttribute(node, "abstract")));
		metaclass.setCmsNode(getBoolean(getNodeNamedAttribute(node, "cmsNode")));
	}

	private void parseMetaclassChildren(MMetaclass metaclass, Node node) {
		if (isElement(node, "attribute")) {
			
			String enumTypeName = getNodeNamedAttribute(node, "enumType");
			String typeName = getNodeNamedAttribute(node, "type");
			MDataType type;

			if(typeName.equals("java.lang.Enum")) {
				type = resolveAttributeEnumType(enumTypeName); 
			} else {
				type = resolveAttributeBasicType(typeName); 
			}
			
			MAttribute attribute = new MAttribute(getNodeNamedAttribute(node, "name"), type);
			metaclass.addAttribute(attribute);

		} else if (isElement(node, "parent")) {

			MMetaclassReference parent = new MMetaclassReference(
					getNodeNamedAttribute(node, "fragment"),
					getNodeNamedAttribute(node, "name"));

			metaclass.setParent(parent);

		} else if (isElement(node, "dependency")) {
			metaclass.addDependency(parseMetaclassDependency(node));
		}
	}

	private MDataType resolveAttributeEnumType(String enumTypeName) {
		MDataType type = currentFragment.getEnumeration(enumTypeName);
		
		// add enumeration if not present in current fragment
		if(type == null) {
			type  = new MEnumeration(enumTypeName);
			currentFragment.addEnumeration((MEnumeration) type);
		}
		
		return type;
	}

	private MDataType resolveAttributeBasicType(String typeName) {
		MDataType type = metamodelDescriptor.getDataType(typeName);

		if(type == null) {
			type  = new MDataType(typeName);
			metamodelDescriptor.addDataType(type);
		}
		return type;
	}

	private MMetaclassDependency parseMetaclassDependency(Node node) {
		MMetaclassDependency dependency = new MMetaclassDependency();

		dependency.setName(getNodeNamedAttribute(node, "name"));
		
		dependency.setAggregation(getNodeNamedAttribute(node, "aggregation"));
		dependency.setNavigate(getBoolean(getNodeNamedAttribute(node, "navigate")));
		
		dependency.setCascadeDelete(getBoolean(getNodeNamedAttribute(node, "cascadeDelete")));
		dependency.setWeakReference(getBoolean(getNodeNamedAttribute(node, "weakReference")));
		
		dependency.setMin(getInt(getNodeNamedAttribute(node, "min")));
		dependency.setMax(getInt(getNodeNamedAttribute(node, "max")));

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			
			if (isElement(childNodes.item(i), "target")) {

				MMetaclassReference target = new MMetaclassReference(
						getNodeNamedAttribute(childNodes.item(i), "fragment"),
						getNodeNamedAttribute(childNodes.item(i), "name"));

				dependency.setTarget(target);

			} else if (isElement(childNodes.item(i), "opposite")) {
				dependency.setOppositeName(getNodeNamedAttribute(childNodes.item(i), "name"));
			}
		}

		return dependency;
	}

	private void parseLinkMetaclassChildren(MLinkMetaclass metaclass, Node node) {
		if (isElement(node, "targets")) {

			NodeList deps = ((Element) node).getElementsByTagName("dep");
			
			for (int j = 0; j < deps.getLength(); j++) {
				
				((MLinkMetaclass) metaclass).addTarget(getNodeNamedAttribute(deps.item(j), "name"));
			}

		} else if (isElement(node, "sources")) {
			
			NodeList deps = ((Element) node).getElementsByTagName("dep");
			
			for (int j = 0; j < deps.getLength(); j++) {
				
				((MLinkMetaclass) metaclass).addSource(getNodeNamedAttribute(deps.item(j), "name"));
			}

		} else {
			parseMetaclassChildren(metaclass, node);
		}
	}
	
	private void resolveReferences() {
		
		for (Entry<String, MFragment> fragmentEntry : this.metamodelDescriptor.getFragments().entrySet()) {
			// resolve fragment references
			for(int i = 0; i < fragmentEntry.getValue().getDependencies().size(); i++) {
				resolveFragmentReference(fragmentEntry.getValue().getDependencies().get(i));
			}

			// resolve metaclasses
			for (Entry<String, MMetaclass> metaclassEntry : fragmentEntry.getValue().getMetaclasses().entrySet()) {

				// resolve parent
				resolveMetaclassReference(metaclassEntry.getValue().getParent());

				// resolve dependencies.target
				for(Entry<String, MMetaclassDependency> dependencyEntry : metaclassEntry.getValue().getDependencies().entrySet()) {	
					resolveMetaclassReference(dependencyEntry.getValue().getTarget());
					resolveOppositeDependency(dependencyEntry.getValue());
					
					// @todo: if link_class, resolve sources and targets
				}
			}
		}
	}

	private void resolveMetaclassReference(MMetaclassReference ref) {
		if(ref != null) {
			MFragment targetFragment = this.metamodelDescriptor.getFragment(ref.getFragmentName());
			MMetaclass targetMetaclass = targetFragment.getMetaclass(ref.getName());
			ref.setMetaclass(targetMetaclass);
		}
	}
	
	private void resolveFragmentReference(MFragmentReference ref) {
		if(ref != null) {
			MFragment targetFragment = this.metamodelDescriptor.getFragment(ref.getName());
			ref.setFragment(targetFragment);
		}
	}
	
	private void resolveOppositeDependency(MMetaclassDependency ref) {
		if(ref != null) {
			MFragment targetFragment = this.metamodelDescriptor.getFragment(ref.getTarget().getFragmentName());
			MMetaclass targetMetaclass = targetFragment.getMetaclass(ref.getTarget().getName());
			MMetaclassDependency dependency = targetMetaclass.getDependency(ref.getOppositeName());
			ref.setOppositeDependency(dependency);
		}
	}
	
	private String getNodeNamedAttribute(Node node, String attributeName) {
		if (node.hasAttributes()) {
			
			if (node.getAttributes().getNamedItem(attributeName) != null) {
				return node.getAttributes().getNamedItem(attributeName).getNodeValue();
			}
		}

		return null;
	}

	private boolean getBoolean(String nodeValue) {

		if (nodeValue != null) {
			if (nodeValue.equalsIgnoreCase("true")) {
				return true;
			}
		}
		return false;
	}

	private int getInt(String nodeValue) {
		int intValue = Integer.MAX_VALUE;
		if (nodeValue != null) {
			intValue = Integer.parseInt(nodeValue);
		}
		return intValue;
	}

	private boolean isElement(Node node, String name) {

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			if (node.getNodeName().equals(name)) {
				return true;
			}
		}

		return false;
	}

}
