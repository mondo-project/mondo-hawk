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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;

public class EMFModelResourceFactory implements IModelResourceFactory {

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

	// String metamodeltype =
	// "org.hawk.emf.metamodel.EMFMetaModelParser";
	HashSet<String> metamodelExtensions;
	HashSet<String> modelExtensions;
	ResourceSet resourceSet = null;

	public EMFModelResourceFactory() {
		metamodelExtensions = new HashSet<String>();
		modelExtensions = new HashSet<String>();

		metamodelExtensions.add("ecore");
		metamodelExtensions.add("ECORE");
		modelExtensions.add("xmi");
		modelExtensions.add("XMI");
		modelExtensions.add("model");
		modelExtensions.add("MODEL");

		if (resourceSet == null) {
			resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("ecore", new EcoreResourceFactoryImpl());
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("*", new XMIResourceFactoryImpl());
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

	// @Override
	// public int registerMeta(File f) {
	//
	// int count = 0;
	//
	// // if (f.getName().endsWith(".ecore")) {
	// String[] path = f.getPath().replaceAll("\\\\", "/").split("/");
	// String[] file = path[path.length - 1].split("\\.");
	// String extension = file[file.length - 1];
	// if (getMetamodelExtensions().contains(extension)) {
	//
	// Resource metamodelResource = modelResourceSet.getResource(
	// URI.createFileURI(f.getAbsolutePath()), true);
	//
	// for (EObject oo : metamodelResource.getContents()) {
	// // for (EObject o : oo.eContents()) {//not adding
	// // packages ... solve!
	// if (oo instanceof EPackage)
	// count += new RegisterMeta().registerPackages((EPackage) oo);
	// // }
	// }
	// metamodelResource = null;
	//
	// }
	// return count;
	// }

	@Override
	public IHawkModelResource parse(File f) {

		IHawkModelResource ret;

		Resource r = null;

		try {

			r = resourceSet.createResource(URI.createFileURI(f
					.getAbsolutePath()));

			// String o = XMIResource.OPTION_DEFER_IDREF_RESOLUTION;

			/*
			 * System.err.println(">" + resourceSet.getPackageRegistry()
			 * .getEPackage("org.amma.dsl.jdt.core")
			 * .getEClassifier("IPackageFragment")); System.err.println(">>" +
			 * EPackage.Registry.INSTANCE.getEPackage(
			 * "org.amma.dsl.jdt.core").getEClassifier( "IPackageFragment"));
			 * 
			 * //for (Object v :
			 * EPackage.Registry.INSTANCE.values())System.err.println
			 * (((EPackage) v).getEClassifiers());
			 * 
			 * for (String s : resourceSet.getPackageRegistry().keySet())
			 * System.err.println(">>>" +
			 * resourceSet.getPackageRegistry().getEPackage(s) .getNsURI() +
			 * " : " + resourceSet.getPackageRegistry().getEPackage(s)
			 * .getEClassifier("IPackageFragment"));
			 * 
			 * System.err.println(resourceSet.getPackageRegistry().size());
			 * 
			 * for (String s : EPackage.Registry.INSTANCE.keySet())
			 * System.err.println(">>>>" +
			 * EPackage.Registry.INSTANCE.getEPackage(s).getNsURI() + " : " +
			 * EPackage.Registry.INSTANCE.getEPackage(s)
			 * .getEClassifier("IPackageFragment"));
			 * 
			 * System.err.println(EPackage.Registry.INSTANCE.size());
			 */

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
		resourceSet = null;
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
