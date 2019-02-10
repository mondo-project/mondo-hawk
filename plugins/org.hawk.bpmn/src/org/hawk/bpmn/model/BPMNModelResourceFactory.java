/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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

	private static final String TYPE = "org.hawk.emf.metamodel.BPMNModelParser";
	private static final String HUMAN_READABLE_NAME = "BPMN Model Resource Factory";

	Set<String> modelExtensions;

	public BPMNModelResourceFactory() {
		modelExtensions = new HashSet<String>();
		modelExtensions.add(".bpmn");
		modelExtensions.add(".bpmn2");
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getHumanReadableName() {
		return HUMAN_READABLE_NAME;
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
