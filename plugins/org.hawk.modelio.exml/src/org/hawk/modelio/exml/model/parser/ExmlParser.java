/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.model.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
	 * @param container
	 *            File that should be associated with the read contents.
	 * @param is
	 *            Input stream with the contents of the <code>.exml</code> file.
	 *            The caller of this function is responsible for closing the
	 *            {@link InputStream}.
	 * @throws XMLStreamException
	 *             There was an error while parsing the input stream.
	 * @throws NoSuchElementException
	 *             No objects are present in the input stream.
	 */
	public ExmlObject getObject(File container, InputStream is) throws XMLStreamException, FactoryConfigurationError {
		final XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(is);
		try {
			skipUntilElementStarts(reader, "OBJECT");
			final ExmlObject exmlObj = new ExmlObject(container);
			fillObject(reader, exmlObj);
			return exmlObj;
		} finally {
			reader.close();
		}
	}

	/**
	 * Returns an iterable object with all the {@link ExmlObject} instances in
	 * the archive. The zipfile is re-read upon each iteration, to save on
	 * memory, and is closed automatically once the iterator has been fully
	 * consumed.
	 *
	 * @param fZip
	 *            File that should be associated with the read contents.
	 */
	public Iterable<ExmlObject> getObjects(final File fZip) {
		return new Iterable<ExmlObject>() {
			@Override
			public Iterator<ExmlObject> iterator() {
				try {
					final ZipFile zipFile = new ZipFile(fZip);
					final Enumeration<? extends ZipEntry> entries = zipFile.entries();
					return new Iterator<ExmlObject>() {
						ExmlObject nextObject = null;
						boolean closed = false;

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
							while (!closed && nextObject == null && entries.hasMoreElements()) {
								ZipEntry entry = entries.nextElement();
								if (entry.getName().toLowerCase().endsWith(".exml")) {
									try (InputStream is = zipFile.getInputStream(entry)) {
										nextObject = getObject(fZip, is);
									} catch (IOException | XMLStreamException | FactoryConfigurationError e) {
										LOGGER.error("Could not parse entry " + entry.getName() + " in "
												+ zipFile.getName() + ": skipping", e);
									}
								}
							}
							if (nextObject == null) {
								try {
									closed = true;
									zipFile.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							return nextObject;
						}

					};
				} catch (IOException e1) {
					e1.printStackTrace();
					return Collections.emptyIterator();
				}
			}
		};
	}

	/**
	 * Fills in the {@link ExmlObject} from the contents of the
	 * {@link XMLEventReader}. The last event returned by the reader should have
	 * been the startElement of the <code>OBJECT</code> tag.
	 */
	private void fillObject(XMLEventReader reader, ExmlObject exmlObj) throws XMLStreamException {
		String currentLink = null;
		String currentComp = null;

		XMLEvent ev;
		do {
			ev = skipUntilElement(reader);
			if (ev.isStartElement()) {
				final StartElement evStart = ev.asStartElement();
				switch (evStart.getName().getLocalPart()) {
				case "ID":
				case "FOREIGNID":
				case "EXTID":
					if (currentLink != null) {
						// ID inside <LINK>
						final ExmlReference ref = new ExmlReference(exmlObj.getFile());
						fillInReference(evStart, ref);
						exmlObj.addToLink(currentLink, ref);
					} else if (currentComp != null) {
						// ID inside <COMP> (e.g. inside a <REFOBJ>)
						final ExmlReference ref = new ExmlReference(exmlObj.getFile());
						fillInReference(evStart, ref);
						exmlObj.addToComposition(currentComp, ref);
					} else {
						fillInReference(evStart, exmlObj);
					}
					break;
				case "PID":
					exmlObj.setParentName(getAttribute(evStart, "name"));
					exmlObj.setParentMClassName(getAttribute(evStart, "mc"));
					exmlObj.setParentUID(getAttribute(evStart, "uid"));
					break;
				case "COMPID":
					final ExmlReference ref = new ExmlReference(exmlObj.getFile());
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
					if (currentComp != null) {
						final ExmlObject compObj = new ExmlObject(exmlObj.getFile());
						fillObject(reader, compObj);
						exmlObj.addToComposition(currentComp, compObj);
					} else {
					    throw new IllegalArgumentException("Unexpected <OBJECT> outside a composition");
					}
					break;
				}
			} else if (ev.isEndElement()) {
				switch (ev.asEndElement().getName().getLocalPart()) {
				case "LINK":
					currentLink = null;
					break;
				case "COMP":
					currentComp = null;
					break;
				}
			}
		} while (!ev.isEndElement() || !ev.asEndElement().getName().getLocalPart().equals("OBJECT"));
	}

	private void fillInReference(final StartElement evStart, ExmlReference ref) {
		if (ref.getName() == null) {
			ref.setName(getAttribute(evStart, "name"));
			ref.setMClassName(getAttribute(evStart, "mc"));
			ref.setUID(getAttribute(evStart, "uid"));
		} else {
			System.err.println("WARNING: tried to overwrite reference " + ref);
		}
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
