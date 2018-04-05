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
package org.hawk.emf.metamodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.EMFPackage;
import org.hawk.emf.EMFWrapperFactory;
import org.hawk.emf.model.util.RegisterMeta;

public class EMFMetaModelResourceFactory implements IMetaModelResourceFactory {

	/**
	 * Property that can be set to a comma-separated list of extensions (e.g.
	 * ".profile.xmi") that should be supported in addition to the default
	 * ones (".ecore"). Composite extensions are allowed (e.g.
	 * ".rail.way").
	 */
	public static final String PROPERTY_EXTRA_EXTENSIONS = "org.hawk.emf.metamodel.extraExtensions";

	private final String hrn = "EMF Metamodel Resource Factory";
	private final Set<String> metamodelExtensions;
	private final ResourceSet resourceSet;

	public EMFMetaModelResourceFactory() {
		metamodelExtensions = new HashSet<String>();
		metamodelExtensions.add(".ecore");
		final String sExtraExtensions = System.getProperty(PROPERTY_EXTRA_EXTENSIONS);
		if (sExtraExtensions != null) {
			String[] extraExtensions = sExtraExtensions.split(",");
			for (String extraExtension : extraExtensions) {
				metamodelExtensions.add(extraExtension);
			}
		}

		if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
			EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		}

		resourceSet = new ResourceSetImpl();

		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",
				new EcoreResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return hrn;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		EMFMetaModelResource ret;

		Resource r = resourceSet.createResource(URI.createFileURI(f.getAbsolutePath()));
		r.load(null);
		RegisterMeta.registerPackages(r);

		ret = new EMFMetaModelResource(r, new EMFWrapperFactory(), this);
		return ret;
	}

	@Override
	public Set<String> getMetaModelExtensions() {
		return metamodelExtensions;
	}

	@Override
	public String dumpPackageToString(IHawkPackage pkg) throws Exception {
		final EMFPackage ePackage = (EMFPackage) pkg;
		final EMFMetaModelResource eResource = (EMFMetaModelResource) ePackage.getResource();
		final Resource oldResource = eResource.getResource();

		// Separate the EPackage to be saved to its own resource
		final Resource newResource = resourceSet
				.createResource(URI.createURI("resource_from_epackage_" + ePackage.getNsURI()));
		final EObject eob = ePackage.getEObject();
		newResource.getContents().add(EcoreUtil.copy(eob));

		/*
		 * Separate the other EPackages that may reside in the old resource in
		 * the same way, so they all refer to each other using
		 * resource_from_epackage_...
		 */
		final List<EObject> otherContents = new ArrayList<>(oldResource.getContents());
		final List<Resource> auxResources = new ArrayList<>();
		for (EObject otherContent : otherContents) {
			if (otherContent instanceof EPackage) {
				final EPackage otherEPackage = (EPackage) otherContent;
				final Resource auxResource = resourceSet
						.createResource(URI.createURI("resource_from_epackage_" + otherEPackage.getNsURI()));
				auxResources.add(auxResource);
				auxResource.getContents().add(EcoreUtil.copy(otherEPackage));
			}
		}

		final ByteArrayOutputStream bOS = new ByteArrayOutputStream();
		try {
			newResource.save(bOS, null);
			final String contents = new String(bOS.toByteArray());
			return contents;
		} finally {
			/*
			 * Unload and remove all the auxiliary resources we've created
			 * during the dumping.
			 */
			newResource.unload();
			resourceSet.getResources().remove(newResource);
			for (Resource auxResource : auxResources) {
				auxResource.unload();
				resourceSet.getResources().remove(auxResource);
			}
		}
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {
		if (name != null && contents != null) {
			Resource r = resourceSet.createResource(URI.createURI(name));
			InputStream input = new ByteArrayInputStream(contents.getBytes("UTF-8"));
			r.load(input, null);

			RegisterMeta.registerPackages(r);

			return new EMFMetaModelResource(r, new EMFWrapperFactory(), this);
		} else
			return null;
	}

	@Override
	public boolean canParse(File f) {
		String[] split = f.getPath().split("\\.");
		String extension = split[split.length - 1];

		return getMetaModelExtensions().contains(extension);
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		return Collections.emptySet();
	}
}
