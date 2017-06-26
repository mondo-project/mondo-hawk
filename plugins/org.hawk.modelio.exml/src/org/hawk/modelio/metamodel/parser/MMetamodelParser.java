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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

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

			for(Node childNode :  NodeListIterable(((Element) node).getElementsByTagName("fragment"))) {
				addFragment(childNode);
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

		for(Node childNode :  NodeListIterable(node.getChildNodes())) {
			if (isElement(childNode, "metaclasses")) {

				for(Node metaclassNode :  NodeListIterable(((Element) childNode).getElementsByTagName("metaclass"))) {
					currentFragment.addMetaclass(parseMetaclass(metaclassNode));
				}

				for(Node metaclassNode :  NodeListIterable(((Element) childNode).getElementsByTagName("link_metaclass"))) {
					currentFragment.addMetaclass(parseLinkMetaclass(metaclassNode));
				}
			} else if (isElement( childNode, "dependencies")) {
				for(Node dependencyNode :  NodeListIterable(((Element) childNode).getElementsByTagName("metamodel_fragment"))) {
					MFragmentReference fragmentRef = new MFragmentReference(
							getNodeNamedAttribute(dependencyNode, "name"),
							getNodeNamedAttribute(dependencyNode, "version"));

					currentFragment.addDependency(fragmentRef);
				}

			} else if (isElement(childNode, "enumerations")) {
				for(Node enumerationNode :  NodeListIterable(((Element) childNode).getElementsByTagName("enumeration"))) {
					currentFragment.addEnumeration(parseEnumeration(enumerationNode));
				}
			}
		}

		if (currentFragment != null) {
			this.metamodelDescriptor.addFragment(currentFragment);
		}
	}

	private MEnumeration parseEnumeration(Node node) {
		MEnumeration enumeration = new MEnumeration(getNodeNamedAttribute(node, "name"));

		for(Node valueNode :  NodeListIterable(((Element) node).getElementsByTagName("value"))) {
			enumeration.addValue(getNodeNamedAttribute(valueNode, "name"));
		}

		return enumeration;
	}

	private MMetaclass parseMetaclass(Node node) {
		MMetaclass metaclass = new MMetaclass();

		parseMetaclassAttributes(metaclass, node);

		for(Node childNode :  NodeListIterable( node.getChildNodes())) {

			parseMetaclassChildren(metaclass, childNode);
		}

		return metaclass;
	}

	private MLinkMetaclass parseLinkMetaclass(Node node) {
		MLinkMetaclass metaclass = new MLinkMetaclass();

		parseMetaclassAttributes(metaclass, node);

		for(Node childNode :  NodeListIterable( node.getChildNodes())) {
			parseLinkMetaclassChildren(metaclass, childNode);
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
				type = new MEnumeration(enumTypeName);

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

		for(Node childNode :  NodeListIterable( node.getChildNodes())) {
			
			if (isElement(childNode, "target")) {

				MMetaclassReference target = new MMetaclassReference(
						getNodeNamedAttribute(childNode, "fragment"),
						getNodeNamedAttribute(childNode, "name"));

				dependency.setTarget(target);

			} else if (isElement(childNode, "opposite")) {
				dependency.setOppositeName(getNodeNamedAttribute(childNode, "name"));
			}
		}

		return dependency;
	}

	private void parseLinkMetaclassChildren(MLinkMetaclass metaclass, Node node) {
		if (isElement(node, "targets")) {

			for(Node depNode : NodeListIterable(((Element) node).getElementsByTagName("dep"))) {
				((MLinkMetaclass) metaclass).addTarget(getNodeNamedAttribute(depNode, "name"));
			}

		} else if (isElement(node, "sources")) {
			
			for(Node depNode : NodeListIterable(((Element) node).getElementsByTagName("dep"))) {
				((MLinkMetaclass) metaclass).addSource(getNodeNamedAttribute(depNode, "name"));
			}

		} else {
			parseMetaclassChildren(metaclass, node);
		}
	}
	
	private void resolveReferences() {
		
		for (Entry<String, MFragment> fragmentEntry : this.metamodelDescriptor.getFragments().entrySet()) {
			// resolve fragment references
			for (MFragmentReference fragmentDependencyEntry : fragmentEntry.getValue().getDependencies()) {
				resolveFragmentReference(fragmentDependencyEntry);
			}

			// resolve metaclasses
			for (Entry<String, MMetaclass> metaclassEntry : fragmentEntry.getValue().getMetaclasses().entrySet()) {

				// resolve parent
				resolveMetaclassReference(metaclassEntry.getValue().getParent());

				// resolve enumerations
				for(MAttribute attribute : metaclassEntry.getValue().getAttributes()) {
					if(attribute.getType().isEnum()) {
						attribute.setType(fragmentEntry.getValue().getEnumeration(attribute.getType().getName()));
					}
				}
				
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
			// @todo: what about version, check version if it is not equal issue a warning
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


	public static Iterable<Node> NodeListIterable(final NodeList n) {
		  return new Iterable<Node>() {

		    @Override
		    public Iterator<Node> iterator() {

		      return new Iterator<Node>() {

		        int index = 0;

		        @Override
		        public boolean hasNext() {
		          return index < n.getLength();
		        }

		        @Override
		        public Node next() {
		          if (hasNext()) {
		            return n.item(index++);
		          } else {
		            throw new NoSuchElementException();
		          }  
		        }

		        @Override
		        public void remove() {
		          throw new UnsupportedOperationException();
		        }
		      };
		    }
		  };
		}

}
