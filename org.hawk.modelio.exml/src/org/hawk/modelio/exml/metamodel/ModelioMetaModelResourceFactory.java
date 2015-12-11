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
package org.hawk.modelio.exml.metamodel;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;

public class ModelioMetaModelResourceFactory implements IMetaModelResourceFactory {

	private ModelioMetaModelResource modelioMetamodel;

	private ModelioMetaModelResource getMetamodel() {
		if (modelioMetamodel == null) {
			modelioMetamodel = new ModelioMetaModelResource(this);
		}
		return modelioMetamodel;
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio static metamodel factory";
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		return getMetamodel();
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		return Collections.singleton((IHawkMetaModelResource) getMetamodel());
	}

	@Override
	public void shutdown() {
		modelioMetamodel = null; 
	}

	@Override
	public boolean canParse(File f) {
		return false;
	}

	@Override
	public Collection<String> getMetaModelExtensions() {
		return Collections.emptySet();
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {
		return getMetamodel();
	}

	@Override
	public void removeMetamodel(String property) {
		// ignore
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		// unsupported
		return "";
	}

}
