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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MMetamodelParser {
	private MMetamodelDescriptor metamodelDescriptor;

	public MMetamodelParser() {
		metamodelDescriptor = new MMetamodelDescriptor();
	}

	public MMetamodelDescriptor parse(File file) {

		this.metamodelDescriptor.clearFragments();

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
	}

	private void addFragment(Node node) {
		MFragment fragment = new MFragment();

		fragment.setName(getNodeNamedAttribute(node, "name"));
		fragment.setVersion(getNodeNamedAttribute(node, "version"));
		fragment.setProvider(getNodeNamedAttribute(node, "provider"));
		fragment.setProviderVersion(getNodeNamedAttribute(node, "providerVersion"));

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			if (isElement(childNodes.item(i), "metaclasses")) {

				NodeList metaclasses = ((Element) childNodes.item(i)).getElementsByTagName("metaclass");

				for (int j = 0; j < metaclasses.getLength(); j++) {
					fragment.addMetaclass(parseMetaclass(metaclasses.item(j)));
				}

				metaclasses = ((Element) childNodes.item(i)).getElementsByTagName("link_metaclass");

				for (int j = 0; j < metaclasses.getLength(); j++) {
					fragment.addMetaclass(parseLinkMetaclass(metaclasses.item(j)));
				}
			} else if (isElement(childNodes.item(i), "dependencies")) {

				NodeList dependencies = ((Element) childNodes.item(i)).getElementsByTagName("metamodel_fragment");

				for (int j = 0; j < dependencies.getLength(); j++) {

					MFragmentReference fragmentRef = new MFragmentReference(
							getNodeNamedAttribute(dependencies.item(j), "name"),
							getNodeNamedAttribute(dependencies.item(j), "version"));

					fragment.addDependency(fragmentRef);
				}

			} else if (isElement(childNodes.item(i), "enumerations")) {

				NodeList enumerations = ((Element) childNodes.item(i)).getElementsByTagName("enumeration");

				for (int j = 0; j < enumerations.getLength(); j++) {
					fragment.addEnumeration(parseEnumeration(enumerations.item(j)));
				}
			}
		}

		if (fragment != null) {
			this.metamodelDescriptor.addFragment(fragment);
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

	void parseMetaclassAttributes(MMetaclass metaclass, Node node) {
		metaclass.setName(getNodeNamedAttribute(node, "name"));
		metaclass.setVersion(getNodeNamedAttribute(node, "version"));
		metaclass.setAbstract(getBoolean(getNodeNamedAttribute(node, "abstract")));
		metaclass.setCmsNode(getBoolean(getNodeNamedAttribute(node, "cmsNode")));
	}

	private void parseMetaclassChildren(MMetaclass metaclass, Node node) {
		if (isElement(node, "attribute")) {
			MAttribute attribute = new MAttribute(getNodeNamedAttribute(node, "name"), getNodeNamedAttribute(node, "type"));
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

	private MMetaclassDependency parseMetaclassDependency(Node node) {
		MMetaclassDependency dependency = new MMetaclassDependency();

		dependency.setName(getNodeNamedAttribute(node, "name"));
		dependency.setAggregation(getNodeNamedAttribute(node, "aggregation"));
		dependency.setNavigate(getBoolean(getNodeNamedAttribute(node, "navigate")));
		dependency.setMin(getInt(getNodeNamedAttribute(node, "min")));
		dependency.setMax(getInt(getNodeNamedAttribute(node, "max")));

		NodeList childNodes = node.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			
			if (isElement(childNodes.item(i), "target")) {

				MMetaclassReference target = new MMetaclassReference(
						getNodeNamedAttribute(childNodes.item(i), "name"),
						getNodeNamedAttribute(childNodes.item(i), "fragment"));

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
