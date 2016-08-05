/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - provision of original EMF factory this (indirectly) extends
 *     Antonio Garcia Dominguez - update to use .zip archives of the projects
 ******************************************************************************/
package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.modelio.metamodel.data.MetamodelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelResourceFactory implements IModelResourceFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelResourceFactory.class);

	@Override
	public String getType() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(File zipFile) {
		return new ModelioModelResource(zipFile, getType());
	}

	@Override
	public void shutdown() {
		// nothing to do!
	}

	@Override
	public Set<String> getModelExtensions() {
		return new HashSet<String>(Arrays.asList(".modelio.xmi.zip"));
	}

	@Override
	public boolean canParse(File f) {
		if (!f.canRead() || !f.isFile() || !f.getName().endsWith("xmi.zip")) {
			return false;
		}

		// Make sure we have a .zip with the Modelio project.conf descriptor in its root folder
		try (final ZipFile zipFile = new ZipFile(f)) {
			final ZipEntry confEntry = zipFile.getEntry("project.conf");
			if (confEntry == null) {
				LOGGER.warn("'{}' does not contain a Modelio project description (/project.conf).", f);
				return false;
			}
		} catch (IOException e1) {
			LOGGER.error(String.format("Could not open '%s' as a zip file", f.getName()), e1);
			return false;
		}

		// Try loading the Modelio Metamodel
		MetamodelLoader.Load();

		return true;
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio parser for Hawk";
	}
}
