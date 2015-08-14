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
package org.hawk.bpmn.metamodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.bpmn.EMFpackage;
import org.hawk.bpmn.model.util.RegisterMeta;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;

public class BPMNMetaModelResourceFactory implements IMetaModelResourceFactory {

	String type = "org.hawk.emf.metamodel.BPMNMetaModelParser";
	String hrn = "BPMN Metamodel Resource Factory";
	// GraphDatabase graph;

	HashSet<String> metamodelExtensions;
	HashSet<String> modelExtensions;
	ResourceSet resourceSet = null;

	public BPMNMetaModelResourceFactory() {

		metamodelExtensions = new HashSet<String>();
		modelExtensions = new HashSet<String>();

		// metamodelExtensions.add("ecore");
		// metamodelExtensions.add("ECORE");
		modelExtensions.add("bpmn2");
		modelExtensions.add("BPMN2");

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

		BPMNMetaModelResource ret;

		Resource r = resourceSet.createResource(URI.createFileURI(f
				.getAbsolutePath()));
		r.load(null);

		//
		RegisterMeta.registerPackages(r);

		ret = new BPMNMetaModelResource(r, this);

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

		return new BPMNMetaModelResource(r, this);

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

			return new BPMNMetaModelResource(r, this);
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
				// EPackage.Registry.INSTANCE.remove(property);
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

		HashSet<IHawkMetaModelResource> set = new HashSet<>();

		LinkedList<String> missingPackages = checkRegistry();

		// if not running in eclipse
		if (missingPackages.size() > 0) {

			List<String> paths = new LinkedList<>();

			ClassLoader emf = EPackage.class.getClassLoader();
			try {
				paths.add(emf.getResource("model/Ecore.ecore").toURI()
						.toString());
				paths.add(emf.getResource("model/XMLType.ecore").toURI()
						.toString());

				ClassLoader bpmn = Activity.class.getClassLoader();
				paths.add(bpmn.getResource("model/BPMN20.ecore").toURI()
						.toString());
				paths.add(bpmn.getResource("model/BPMNDI.ecore").toURI()
						.toString());
				paths.add(bpmn.getResource("model/DC.ecore").toURI()
						.toString());
				paths.add(bpmn.getResource("model/DI.ecore").toURI()
						.toString());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			EPackage e = EcorePackageImpl.eINSTANCE;
			RegisterMeta.registerPackages(e);
			
			for (String path : paths)
				for (EObject o : resourceSet.getResource(
						URI.createURI(path), true).getContents())
					if (o instanceof EPackage)
						RegisterMeta.registerPackages((EPackage) o);

			missingPackages = checkRegistry();

		}

		if (missingPackages.size() == 0) {

			Registry globalRegistry = EPackage.Registry.INSTANCE;

			set.add(new BPMNMetaModelResource(globalRegistry.getEPackage(
					"http://www.eclipse.org/emf/2003/XMLType").eResource(),
					this));

			set.add(new BPMNMetaModelResource(globalRegistry.getEPackage(
					"http://www.omg.org/spec/BPMN/20100524/MODEL-XMI")
					.eResource(), this));

			set.add(new BPMNMetaModelResource(globalRegistry.getEPackage(
					"http://www.omg.org/spec/DD/20100524/DC-XMI").eResource(),
					this));

			set.add(new BPMNMetaModelResource(globalRegistry.getEPackage(
					"http://www.eclipse.org/emf/2002/Ecore").eResource(), this));

			set.add(new BPMNMetaModelResource(
					globalRegistry.getEPackage(
							"http://www.omg.org/spec/BPMN/20100524/DI-XMI")
							.eResource(), this));

			set.add(new BPMNMetaModelResource(globalRegistry.getEPackage(
					"http://www.omg.org/spec/DD/20100524/DI-XMI").eResource(),
					this));

		} else {
			System.err
					.println("WARNING: one or more of the static metamodels of BPMN were not found, no static metamodels inserted for this plugin, please insert the relevant metamodels manually:\n"
							+ missingPackages.toString());
		}

		// System.err.println(set);

		return set;

	}

	private LinkedList<String> checkRegistry() {

		Registry globalRegistry = EPackage.Registry.INSTANCE;

		LinkedList<String> missingPackages = new LinkedList<>();

		if (!globalRegistry
				.containsKey("http://www.eclipse.org/emf/2003/XMLType"))
			missingPackages.add("http://www.eclipse.org/emf/2003/XMLType");
		if (!globalRegistry
				.containsKey("http://www.omg.org/spec/BPMN/20100524/MODEL-XMI"))
			missingPackages
					.add("http://www.omg.org/spec/BPMN/20100524/MODEL-XMI");
		if (!globalRegistry
				.containsKey("http://www.omg.org/spec/DD/20100524/DC-XMI"))
			missingPackages.add("http://www.omg.org/spec/DD/20100524/DC-XMI");
		if (!globalRegistry
				.containsKey("http://www.eclipse.org/emf/2002/Ecore"))
			missingPackages.add("http://www.eclipse.org/emf/2002/Ecore");
		if (!globalRegistry
				.containsKey("http://www.omg.org/spec/BPMN/20100524/DI-XMI"))
			missingPackages.add("http://www.omg.org/spec/BPMN/20100524/DI-XMI");
		if (!globalRegistry
				.containsKey("http://www.omg.org/spec/DD/20100524/DI-XMI"))
			missingPackages.add("http://www.omg.org/spec/DD/20100524/DI-XMI");

		return missingPackages;
	}

}
