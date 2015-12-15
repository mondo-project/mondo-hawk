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
package org.hawk.modelio.exml.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlParser;

public class ModelioModelResourceFactory implements IModelResourceFactory {

	private static final String EXML_EXT = ".exml";
	private static final Set<String> MODEL_EXTS = new HashSet<String>();
	static {
		MODEL_EXTS.add(EXML_EXT);
		MODEL_EXTS.add(".ramc");
		MODEL_EXTS.add(".modelio.zip");
	}

	private ModelioMetaModelResource metamodel;

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio exml-based model factory";
	}

	@Override
	public IHawkModelResource parse(File f) throws Exception {
		if (metamodel == null) {
			this.metamodel = new ModelioMetaModelResource(null);
		}

		if (f.getName().toLowerCase().endsWith(EXML_EXT)) {
			try (final FileInputStream fIS = new FileInputStream(f)) {
				final ExmlParser parser = new ExmlParser();
				final ExmlObject object = parser.getObject(f, fIS);
				return new ModelioModelResource(metamodel, object);
			}
		} else {
			try (final ZipFile zf = new ZipFile(f)) {
				final ExmlParser parser = new ExmlParser();
				final Iterable<ExmlObject> objects = parser.getObjects(f, zf);
				return new ModelioModelResource(metamodel, objects);
			}
		}
	}

	@Override
	public void shutdown() {
		metamodel = null;
	}

	@Override
	public boolean canParse(File f) {
		for (String ext : MODEL_EXTS) {
			if (f.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<String> getModelExtensions() {
		return MODEL_EXTS;
	}

}
