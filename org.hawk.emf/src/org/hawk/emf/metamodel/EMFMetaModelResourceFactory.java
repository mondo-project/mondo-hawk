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
package org.hawk.emf.metamodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.EMFpackage;
import org.hawk.emf.model.util.RegisterMeta;

public class EMFMetaModelResourceFactory implements IMetaModelResourceFactory {

	String type = "org.hawk.emf.metamodel.EMFMetaModelParser";
	String hrn = "EMF Metamodel Resource Factory";
	// GraphDatabase graph;

	HashSet<String> metamodelExtensions;
	HashSet<String> modelExtensions;
	ResourceSet resourceSet = null;

	public EMFMetaModelResourceFactory() {

		metamodelExtensions = new HashSet<String>();
		modelExtensions = new HashSet<String>();

		metamodelExtensions.add("ecore");
		metamodelExtensions.add("ECORE");
		modelExtensions.add("xmi");
		modelExtensions.add("XMI");

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

	@Override
	public void shutdown() {
		type = null;
		metamodelExtensions = null;
		modelExtensions = null;
		resourceSet = null;
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {

		EMFMetaModelResource ret;

		Resource r = resourceSet.createResource(URI.createFileURI(f
				.getAbsolutePath()));
		r.load(null);

		//
		RegisterMeta.registerPackages(r);

		ret = new EMFMetaModelResource(r, this);

		return ret;

	}

	@Override
	public HashSet<String> getMetaModelExtensions() {

		return metamodelExtensions;
	}

	@Override
	public IHawkMetaModelResource createMetamodelWithSinglePackage(String s,
			IHawkPackage p) {

		Resource r = resourceSet.createResource(URI.createURI(s));

		r.getContents().add(((EMFpackage) p).getEObject());

		return new EMFMetaModelResource(r, this);

	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents)
			throws Exception {

		if (name != null && contents != null) {

			Resource r = resourceSet.createResource(URI.createURI(name));

			InputStream input = new ByteArrayInputStream(
					contents.getBytes("UTF-8"));

			r.load(input, null);

			//
			RegisterMeta.registerPackages(r);

			return new EMFMetaModelResource(r, this);
		} else
			return null;
	}

	@Override
	public void removeMetamodel(String property) {

		boolean found = false;
		Resource rem = null;

		for (Resource r : resourceSet.getResources())
			if (r.getURI().toString().contains(property)) {
				rem = r;
				found = true;
				break;
			}

		if (found)
			try {
				rem.delete(null);
				//EPackage.Registry.INSTANCE.remove(property);
			} catch (Exception e) {
				e.printStackTrace();
			}

		System.err.println(found ? "removed: " + property : property
				+ " not present in this EMF parser");

	}

	@Override
	public boolean canParse(File f) {
		String[] split = f.getPath().split("\\.");
		String extension = split[split.length - 1];

		return getMetaModelExtensions().contains(extension);
	}

	@Override
	public HashSet<IHawkMetaModelResource> getStaticMetamodels() {

		// System.out.println("insertRegisteredMetamodels() called");

		HashSet<IHawkMetaModelResource> set = new HashSet<>();

		// Registry globalRegistry = EPackage.Registry.INSTANCE;
		//
		// HashSet<String> keys = new HashSet<>();
		//
		// keys.addAll(globalRegistry.keySet());
		//
		// for (String e : keys)
		// if (notDefaultPackage(e)) {
		// // System.out.println(">" + e);
		// Object ep = globalRegistry.get(e);
		// if (ep instanceof EPackage)
		// set.add(new EMFMetaModelResource(((EPackage) ep)
		// .eResource(), this));
		// else if (ep instanceof EPackage.Descriptor)
		// set.add(new EMFMetaModelResource(((EPackage.Descriptor) ep)
		// .getEPackage().eResource(), this));
		// }
		//
		// System.err.println(set);

		// if (set.size() > 0)
		// new GraphMetaModelResourceInjector(graph, set);

		// return graph;
		// System.out.println("insertRegisteredMetamodels() finished");

		// XXX removing any static (global registry resident) metamodels in emf
		// as we treat it as file-based only for now
		set.clear();
		return set;
	}

	private boolean notDefaultPackage(String e) {
		// System.err.println(">" + e);

		// new eclipse populates the registry with MANY random metamodels so no
		// way to pre-populate this in emf without ignoring www.eclipse

		// http://www.eclipse.org/emf/2003/XMLType,
		// http://www.eclipse.org/emf/2002/Ecore,
		// http://www.w3.org/XML/1998/namespace

		// if (e.contains("www.eclipse.org/emf/") && e.contains("XMLType")
		// || e.contains("www.eclipse.org/emf/") && e.contains("Ecore")
		// || e.contains("www.w3.org/XML") && e.contains("namespace"))
		if (e.contains("http://www.eclipse.org/")
				|| e.contains("http:///org/eclipse/")
				|| e.contains("www.w3.org/XML") && e.contains("namespace"))
			return false;
		else
			return true;
	}
}
