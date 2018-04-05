/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.bpmn.model;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.core.IFileImporter;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;

public class BPMNModelResourceFactory implements IModelResourceFactory {

	public static void main(String[] args) throws Exception {

		BPMNModelResourceFactory f = new BPMNModelResourceFactory();
		BPMNMetaModelResourceFactory mf = new BPMNMetaModelResourceFactory();
		mf.parse(new File(
				"C:\\Users\\kb\\Desktop\\workspace\\org.hawk.emf\\src\\org\\hawk\\emf\\metamodel\\examples\\single\\JDTAST.ecore"));
		f.parse(null, new File(
				"C:/Users/kb/Desktop/workspace/org.hawk.emf/src/org/hawk/emf/model/examples/single/0/set0.xmi"));

	}

	String type = "org.hawk.emf.metamodel.BPMNModelParser";
	String hrn = "BPMN Model Resource Factory";

	Set<String> modelExtensions;

	public BPMNModelResourceFactory() {
		modelExtensions = new HashSet<String>();
		modelExtensions.add(".bpmn");
		modelExtensions.add(".bpmn2");
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
	public IHawkModelResource parse(IFileImporter importer, File f) {

		IHawkModelResource ret;
		Resource r = null;

		try {

			// determinePackagesFrom(resourceSet);

			// Note that AbstractEmfModel#getPackageRegistry() is not usable
			// yet, as
			// modelImpl is not set

			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("bpmn", new Bpmn2ResourceFactoryImpl());
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("bpmn2", new Bpmn2ResourceFactoryImpl());

			r = resourceSet.createResource(URI.createFileURI(f.getPath()));
			r.load(null);

			// EmfModel m = new EmfModel();
			// m.setMetamodelUri(f.getPath());
			// m.loadModelFromUri();
			// r = m.getResource();

			ret = new BPMNModelResource(r, this);

		} catch (Exception e) {
			System.err.print("error in parse(File f): ");
			System.err.println(e.getCause());
			// e.printStackTrace();
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
		return getModelExtensions().contains("." + extension);
	}

}
