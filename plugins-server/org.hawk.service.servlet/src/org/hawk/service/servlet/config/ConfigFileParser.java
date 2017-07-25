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
import java.io.StringWriter;
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
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
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

	public HawkInstanceConfig parse(File file) {
		HawkInstanceConfig config = null;
		try {
			Element element = getXmlDocumentRootElement(file);
			config = new HawkInstanceConfig(file.getAbsolutePath());
			parseConfig(element, config);
		} catch (Exception e) {
			System.err.print("error in parse(File file): ");
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
		Document document  = builder.parse(file);
		Element element  = document.getDocumentElement();
		return element;
	}

	private void parseConfig(Element element , HawkInstanceConfig config) {
		if(element.getNodeName().equals(HAWK_SERVER_CONFIGURATION)) {
			// get hawk, usually one element per file
			// FIXME if there is more than one, only the last one will be returned
			for(Node hawkElement : NodeListIterable(element.getElementsByTagName(HAWK))) {

				config.setName(((Element)hawkElement).getAttribute(NAME));

				config.setBackend(((Element)hawkElement).getAttribute(BACKEND));

				readDelay(((Element)hawkElement).getElementsByTagName(DELAY),  config);

				readPlugins(((Element)hawkElement).getElementsByTagName(PLUGINS),  config);

				readMetamodels(((Element)hawkElement).getElementsByTagName(METAMODELS),  config);

				readRepositories(((Element)hawkElement).getElementsByTagName(REPOSITORIES),  config);

				readIndexedAttributes(((Element)hawkElement).getElementsByTagName(INDEXED_ATTRIBUTES),  config);

				readDerivedAttributes(((Element)hawkElement).getElementsByTagName(DERIVED_ATTRIBUTES),  config);
			}
		}  
	}


	public void saveConfigAsXml(HawkInstanceConfig config) {
		try {

			DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();

			Document document  = builder.newDocument();

			/** <HawkServerConfiguration> */
			Element root = document.createElement(HAWK_SERVER_CONFIGURATION);

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
			root.appendChild(hawkElement);

			/** </HawkServerConfiguration> */
			document.appendChild(root);

			writeXmlDocumentToFile(document, config.getFileName());

		} catch (ParserConfigurationException e) {
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
				createAndAddAttribute(document, indexedAttributeElement, NAME, params.getAttributeName());

				indexedAttributesElement.appendChild(indexedAttributeElement); // add to tree
				/** </indexedAttribute> */
			}

			hawkElement.appendChild(indexedAttributesElement); // add to tree
		}
	}

	private void writeDerivedAttributes(HawkInstanceConfig config,
			Document document, Element hawkElement) {
		if(config.getDerivedAttributes() != null && !config.getDerivedAttributes().isEmpty()) {
			Element derivedAttributesElement = document.createElement(DERIVED_ATTRIBUTES);
			for(DerivedAttributeParameters params : config.getDerivedAttributes()) {
				/** <derivedAttribute> */
				Element derivedAttributeElement = document.createElement(DERIVED_ATTRIBUTE);

				createAndAddAttribute(document, derivedAttributeElement, METAMODEL_URI, params.getMetamodelUri());
				createAndAddAttribute(document, derivedAttributeElement, TYPE_NAME, params.getTypeName());
				createAndAddAttribute(document, derivedAttributeElement, NAME, params.getAttributeName());
				createAndAddAttribute(document, derivedAttributeElement, TYPE, params.getAttributeType());
				createAndAddAttribute(document, derivedAttributeElement, IS_MANY, params.isMany());
				createAndAddAttribute(document, derivedAttributeElement, IS_UNIQUE, params.isUnique());
				createAndAddAttribute(document, derivedAttributeElement, IS_ORDERED, params.isOrdered());

				/** <derivation> */
				Element derivationElement = document.createElement(DERIVATION);
				createAndAddAttribute(document, derivationElement, LANGUAGE, params.getDerivationLanguage());

				/** <logic> */
				Element derivationLogicElement = document.createElement(LOGIC);
				/** ![CDATA[ */
				CDATASection derivationLogicCDATA = document.createCDATASection(params.getDerivationLogic());

				derivationLogicElement.appendChild(derivationLogicCDATA); // add to tree
				/** ]]> */
				
				derivedAttributeElement.appendChild(derivationLogicElement); // add to tree
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

	private void readIndexedAttributes(NodeList nodes, HawkInstanceConfig config) {
		for(Node indexedAttributeElements : NodeListIterable(nodes)) {
			for(Node indexedAttributeElement : NodeListIterable(((Element) indexedAttributeElements).getElementsByTagName(INDEXED_ATTRIBUTE))) {
				IndexedAttributeParameters params = new IndexedAttributeParameters(((Element)indexedAttributeElement).getAttribute(METAMODEL_URI), 
						((Element)indexedAttributeElement).getAttribute(TYPE_NAME), 
						((Element)indexedAttributeElement).getAttribute(ATTRIBUTE_NAME));

				config.getIndexedAttributes().add(params);
			}
		}		

	}

	private void readRepositories(NodeList nodes, HawkInstanceConfig config) {
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

	private void readMetamodels(NodeList nodes, HawkInstanceConfig config) {
		for(Node metamodelElements : NodeListIterable(nodes)) {
			for(Node metamodelElement : NodeListIterable(((Element) metamodelElements).getElementsByTagName(METAMODEL))) {
				MetamodelParameters params = new MetamodelParameters(((Element)metamodelElement).getAttribute(URI),
						((Element)metamodelElement).getAttribute(LOCATION));

				config.getMetamodels().add(params);
			}
		}		
	}

	private void readDelay(NodeList nodes, HawkInstanceConfig config) {
		// only one element is expected
		if(nodes.getLength() >= 1) {
			config.setDelayMax(Integer.valueOf(((Element) nodes.item(0)).getAttribute(MAX)));
			config.setDelayMin(Integer.valueOf(((Element) nodes.item(0)).getAttribute(MIN)));
		}
	}

	
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

	private void createAndAddAttribute(Document document, Element element, String tagName,
			int value) {
		createAndAddAttribute(document, element, tagName, String.valueOf(value));
	}

	private void readPlugins(NodeList nodes, HawkInstanceConfig config) {
		for(Node pluginsElement : NodeListIterable(nodes)) {
			for(Node pluginElement : NodeListIterable(((Element) pluginsElement).getElementsByTagName(PLUGIN))) {
				config.getPlugins().add(((Element)pluginElement).getAttribute(NAME));
			}
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

