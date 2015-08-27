/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;

public class IFCModelFactory implements IModelResourceFactory {

	static enum IFCModelType {
		IFC2X2_STEP,
		IFC2X2_XML,
		IFC2X3_STEP,
		IFC2X3_XML,
		IFC4_STEP,
		IFC4_XML,
		UNKNOWN
	}

	private final String metamodeltype = "com.googlecode.hawk.emf.metamodel.EMFMetaModelParser";
	private static final Set<String> EXTENSIONS = new HashSet<String>(Arrays.asList(".ifc", ".ifcxml", ".ifc.txt", ".ifcxml.txt"));

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(File f) {
		return new IFCModelResource(f, this);
	}

	@Override
	public void shutdown() {
		// nothing to do
	}

	@Override
	public Set<String> getModelExtensions() {
		return EXTENSIONS;
	}

	@Override
	public boolean canParse(File f) {
		final IFCModelType type = getIFCModelType(f);
		switch (type) {
		case IFC2X3_STEP:
		case IFC2X3_XML:
		case IFC4_STEP:
		case IFC4_XML:
			// BIMserver 1.4.0 cannot parse IFC2x2
			return true;
		default:
			return false;
		}
	}

	IFCModelType getIFCModelType(File f) {
		// Try first with the STEP-based formats
		try (final BufferedReader reader = new BufferedReader(new FileReader(f))) {
			// Read the first line to check if this is in STEP format
			String line = reader.readLine().trim();
			if ("ISO-10303-21;".equals(line)) {
				// This is in STEP format: now look for a FILE_SCHEMA line
				line = reader.readLine();
				while (line != null && !line.contains("ENDSEC;")) {
					if (line.startsWith("FILE_SCHEMA")) {
						if (line.contains("IFC2X3")) {
							return IFCModelType.IFC2X3_STEP;
						}
						else if (line.contains("IFC2X2")) {
							return IFCModelType.IFC2X2_STEP;
						}
						else if (line.contains("IFC4")) {
							return IFCModelType.IFC4_STEP;
						}
						else {
							return IFCModelType.UNKNOWN;
						}
					}
					line = reader.readLine();
				}
			}
		} catch (IOException e) {
			// We couldn't read the file at all - log the error and report it as unknown
			e.printStackTrace();
			return IFCModelType.UNKNOWN;
		}

		// Try the XML-based formats now (use StAX to avoid using up too much memory)
		try (final Reader fReader = new FileReader(f)) {
			final XMLInputFactory factory = XMLInputFactory.newInstance();
			final XMLEventReader rawXmlReader = factory.createXMLEventReader(fReader);
			final XMLEventReader xmlReader = factory.createFilteredReader(rawXmlReader, new EventFilter() {
				@Override
				public boolean accept(XMLEvent event) {
					if (event.isStartElement()) {
						final StartElement e = event.asStartElement();
						final String localPart = e.getName().getLocalPart();
						return "ifcXML".equals(localPart) || "iso_10303_28".equals(localPart) || "uos".equals(localPart);
					}
					return false;
				}
			});

			XMLEvent mainTagEvent = xmlReader.nextTag();
			if (mainTagEvent != null) {
				final String mainTagLocalPart = mainTagEvent.asStartElement().getName().getLocalPart();
				if ("iso_10303_28".equals(mainTagLocalPart)) {
					// This is an IFC2x3 XML document: look for the <uos> element now
					XMLEvent uosEvent = xmlReader.nextTag();
					if (uosEvent != null) {
						final String configurationValue = uosEvent.asStartElement().getAttributeByName(new QName(null, "configuration")).getValue();
						if (configurationValue.contains("ifc2x3")) {
							return IFCModelType.IFC2X3_XML;
						} else if (configurationValue.contains("ifc2x2")) {
							return IFCModelType.IFC2X2_XML;
						}
					}
				}
				else if ("ifcXML".equals(mainTagLocalPart)) {
					return IFCModelType.IFC4_XML;
				}
			}
		} catch (XMLStreamException | FactoryConfigurationError | IOException e) {
			// We couldn't parse this as XML either
			e.printStackTrace();
		}

		return IFCModelType.UNKNOWN;
	}

	public String getMetaModelType() {
		return metamodeltype;
	}

	@Override
	public String getHumanReadableName() {
		return "BIM IFC Parser for Hawk";
	}
}
