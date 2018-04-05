/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IFCModelFactory implements IModelResourceFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(IFCModelFactory.class);

	private static final class FileInputStreamFactory implements InputStreamFactory {
		private File file;

		public FileInputStreamFactory(File f) {
			this.file = f;
		}

		@Override
		public InputStream createStream() throws FileNotFoundException {
			return new FileInputStream(file);
		}
	}

	private interface InputStreamFactory {
		InputStream createStream() throws Exception;
	}

	static enum IFCModelType {
		IFC2X2_STEP,
		IFC2X2_XML,
		IFC2X3_STEP,
		IFC2X3_XML,
		IFC4_STEP,
		IFC4_XML,
		UNKNOWN
	}

	private static final Set<String> EXTENSIONS = new HashSet<String>(Arrays.asList(
			".ifc", ".ifcxml", ".ifc.txt", ".ifcxml.txt", ".ifc.zip", ".ifczip"
	));

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(IFileImporter importer, File f) {
		try {
			final ZipFile zf = new ZipFile(f);
			final List<ZipEntry> candidates = getIFCFilesInZIP(zf);
			final IFCModelType type = getIFCZipModelType(f, zf, candidates);
			return new IFCZippedModelResource(zf, candidates, this, type);
		} catch (Exception e) {
			// this isn't a zip, go on
			LOGGER.debug("File {} is not a zipfile or failed to be read", f);
		}

		final IFCModelType type = getIFCStepOrXMLModelType(new FileInputStreamFactory(f));
		return new IFCModelResource(f, this, type);
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

	@Override
	public String getHumanReadableName() {
		return "BIM IFC Parser for Hawk";
	}

	IFCModelType getIFCModelType(File f) {
		final IFCModelType zipModelType = getIFCZipModelType(f);
		if (zipModelType != null) {
			return zipModelType;
		}
		return getIFCStepOrXMLModelType(new FileInputStreamFactory(f));
	}

	/**
	 * Returns the IFC model type contained in the <code>f</code> zipfile. If it
	 * is a zipfile but it doesn't have any recognizable IFC files in it, it
	 * returns {@link IFCModelType#UNKNOWN}. If it has more than one, it logs a
	 * warning and uses the type of the first one anyway. If it is not a
	 * zipfile, it returns <code>null</code>.
	 */
	private IFCModelType getIFCZipModelType(File f) {
		try (final ZipFile zf = new ZipFile(f)) {
			final List<ZipEntry> candidates = getIFCFilesInZIP(zf);
			return getIFCZipModelType(f, zf, candidates);
		} catch (IOException e) {
			LOGGER.debug("Could not parse {} as a zip file", f);
		}
		return null;
	}

	protected IFCModelType getIFCZipModelType(File f, final ZipFile zf,
			final List<ZipEntry> candidates) {
		if (!candidates.isEmpty()) {
			if (candidates.size() > 1) {
				LOGGER.warn("The ifcZIP file {} has more than one .ifc* file: {}. Will use the first one.", f, candidates);
			}
			final ZipEntry ifcEntry = candidates.get(0);
			return getIFCStepOrXMLModelType(new InputStreamFactory() {
				@Override
				public InputStream createStream() throws Exception {
					return zf.getInputStream(ifcEntry);
				}
			});
		} else {
			// it's a zip file but it doesn't contain any .ifc files.
			LOGGER.warn("The ifcZIP file {} has no .ifc* files", f);
			return IFCModelType.UNKNOWN;
		}
	}

	private List<ZipEntry> getIFCFilesInZIP(final ZipFile zf) {
		final Enumeration<? extends ZipEntry> entries = zf.entries();
		final List<ZipEntry> candidates = new ArrayList<>();
		while (entries.hasMoreElements()) {
			final ZipEntry ze = entries.nextElement();
			final String lowerCaseName = ze.getName().toLowerCase();
			for (String ext : EXTENSIONS) {
				if (!ext.contains("zip") && lowerCaseName.endsWith(ext)) {
					candidates.add(ze);
				}
			}
		}
		return candidates;
	}

	private IFCModelType getIFCStepOrXMLModelType(InputStreamFactory isf) {
		try {
			IFCModelType type = getIFCStepModelType(isf.createStream());
			if (type == IFCModelType.UNKNOWN) {
				type = getIFCXMLModelType(isf.createStream());
			}
			return type;
		} catch (Exception e) {
			LOGGER.error("Could not read the file", e);
			return IFCModelType.UNKNOWN;
		}
	}

	private IFCModelType getIFCXMLModelType(InputStream is) {
		try (final Reader fReader = new BufferedReader(new InputStreamReader(is))) {
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
			LOGGER.error("Could not parse as XML", e);
		}

		return IFCModelType.UNKNOWN;
	}

	@SuppressWarnings("resource")
	private IFCModelType getIFCStepModelType(InputStream is) throws IOException {
		// Try first with the STEP-based formats
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
		}
		return IFCModelType.UNKNOWN;
	}

}
