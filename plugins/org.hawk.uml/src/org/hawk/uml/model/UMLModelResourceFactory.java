/*******************************************************************************
 * Copyright (c) 2017-2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.uml.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.emf.model.EMFModelResource;
import org.hawk.uml.metamodel.UMLWrapperFactory;

public class UMLModelResourceFactory implements IModelResourceFactory {

	@Override
	public String getHumanReadableName() {
		return "UML Model Resource Factory";
	}

	@Override
	public IHawkModelResource parse(IFileImporter importer, File changedFile) throws Exception {
		ResourceSet rset = new ResourceSetImpl();
		UMLUtil.init(rset);
		Resource r = rset.createResource(URI.createFileURI(changedFile.getAbsolutePath()));
		r.load(null);

		return new EMFModelResource(r, new UMLWrapperFactory(), this);
	}

	@Override
	public void shutdown() {
		// nothing to do for now
	}

	@Override
	public boolean canParse(File f) {
		return f.getName().toLowerCase().endsWith(".uml");
	}

	@Override
	public Collection<String> getModelExtensions() {
		return Collections.singleton(".uml");
	}

}
