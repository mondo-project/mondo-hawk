/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial implementation
 ******************************************************************************/
package org.hawk.ifc;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.Deserializer;
import org.hawk.ifc.IFCModelFactory.IFCModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IFCZippedModelResource extends IFCAbstractModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(IFCZippedModelResource.class);

	private ZipFile zipFile;
	private List<ZipEntry> ifcEntries;

	public IFCZippedModelResource(ZipFile zf, List<ZipEntry> ifcEntries, IFCModelFactory ifcModelFactory,
			IFCModelType type) {
		super(ifcModelFactory, type);
		this.zipFile = zf;
		this.ifcEntries = ifcEntries;
	}

	@Override
	public void unload() {
		try {
			zipFile.close();
		} catch (IOException e) {
			LOGGER.error("Could not close the zip file", e);
		}
		zipFile = null;
		ifcEntries = null;
	}

	@Override
	protected IfcModelInterface readModel(Deserializer d) throws DeserializeException, IOException {
		// The factory only reports a non-unknown type if the zip has at least
		// one .ifc* file, so this should be safe.
		final ZipEntry first = ifcEntries.get(0);

		return d.read(zipFile.getInputStream(first), first.getName(), first.getSize());
	}

	@Override
	public boolean providesSingletonElements() {
		return false;
	}

}
