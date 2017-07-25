/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Initial Implementation of Hawk Server Configuration  
 ******************************************************************************/
package org.hawk.service.servlet.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class ConfigFileParser {
	private static final String SCHEMA_URL = "platform:/plugin/org.hawk.service.servlet/resources/HawkServerConfigurationSchema.xsd";

	private static final String HAWK_SERVER_CONFIGURATION  = "HawkServerConfiguration";
	private static final String HAWK  = "hawk";

	private static final String NAME = "name";
	private static final String TYPE = "type";

	private static final String BACKEND  = "backend";

	private static final String DELAY  = "delay";
	private static final String MIN  = "min";
	private static final String MAX  = "max";

	private static final String PLUGINS = "plugins";
	private static final String PLUGIN = "plugin";

	private static final String METAMODELS = "metamodels";
	private static final String METAMODEL = "metamodel";

	private static final String LOCATION = "location";

	private static final String DERIVED_ATTRIBUTES = "derivedAttributes";
	private static final String DERIVED_ATTRIBUTE = "derivedAttribute";

	private static final String INDEXED_ATTRIBUTES = "indexedAttributes";
	private static final String INDEXED_ATTRIBUTE = "indexedAttribute";

	private static final String METAMODEL_URI = "metamodelUri";

	private static final String TYPE_NAME = "typeName";

	private static final String ATTRIBUTE_NAME = "attributeName";
	private static final String ATTRIBUTE_TYPE = "attributeType";

	private static final String IS_MANY = "isMany";
	private static final String IS_ORDERED = "isOrdered";
	private static final String IS_UNIQUE = "isUnique";

	private static final String DERIVATION = "derivation";
	private static final String LANGUAGE = "language";
	private static final String LOGIC = "logic";

	private static final String REPOSITORIES = "repositories";
	private static final String REPOSITORY = "repository";
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final String FROZEN = "frozen";

	//private HawkInstanceConfig currentConfig;

	Document document;
	public ConfigFileParser() {

	}

	public HawkInstanceConfig parse(File file) {
		HawkInstanceConfig config = null;
		try {
			
			Element element = getXmlDocumentRootElement(file);
			config = new HawkInstanceConfig(file.getAbsolutePath());
			parseConfig(element, config);

		} catch (Exception e) {
			System.err.print("error in parse(InputStream  in): ");
			System.err.println(e.getCause());
			e.printStackTrace();
		}

		return config;
	}

	private Element getXmlDocumentRootElement(File file)
			throws MalformedURLException, IOException, SAXException,
			ParserConfigurationException {
		DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		URL url = new URL(SCHEMA_URL);
		InputStream inputStream = url.openConnection().getInputStream();

		// create Schema for validation
		Source schemaSource = new StreamSource(inputStream);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(schemaSource);

		// set schema in Document Builder Factory
		factory.setSchema(schema);

		// parse document
		DocumentBuilder builder  = factory.newDocumentBuilder();

		// set custom error handler 
		builder.setErrorHandler(new SchemaErrorHandler());

		// start parsing
		document  = builder.parse(file);
		Element element  = document.getDocumentElement();
		return element;
	}

	Node createNewElement(String tagName) {
		return document.createElement(tagName);
	}
	
	Attr createNewAttribute(String tagName) {
		return document.createAttribute(tagName);
	}
	
	
	private boolean isElement(Node node, String name) {

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			if (node.getNodeName().equals(name)) {
				return true;
			}
		}

		return false;
	}

	private void parseConfig(Element element , HawkInstanceConfig config) {
		if(isElement(element, HAWK_SERVER_CONFIGURATION)) {

			// get hawk, usually one element per file
			for(Node hawkElement : NodeListIterable(element.getElementsByTagName(HAWK))) {
				// get name
				config.setName(((Element)hawkElement).getAttribute(NAME));
				// get back-end
				config.setBackend(((Element)hawkElement).getAttribute(BACKEND));

				// get delay
				setDelay(((Element)hawkElement).getElementsByTagName(DELAY),  config);

				// get plug ins 
				setPlugins(((Element)hawkElement).getElementsByTagName(PLUGINS),  config);

				// get meta-models
				setMetamodels(((Element)hawkElement).getElementsByTagName(METAMODELS),  config);

				// get repositories
				setRepositories(((Element)hawkElement).getElementsByTagName(REPOSITORIES),  config);

				// get indexed attributes
				setIndexedAttributes(((Element)hawkElement).getElementsByTagName(INDEXED_ATTRIBUTES),  config);

				// get derived attributes
				setDerivedAttributes(((Element)hawkElement).getElementsByTagName(DERIVED_ATTRIBUTES),  config);
			}
		}  
	}


	public String saveConfigAsXml(HawkInstanceConfig config) {
//		try {
//
//			Element element = getXmlDocumentRootElement(new File(config.getFileName()));
//			
//			for(Node hawkElement : NodeListIterable(element.getElementsByTagName(HAWK))) {
//				// get name
//				((Element)hawkElement).setAttribute(NAME, config.getName());
//				
//				// get back-end
//				//config.setBackend(((Element)hawkElement).getAttribute(BACKEND));
//				((Element)hawkElement).setAttribute(BACKEND, config.getBackend());
//				
//				// get delay
//				setDelay(((Element)hawkElement).getElementsByTagName(DELAY),  config);
//
//				
//				// get plug ins 
//				setPlugins(((Element)hawkElement).getElementsByTagName(PLUGINS),  config);
//
//				// get meta-models
//				setMetamodels(((Element)hawkElement).getElementsByTagName(METAMODELS),  config);
//
//				// get repositories
//				setRepositories(((Element)hawkElement).getElementsByTagName(REPOSITORIES),  config);
//
//				// get indexed attributes
//				setIndexedAttributes(((Element)hawkElement).getElementsByTagName(INDEXED_ATTRIBUTES),  config);
//
//				// get derived attributes
//				setDerivedAttributes(((Element)hawkElement).getElementsByTagName(DERIVED_ATTRIBUTES),  config);
//			}
//
//
//
//		} catch (IOException | SAXException | ParserConfigurationException e) {
//			e.printStackTrace();
//		}



		return null;
	}

	private void setPlugins(NodeList nodes, HawkInstanceConfig config) {
		for(Node pluginsElement : NodeListIterable(nodes)) {
			for(Node pluginElement : NodeListIterable(((Element) pluginsElement).getElementsByTagName(PLUGIN))) {
				config.getPlugins().add(((Element)pluginElement).getAttribute(NAME));
			}
		}			
	}

	private void setDerivedAttributes(NodeList nodes, HawkInstanceConfig config) {
		for(Node derivedAttributesElement : NodeListIterable(nodes)) {
			for(Node derivedAttributeElement : NodeListIterable(((Element) derivedAttributesElement).getElementsByTagName(DERIVED_ATTRIBUTE))) {
				DerivedAttributeParameters params = new DerivedAttributeParameters();

				params.metamodelUri = ((Element)derivedAttributeElement).getAttribute(METAMODEL_URI);
				params.typeName = ((Element)derivedAttributeElement).getAttribute(TYPE_NAME);
				params.attributeName = ((Element)derivedAttributeElement).getAttribute(ATTRIBUTE_NAME);
				params.attributeType = ((Element)derivedAttributeElement).getAttribute(ATTRIBUTE_TYPE);

				params.isMany = Boolean.valueOf(((Element)derivedAttributeElement).getAttribute(IS_MANY));
				params.isOrdered = Boolean.valueOf(((Element)derivedAttributeElement).getAttribute(IS_ORDERED));
				params.isUnique = Boolean.valueOf(((Element)derivedAttributeElement).getAttribute(IS_UNIQUE));

				// get derivation
				NodeList derivations = ((Element)derivedAttributeElement).getElementsByTagName(DERIVATION);

				// only one element is expected
				if(derivations.getLength() >= 1) {
					// get derivation language
					params.derivationLanguage = ((Element) derivations.item(0)).getAttribute(LANGUAGE);

					// get logic
					NodeList logics = ((Element)derivations.item(0)).getElementsByTagName(LOGIC);
					// only one element is expected
					if(logics.getLength() >= 1) {
						params.derivationLogic = getElementCDataValue(logics.item(0));
					}
				}

				config.getDerivedAttributes().add(params);
			}
		}			

	}

	private String getElementCDataValue(Node node) {
		String value = "";

		NodeList cdataSections = node.getChildNodes();

		for(Node cdata : NodeListIterable(cdataSections)) {
			value += cdata.getNodeValue();
		}

		return value;
	}

	private void setIndexedAttributes(NodeList nodes, HawkInstanceConfig config) {
		for(Node indexedAttributeElements : NodeListIterable(nodes)) {
			for(Node indexedAttributeElement : NodeListIterable(((Element) indexedAttributeElements).getElementsByTagName(INDEXED_ATTRIBUTE))) {
				IndexedAttributeParameters params = new IndexedAttributeParameters(((Element)indexedAttributeElement).getAttribute(METAMODEL_URI), 
						((Element)indexedAttributeElement).getAttribute(TYPE_NAME), 
						((Element)indexedAttributeElement).getAttribute(ATTRIBUTE_NAME));

				config.getIndexedAttributes().add(params);
			}
		}		

	}

	private void setRepositories(NodeList nodes, HawkInstanceConfig config) {
		for(Node repoElements : NodeListIterable(nodes)) {
			for(Node repoElement : NodeListIterable(((Element) repoElements).getElementsByTagName(REPOSITORY))) {
				RepositoryParameters params = new RepositoryParameters();

				params.type = ((Element)repoElement).getAttribute(TYPE);
				params.location = ((Element)repoElement).getAttribute(LOCATION);
				params.user = ((Element)repoElement).getAttribute(USER);
				params.pass = ((Element)repoElement).getAttribute(PASS);
				params.isFrozen = Boolean.valueOf(((Element)repoElement).getAttribute(FROZEN));

				config.getRepositories().add(params);
			}
		}		
	}



	private void setMetamodels(NodeList nodes, HawkInstanceConfig config) {
		for(Node metamodelElements : NodeListIterable(nodes)) {
			for(Node metamodelElement : NodeListIterable(((Element) metamodelElements).getElementsByTagName(METAMODEL))) {
				config.getMetamodels().add(((Element)metamodelElement).getAttribute(LOCATION));
			}
		}		
	}

	private void setDelay(NodeList nodes, HawkInstanceConfig config) {
		// only one element is expected
		if(nodes.getLength() >= 1) {
			config.setDelayMax(Integer.valueOf(((Element) nodes.item(0)).getAttribute(MAX)));
			config.setDelayMin(Integer.valueOf(((Element) nodes.item(0)).getAttribute(MIN)));
		}
	}
	
	
	private void writeDelay(Element parent, HawkInstanceConfig config) {
		// only one element is expected
		NodeList nodes = ((Element)parent).getElementsByTagName(DELAY);
		if(nodes.getLength() >= 1) {
			((Element) nodes.item(0)).setAttribute(MAX, String.valueOf(config.getDelayMax()));
			((Element) nodes.item(0)).setAttribute(MIN, String.valueOf(config.getDelayMin()));
		} else {
			// add new child
//			Node delayNode = 
//			parent.appendChild(newChild);
		}
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

