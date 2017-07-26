/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - use eINSTANCE to provide static metamodels
 ******************************************************************************/
package org.hawk.ifc.mm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bimserver.models.geometry.GeometryPackage;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc4.Ifc4Package;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
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
import org.hawk.ifc.IFCPackage;

public class IFCMetaModelResourceFactory implements IMetaModelResourceFactory {

	private ResourceSet resourceSet = null;

	public IFCMetaModelResourceFactory() {
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
		return getClass().getName();
	}

	@Override
	public void shutdown() {
		resourceSet = null;
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		IFCMetaModelResource ret;

		Resource r = resourceSet.createResource(URI.createFileURI(f
				.getAbsolutePath()));
		r.load(null);

		RegisterMeta.registerPackages(r);
		ret = new IFCMetaModelResource(r, this);
		return ret;
	}

	@Override
	public Set<String> getMetaModelExtensions() {
		return Collections.emptySet();
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents)
			throws Exception {
		if (name == null || contents == null) {
			return null;
		}

		try (final InputStream input = new ByteArrayInputStream(
				contents.getBytes("UTF-8"))) {
			final Resource r = resourceSet.createResource(URI.createURI(name));
			r.load(input, null);
			RegisterMeta.registerPackages(r);
			return new IFCMetaModelResource(r, this);
		}
	}

	@Override
	public String dumpPackageToString(IHawkPackage pkg) throws Exception {
		final IFCPackage ePackage = (IFCPackage) pkg;
		final Resource newResource = resourceSet.createResource(
				URI.createURI("resource_from_epackage_" + ePackage.getNsURI()));
		final EObject eob = ePackage.getEObject();
		newResource.getContents().add(EcoreUtil.copy(eob));

		final ByteArrayOutputStream bOS = new ByteArrayOutputStream();
		newResource.save(bOS, null);
		final String contents = new String(bOS.toByteArray());
		return contents;
	}

	@Override
	public boolean canParse(File f) {
		return false;
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		final Set<IHawkMetaModelResource> set = new LinkedHashSet<>();
		set.add(new IFCMetaModelResource(EcorePackage.eINSTANCE.eResource(), this));
		set.add(new IFCMetaModelResource(GeometryPackage.eINSTANCE.eResource(), this));
		set.add(new IFCMetaModelResource(Ifc2x3tc1Package.eINSTANCE.eResource(), this));
		set.add(new IFCMetaModelResource(Ifc4Package.eINSTANCE.eResource(), this));
		return set;
	}

	@Override
	public String getHumanReadableName() {
		return "IFC Metamodel parser";
	}
}
