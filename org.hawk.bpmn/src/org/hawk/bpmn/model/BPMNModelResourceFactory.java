/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.bpmn.model;

import java.io.File;
import java.util.HashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;

public class BPMNModelResourceFactory implements IModelResourceFactory {

	public static void main(String[] args) throws Exception {

		BPMNModelResourceFactory f = new BPMNModelResourceFactory();
		BPMNMetaModelResourceFactory mf = new BPMNMetaModelResourceFactory();
		mf.parse(new File(
				"C:\\Users\\kb\\Desktop\\workspace\\org.hawk.emf\\src\\org\\hawk\\emf\\metamodel\\examples\\single\\JDTAST.ecore"));
		f.parse(new File(
				"C:/Users/kb/Desktop/workspace/org.hawk.emf/src/org/hawk/emf/model/examples/single/0/set0.xmi"));

	}

	String type = "org.hawk.emf.metamodel.BPMNModelParser";
	String hrn = "BPMN Model Resource Factory";

	// String metamodeltype =
	// "org.hawk.emf.metamodel.EMFMetaModelParser";
	HashSet<String> metamodelExtensions;
	HashSet<String> modelExtensions;

	ResourceSet resourceSet = null;

	public BPMNModelResourceFactory() {
		metamodelExtensions = new HashSet<String>();
		modelExtensions = new HashSet<String>();

		// metamodelExtensions.add("ecore");
		// metamodelExtensions.add("ECORE");
		modelExtensions.add("bpmn2");
		modelExtensions.add("BPMN2");

		if (resourceSet == null) {
			resourceSet = new ResourceSetImpl();
			resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI,
					EcorePackage.eINSTANCE);
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

			if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
				EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI,
						EcorePackage.eINSTANCE);
			}

			// determinePackagesFrom(resourceSet);

			// Note that AbstractEmfModel#getPackageRegistry() is not usable
			// yet, as
			// modelImpl is not set

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
			e.printStackTrace();
			ret = null;
		}

		return ret;

		// FIXME possibly keep metadata about failure to aid users

	}

	// @Override
	// public String printregistry() {
	// return EPackage.Registry.INSTANCE.keySet().toString();
	// }

	// @Override
	// public List<String> getepackageuris() {
	//
	// List<String> l = new LinkedList<String>();
	//
	// for (Object pp : EPackage.Registry.INSTANCE.keySet().toArray()) {
	// String p = pp.toString();
	// if (!p.contains("Ecore") && !p.contains("XMLType")) {
	// EPackage ep = EPackage.Registry.INSTANCE.getEPackage(p);
	// l.add(ep.getNsURI());
	// }
	// }
	// return l;
	//
	// }

	@Override
	public void shutdown() {
		type = null;
		metamodelExtensions = null;
		modelExtensions = null;
		// resourceSet = null;
	}

	@Override
	public HashSet<String> getModelExtensions() {

		return modelExtensions;

	}

	@Override
	public boolean canParse(File f) {

		String[] split = f.getPath().split("\\.");
		String extension = split[split.length - 1];

		return getModelExtensions().contains(extension);

	}

}
