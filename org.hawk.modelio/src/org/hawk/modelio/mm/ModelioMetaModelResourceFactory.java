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
package org.hawk.modelio.mm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.modelio.ModelioPackage;

public class ModelioMetaModelResourceFactory implements IMetaModelResourceFactory {

	private static final String TYPE = "org.hawk.modelio.mm.ModelioMetaModelResourceFactory";

	ResourceSet resourceSet = null;

	public ModelioMetaModelResourceFactory() {

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
		return TYPE;
	}

	@Override
	public void shutdown() {
		resourceSet = null;
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		ModelioMetaModelResource ret;

		Resource r = resourceSet.createResource(URI.createFileURI(f
				.getAbsolutePath()));
		r.load(null);

		RegisterMeta.registerPackages(r);
		ret = new ModelioMetaModelResource(r, this);
		return ret;
	}

	@Override
	public Set<String> getMetaModelExtensions() {
		return Collections.emptySet();
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {

		if (name != null && contents != null) {
			Resource r = resourceSet.createResource(URI.createURI(name));
			InputStream input = new ByteArrayInputStream(contents.getBytes("UTF-8"));
			r.load(input, null);

			RegisterMeta.registerPackages(r);
			return new ModelioMetaModelResource(r, this);
		} else
			return null;
	}

	@Override
	public String dumpPackageToString(IHawkPackage pkg) throws Exception {
		final ModelioPackage ePackage = (ModelioPackage) pkg;
		final ModelioMetaModelResource eResource = (ModelioMetaModelResource)ePackage.getResource();

		final Resource oldResource = eResource.res;
		final Resource newResource = resourceSet.createResource(URI.createURI("resource_from_epackage_" + ePackage.getNsURI()));
		final EObject eob = ePackage.getEObject();
		newResource.getContents().add(eob);

		final ByteArrayOutputStream bOS = new ByteArrayOutputStream();
		try {
			newResource.save(bOS, null);
			final String contents = new String(bOS.toByteArray());
			return contents;
		} finally {
			/*
			 * Move back the EPackage into its original resource, to avoid
			 * inconsistencies across restarts.
			 */
			oldResource.getContents().add(eob);
		}
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
		return false;
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		Set<IHawkMetaModelResource> set = new HashSet<>();

		Resource rEcore = EcorePackage.eINSTANCE.eResource();
		Resource rUML = UMLPackage.eINSTANCE.eResource();
		//Resource rr = PrimitiveType resourceSet.createResource(URI.createFileURI(fr.getAbsolutePath()));
		try {
			rEcore.load(null);
			rUML.load(null);
			//rr.load(null);
		} catch (IOException e) {
			System.err.print("WARNING: static metamodel of UML was not found, no static metamodels inserted for this plugin, please insert the relevant metamodels manually");
		}

		set.add(new ModelioMetaModelResource(rEcore, this));
		set.add(new ModelioMetaModelResource(rUML, this));

		return set;
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio Meta model parser for Hawk";
	}
}
