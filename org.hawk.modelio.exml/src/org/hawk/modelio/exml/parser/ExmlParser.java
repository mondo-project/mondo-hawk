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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for a single <code>.exml</code> file or a ZIP archive containing <code>.exml</code> files.
 */
public class ExmlParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExmlParser.class);

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
	 * Returns an iterable object with all the {@link ExmlObject} instances in the archive.
	 */
	public Iterable<ExmlObject> getObjects(final ZipFile zipFile) {
		return new Iterable<ExmlObject>() {

			@Override
			public Iterator<ExmlObject> iterator() {
				final Enumeration<? extends ZipEntry> entries = zipFile.entries();

				return new Iterator<ExmlObject>(){
					ExmlObject nextObject = null;

					@Override
					public boolean hasNext() {
						return findNextObject(entries) != null;
					}

					@Override
					public ExmlObject next() {
						final ExmlObject ret = findNextObject(entries);
						nextObject = null;
						return ret;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					private ExmlObject findNextObject(final Enumeration<? extends ZipEntry> entries) {
						while (nextObject == null && entries.hasMoreElements()) {
							ZipEntry entry = entries.nextElement();
							if (entry.getName().toLowerCase().endsWith(".exml")) {
								try (InputStream is = zipFile.getInputStream(entry)) {
									nextObject = getObject(is);
								} catch (IOException | XMLStreamException | FactoryConfigurationError e) {
									LOGGER.error("Could not parse entry " + entry.getName() + " in " + zipFile.getName() + ": skipping", e);
								}
							}
						}
						return nextObject;
					}

				};
			}
			
		};
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