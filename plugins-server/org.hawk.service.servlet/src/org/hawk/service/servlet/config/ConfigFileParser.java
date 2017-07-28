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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;


public class ConfigFileParser {

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

	private static final String URI = "uri";

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


	public ConfigFileParser() {
	}

	public HawkInstanceConfig parse(File xmlFile) {
		HawkInstanceConfig config = null;
		if(xmlFile != null && xmlFile.exists()) {
		
			try {
				Element element = getXmlDocumentRootElement(xmlFile);
				config = new HawkInstanceConfig(xmlFile.getAbsolutePath());
				parseConfig(element, config);
			} catch (Exception e) {
				System.err.print("error in parse(File file): ");
				e.printStackTrace();
			}
		}
		return config;
	}

	private Element getXmlDocumentRootElement(File xmlFile) throws Exception {

		DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
		
		InputStream xsdFile = ConfigFileParser.class.getResourceAsStream("/resources/HawkServerConfigurationSchema.xsd");
		if(xsdFile != null ) {
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			
			// create Schema for validation
			Source schemaSource = new StreamSource(xsdFile);
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaSource);

			// set schema in Document Builder Factory
			factory.setSchema(schema);
		}
		// parse document
		DocumentBuilder builder  = factory.newDocumentBuilder();

		// set custom error handler 
		builder.setErrorHandler(new SchemaErrorHandler());

		// start parsing
		Document document  = builder.parse(xmlFile);
		Element element  = document.getDocumentElement();
		return element;
	}

	private void parseConfig(Element hawkElement , HawkInstanceConfig config) {
		if(hawkElement.getNodeName().equals(HAWK)) {
			config.setName(hawkElement.getAttribute(NAME));

			config.setBackend(hawkElement.getAttribute(BACKEND));

			readDelay(hawkElement.getElementsByTagName(DELAY),  config);

			readPlugins(hawkElement.getElementsByTagName(PLUGINS),  config);

			readMetamodels(hawkElement.getElementsByTagName(METAMODELS),  config);

			readRepositories(hawkElement.getElementsByTagName(REPOSITORIES),  config);

			readIndexedAttributes(hawkElement.getElementsByTagName(INDEXED_ATTRIBUTES),  config);

			readDerivedAttributes(hawkElement.getElementsByTagName(DERIVED_ATTRIBUTES),  config);
		}
	}


	public void saveConfigAsXml(HawkInstanceConfig config) {
		try {

			DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();

			Document document  = builder.newDocument();

			/** <hawk> */
			Element hawkElement = document.createElement(HAWK);
			createAndAddAttribute(document, hawkElement, NAME, config.getName());
			createAndAddAttribute(document, hawkElement, BACKEND, config.getBackend());

			/** <delay> */
			writeDelay(config, document, hawkElement);
			/** </delay> */

			/** <plugins> */
			writePlugins(config, document, hawkElement);
			/** </plugins> */

			/** <metamodels> */
			writeMetamodels(config, document, hawkElement);
			/** </metamodels> */

			/** <derivedAttributes> */
			writeDerivedAttributes(config, document, hawkElement);
			/** </derivedAttributes> */

			/** <indexedAttributes> */
			writeIndexedAttributes(config, document, hawkElement);
			/** </indexedAttributes> */

			/** <repositories> */
			writeRepositories(config, document, hawkElement);
			/** </repositories> */

			/** </hawk> */
			document.appendChild(hawkElement);

			writeXmlDocumentToFile(document, config.getFileName());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeDelay(HawkInstanceConfig config, Document document, Element hawkElement) {
		
		Element delayElement = document.createElement(DELAY);
		
		createAndAddAttribute(document, delayElement, MAX, config.getDelayMax());
		createAndAddAttribute(document, delayElement, MIN, config.getDelayMin());
		
		hawkElement.appendChild(delayElement); // add to tree
	}

	private void writePlugins(HawkInstanceConfig config, Document document, Element hawkElement) {
		if(config.getPlugins() != null && !config.getPlugins().isEmpty()) {
			Element pluginsElement = document.createElement(PLUGINS);
			for(String plugin : config.getPlugins()) {
				
				/** <plugins> */
				Element pluginElement = document.createElement(PLUGIN);
				createAndAddAttribute(document, pluginElement, NAME, plugin);
				
				pluginsElement.appendChild(pluginElement); // add to tree
				/** </plugins> */
			}

			hawkElement.appendChild(pluginsElement); // add to tree
		}
	}

	private void writeRepositories(HawkInstanceConfig config, Document document, Element hawkElement) {

		if(config.getRepositories() != null && !config.getRepositories().isEmpty()) {

			Element repositoriesElement = document.createElement(REPOSITORIES);
			for(RepositoryParameters params : config.getRepositories()) {
				
				/**	<repository> */
				Element repositoryElement = document.createElement(REPOSITORY);

				createAndAddAttribute(document, repositoryElement, TYPE, params.getType());
				createAndAddAttribute(document, repositoryElement, LOCATION, params.getLocation());
				createAndAddAttribute(document, repositoryElement, USER, params.getUser());
				createAndAddAttribute(document, repositoryElement, PASS, params.getPass());
				createAndAddAttribute(document, repositoryElement, FROZEN, params.isFrozen());

				repositoriesElement.appendChild(repositoryElement); // add to tree
				
				/**	</repository> */
			}

			// build tree
			hawkElement.appendChild(repositoriesElement);
		}
	}

	private void writeIndexedAttributes(HawkInstanceConfig config, Document document, Element hawkElement) {

		if(config.getIndexedAttributes() != null && !config.getIndexedAttributes().isEmpty()) {

			Element indexedAttributesElement = document.createElement(INDEXED_ATTRIBUTES);
			
			for(IndexedAttributeParameters params : config.getIndexedAttributes()) {
				/** <indexedAttribute> */
				Element indexedAttributeElement = document.createElement(INDEXED_ATTRIBUTE);

				createAndAddAttribute(document, indexedAttributeElement, METAMODEL_URI, params.getMetamodelUri());
				createAndAddAttribute(document, indexedAttributeElement, TYPE_NAME, params.getTypeName());
				createAndAddAttribute(document, indexedAttributeElement, ATTRIBUTE_NAME, params.getAttributeName());

				indexedAttributesElement.appendChild(indexedAttributeElement); // add to tree
				/** </indexedAttribute> */
			}

			hawkElement.appendChild(indexedAttributesElement); // add to tree
		}
	}

	private void writeDerivedAttributes(HawkInstanceConfig config, Document document, Element hawkElement) {
		if(config.getDerivedAttributes() != null && !config.getDerivedAttributes().isEmpty()) {
			Element derivedAttributesElement = document.createElement(DERIVED_ATTRIBUTES);
			for(DerivedAttributeParameters params : config.getDerivedAttributes()) {
				/** <derivedAttribute> */
				Element derivedAttributeElement = document.createElement(DERIVED_ATTRIBUTE);

				createAndAddAttribute(document, derivedAttributeElement, METAMODEL_URI, params.getMetamodelUri());
				createAndAddAttribute(document, derivedAttributeElement, TYPE_NAME, params.getTypeName());
				createAndAddAttribute(document, derivedAttributeElement, ATTRIBUTE_NAME, params.getAttributeName());
				createAndAddAttribute(document, derivedAttributeElement, ATTRIBUTE_TYPE, params.getAttributeType());
				createAndAddAttribute(document, derivedAttributeElement, IS_MANY, params.isMany());
				createAndAddAttribute(document, derivedAttributeElement, IS_UNIQUE, params.isUnique());
				createAndAddAttribute(document, derivedAttributeElement, IS_ORDERED, params.isOrdered());

				/** <derivation> */
				Element derivationElement = document.createElement(DERIVATION);
				createAndAddAttribute(document, derivationElement, LANGUAGE, params.getDerivationLanguage());

				/** <logic> */
				Element derivationLogicElement = document.createElement(LOGIC);
				/** ![CDATA[ */
				CDATASection derivationLogicCData = document.createCDATASection(params.getDerivationLogic());

				derivationLogicElement.appendChild(derivationLogicCData); // add to tree
				/** ]]> */
				
				derivationElement.appendChild(derivationLogicElement); // add to tree
				/** </logic> */
				
				derivedAttributeElement.appendChild(derivationElement); // add to tree
				/** <derivation> */
				
				derivedAttributesElement.appendChild(derivedAttributeElement); // add to tree
				/** </derivedAttribute> */
			}

			hawkElement.appendChild(derivedAttributesElement); // add to tree
		}
	}

	private void writeMetamodels(HawkInstanceConfig config, Document document, Element hawkElement) {
		if(config.getMetamodels() != null && !config.getMetamodels().isEmpty()) {
			Element metamodelsElement = document.createElement(METAMODELS);

			for(MetamodelParameters metamodel : config.getMetamodels()) {
				/** <metamodel> */
				Element metamodelElement = document.createElement(METAMODEL);
				createAndAddAttribute(document, metamodelElement, LOCATION, metamodel.getLocation());
				createAndAddAttribute(document, metamodelElement, URI, metamodel.getUri());

				metamodelsElement.appendChild(metamodelElement); // add to tree
				/** </metamodel> */
			}

			hawkElement.appendChild(metamodelsElement); // add to tree
		}
	}

	private void readDerivedAttributes(NodeList nodes, HawkInstanceConfig config) {
		for(Element derivedAttributesElement : ElementListIterable(nodes)) {
			for(Element derivedAttributeElement : ElementListIterable(derivedAttributesElement.getElementsByTagName(DERIVED_ATTRIBUTE))) {
				DerivedAttributeParameters params = new DerivedAttributeParameters(
				derivedAttributeElement.getAttribute(METAMODEL_URI),
				derivedAttributeElement.getAttribute(TYPE_NAME),
				derivedAttributeElement.getAttribute(ATTRIBUTE_NAME),
				derivedAttributeElement.getAttribute(ATTRIBUTE_TYPE),
				Boolean.valueOf(derivedAttributeElement.getAttribute(IS_MANY)),
				Boolean.valueOf(derivedAttributeElement.getAttribute(IS_UNIQUE)),
				Boolean.valueOf(derivedAttributeElement.getAttribute(IS_ORDERED)));
				// get derivation

				Element derivationElement = getFirstElement(derivedAttributeElement.getElementsByTagName(DERIVATION));
				if(derivationElement != null) {
					// get derivation language
					params.setDerivationLanguage(derivationElement.getAttribute(LANGUAGE));

					// get logic
					Element logic = getFirstElement(derivationElement.getElementsByTagName(LOGIC));
					if(logic != null) {
						params.setDerivationLogic(readElementCDataValue(logic));
					}
				}

				config.getDerivedAttributes().add(params);
			}
		}			

	}

	private String readElementCDataValue(Node node) {
		StringBuffer buffer = new StringBuffer();

		NodeList cDataElements = node.getChildNodes();
		// CDATA sections , we cannot use ElementListIterable
		
		for(Node cDataElement : NodeListIterable(cDataElements)) {
			buffer.append(cDataElement.getNodeValue());
		}

		return buffer.toString();
	}

	private void readIndexedAttributes(NodeList nodes, HawkInstanceConfig config) {
		for(Element indexedAttributeElements : ElementListIterable(nodes)) {
			for(Element indexedAttributeElement : ElementListIterable(indexedAttributeElements.getElementsByTagName(INDEXED_ATTRIBUTE))) {
				IndexedAttributeParameters params = new IndexedAttributeParameters(indexedAttributeElement.getAttribute(METAMODEL_URI), 
						indexedAttributeElement.getAttribute(TYPE_NAME), 
						indexedAttributeElement.getAttribute(ATTRIBUTE_NAME));

				config.getIndexedAttributes().add(params);
			}
		}		

	}

	private void readRepositories(NodeList nodes, HawkInstanceConfig config) {
		for(Element repoElements : ElementListIterable(nodes)) {
			for(Element repoElement : ElementListIterable( repoElements.getElementsByTagName(REPOSITORY))) {
				RepositoryParameters params = new RepositoryParameters(
						repoElement.getAttribute(TYPE),
						repoElement.getAttribute(LOCATION),
						repoElement.getAttribute(USER),
						repoElement.getAttribute(PASS),
						Boolean.valueOf(repoElement.getAttribute(FROZEN)));

				config.getRepositories().add(params);
			}
		}		
	}

	private void readMetamodels(NodeList nodes, HawkInstanceConfig config) {
		for(Element metamodelElements : ElementListIterable(nodes)) {
			for(Element metamodelElement : ElementListIterable(( metamodelElements).getElementsByTagName(METAMODEL))) {
				MetamodelParameters params = new MetamodelParameters(metamodelElement.getAttribute(URI),
						metamodelElement.getAttribute(LOCATION));

				config.getMetamodels().add(params);
			}
		}		
	}

	private void readDelay(NodeList nodes, HawkInstanceConfig config) {
		// only one element is expected
		Element element = getFirstElement(nodes);
		if(element != null) {
			config.setDelayMax(Integer.valueOf(element.getAttribute(MAX)));
			config.setDelayMin(Integer.valueOf(element.getAttribute(MIN)));
		}
	}
	
	private void readPlugins(NodeList nodes, HawkInstanceConfig config) {
		for(Element pluginsElement : ElementListIterable(nodes)) {
			for(Element pluginElement : ElementListIterable(pluginsElement.getElementsByTagName(PLUGIN))) {
				config.getPlugins().add(pluginElement.getAttribute(NAME));
			}
		}			
	}
	
	/** Utility methods */
	private void writeXmlDocumentToFile(Node node, String filename) {
		try {
			File file = new File(filename);
			if(!file.exists()) {
				file.createNewFile();
			}

			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS implementationLS = 	(DOMImplementationLS)registry.getDOMImplementation("LS");

			LSOutput output = implementationLS.createLSOutput();
			output.setEncoding("UTF-8");
			output.setCharacterStream(new FileWriter(file));

			LSSerializer serializer = implementationLS.createLSSerializer();
			serializer.getDomConfig().setParameter("format-pretty-print",true);

			serializer.write(node, output);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void createAndAddAttribute(Document document, Element element, String tagName,
			boolean value) {
		createAndAddAttribute(document, element, tagName, String.valueOf(value));

	}

	private void createAndAddAttribute(Document document, Element element, String tagName,
			String value) {
		if(value != null) {
			Attr attr = document.createAttribute(tagName);
			attr.setValue(value);
			element.setAttributeNode(attr);
		}
	}

	private void createAndAddAttribute(Document document, Element element, String tagName, int value) {
		createAndAddAttribute(document, element, tagName, String.valueOf(value));
	}

	private Element getFirstElement(NodeList n) {
		if (n.getLength() > 0)
			return (Element) n.item(0);
		return null;
	}


	public static Iterable<Element> ElementListIterable(final NodeList n) {
		return new Iterable<Element>() {

			@Override
			public Iterator<Element> iterator() {

				return new Iterator<Element>() {

					int index = 0;

					@Override
					public boolean hasNext() {
						return index < n.getLength();
					}

					@Override
					public Element next() {
						if (hasNext()) {
							return (Element) n.item(index++);
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

