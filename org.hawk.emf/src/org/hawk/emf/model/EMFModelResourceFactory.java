/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.emf.model;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;

public class EMFModelResourceFactory implements IModelResourceFactory {

	/**
	 * Property that can be set to a comma-separated list of extensions (e.g. ".railway,.myext")
	 * that should be supported in addition to the default ones (".xmi" and ".model"). Composite
	 * extensions are allowed (e.g. ".rail.way").
	 */
	public static final String PROPERTY_EXTRA_EXTENSIONS = "org.hawk.emf.model.extraExtensions";

	public static void main(String[] args) throws Exception {
		EMFModelResourceFactory f = new EMFModelResourceFactory();
		EMFMetaModelResourceFactory mf = new EMFMetaModelResourceFactory();
		mf.parse(new File(
				"C:\\Users\\kb\\Desktop\\workspace\\org.hawk.emf\\src\\org\\hawk\\emf\\metamodel\\examples\\single\\JDTAST.ecore"));
		f.parse(new File(
				"C:/Users/kb/Desktop/workspace/org.hawk.emf/src/org/hawk/emf/model/examples/single/0/set0.xmi"));
	}

	String type = "org.hawk.emf.metamodel.EMFModelParser";
	String hrn = "EMF Model Resource Factory";

	Set<String> modelExtensions;

	public EMFModelResourceFactory() {
		modelExtensions = new HashSet<String>();
		modelExtensions.add(".xmi");
		modelExtensions.add(".model");

		final String sExtraExtensions = System.getProperty(PROPERTY_EXTRA_EXTENSIONS);
		if (sExtraExtensions != null) {
			String[] extraExtensions = sExtraExtensions.split(",");
			for (String extraExtension : extraExtensions) {
				modelExtensions.add(extraExtension);
			}
		}
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getHumanReadableName() {
		return hrn;
	}

	@Override
	public IHawkModelResource parse(File f) {

		IHawkModelResource ret;

		Resource r = null;

		try {
			ResourceSet resourceSet = new ResourceSetImpl();

			final Map<String, Object> extensionToFactoryMap
				= resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
			for (String ext : modelExtensions) {
				if (ext.startsWith(".")) {
					// Remove the initial period (if any)
					ext = ext.substring(1);
				}
				extensionToFactoryMap.put(ext, new XMIResourceFactoryImpl());
			}

			r = resourceSet.createResource(URI.createFileURI(f.getAbsolutePath()));
			r.load(null);
			ret = new EMFModelResource(r, this);
		} catch (Exception e) {
			System.err.print("error in parse(File f): ");
			System.err.println(e.getCause());
			e.printStackTrace();
			ret = null;
		}

		return ret;
		// FIXME possibly keep metadata about failure to aid users
	}

	@Override
	public void shutdown() {
	}

	@Override
	public Set<String> getModelExtensions() {
		return modelExtensions;
	}

	@Override
	public boolean canParse(File f) {
		String[] split = f.getPath().split("\\.");
		String extension = split[split.length - 1];
		return getModelExtensions().contains(extension);
	}
}
