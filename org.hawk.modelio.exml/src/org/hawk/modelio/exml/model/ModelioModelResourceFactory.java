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
import java.util.Collections;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlParser;

public class ModelioModelResourceFactory implements IModelResourceFactory {

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
		try (final FileInputStream fIS = new FileInputStream(f)) {
			final ExmlParser parser = new ExmlParser();
			final ExmlObject object = parser.getObject(fIS);
			return new ModelioModelResource(metamodel, object);
		}
	}

	@Override
	public void shutdown() {
		metamodel = null;
	}

	@Override
	public boolean canParse(File f) {
		return f.getName().endsWith(".exml");
	}

	@Override
	public Collection<String> getModelExtensions() {
		return Collections.singleton(".exml");
	}

}
