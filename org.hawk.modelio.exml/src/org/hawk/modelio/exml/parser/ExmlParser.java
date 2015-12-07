/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.parser;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parser for a single <code>.exml</code> file.
 */
public class ExmlParser {

	/**
	 * Parses a single <code>.exml</code> file and returns the
	 * {@link ExmlObject} inside it (which may contain other {@link ExmlObject}
	 * instances).
	 * 
	 * @param is
	 *            Input stream with the contents of the <code>.exml</code> file.
	 *            The caller of this function is responsible for closing the
	 *            {@link InputStream}.
	 * @throws XMLStreamException
	 *             There was an error while parsing the input stream.
	 * @throws NoSuchElementException
	 *             No objects are present in the input stream.
	 */
	public ExmlObject getObject(InputStream is) throws XMLStreamException, FactoryConfigurationError {
		final XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(is);
		try {
			skipUntilElementStarts(reader, "OBJECT");
			final ExmlObject exmlObj = new ExmlObject();
			fillObject(reader, exmlObj);
			return exmlObj;
		} finally {
			reader.close();
		}
	}

	/**
	 * Fills in the {@link ExmlObject} from the contents of the
	 * {@link XMLEventReader}. The last event returned by the reader should have
	 * been the startElement of the <code>OBJECT</code> tag.
	 */
	private void fillObject(XMLEventReader reader, ExmlObject exmlObj) throws XMLStreamException {
		String currentLink = "unknown";
		String currentComp = "unknown";
		Deque<String> elemStack = new ArrayDeque<>();
		elemStack.push("OBJECT");

		XMLEvent ev;
		do {
			ev = skipUntilElement(reader);
			if (ev.isStartElement()) {
				final StartElement evStart = ev.asStartElement();
				switch (evStart.getName().getLocalPart()) {
				case "ID":
				case "FOREIGNID":
				case "EXTID":
					switch (elemStack.peek()) {
					case "OBJECT":
						fillInReference(evStart, exmlObj);
						break;
					case "LINK":
						final ExmlReference ref = new ExmlReference();
						fillInReference(evStart, ref);
						exmlObj.addToLink(currentLink, ref);
						break;
					}
					break;
				case "PID":
					exmlObj.setParentName(getAttribute(evStart, "name"));
					exmlObj.setParentMClassName(getAttribute(evStart, "mc"));
					exmlObj.setParentUID(getAttribute(evStart, "uid"));
					break;
				case "COMPID":
					final ExmlReference ref = new ExmlReference();
					fillInReference(evStart, ref);
					exmlObj.addToComposition(currentComp, ref);
					break;
				case "ATT":
					exmlObj.setAttribute(getAttribute(evStart, "name"), reader.getElementText());
					break;
				case "LINK":
					currentLink = getAttribute(evStart, "relation");
					break;
				case "COMP":
					currentComp = getAttribute(evStart, "relation");
					break;
				case "OBJECT":
					switch (elemStack.peek()) {
					case "COMP":
						final ExmlObject compObj = new ExmlObject();
						fillObject(reader, compObj);
						exmlObj.addToComposition(currentComp, compObj);
						break;
					}
					break;
				}
				elemStack.push(evStart.getName().getLocalPart());
			} else if (ev.isEndElement()) {
				elemStack.pop();
			}
		} while (!ev.isEndElement() || !ev.asEndElement().getName().getLocalPart().equals("OBJECT"));
	}

	private void fillInReference(final StartElement evStart, ExmlReference ref) {
		ref.setName(getAttribute(evStart, "name"));
		ref.setMClassName(getAttribute(evStart, "mc"));
		ref.setUID(getAttribute(evStart, "uid"));
	}

	private String getAttribute(final StartElement evStart, final String attr) {
		return evStart.getAttributeByName(new QName(attr)).getValue();
	}

	private XMLEvent skipUntilElement(XMLEventReader reader) throws XMLStreamException {
		XMLEvent ev = reader.nextEvent();
		while (!ev.isStartElement() && !ev.isEndElement()) {
			ev = reader.nextEvent();
		}
		return ev;
	}

	private XMLEvent skipUntilElementStarts(XMLEventReader reader, final String localPart) throws XMLStreamException {
		XMLEvent ev = reader.nextEvent();
		while (!ev.isStartElement() || !ev.asStartElement().getName().getLocalPart().equals(localPart)) {
			ev = reader.nextEvent();
		}
		return ev;
	}
}
