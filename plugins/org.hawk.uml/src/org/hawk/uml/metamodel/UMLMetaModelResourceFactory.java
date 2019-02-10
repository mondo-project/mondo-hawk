/*******************************************************************************
 * Copyright (c) 2017-2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.uml.metamodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.eclipse.uml2.types.TypesPackage;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;
import org.eclipse.uml2.uml.profile.standard.StandardPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.EMFWrapperFactory;
import org.hawk.emf.metamodel.EMFMetaModelResource;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds support for the UML metamodel.
 */
@SuppressWarnings("restriction")
public class UMLMetaModelResourceFactory implements IMetaModelResourceFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(UMLMetaModelResourceFactory.class);

	private static final String MM_EXTENSION = ".profile.uml";
	private EMFMetaModelResourceFactory emfMMFactory = new EMFMetaModelResourceFactory();
	private ResourceSet resourceSet;

	private EMFWrapperFactory emfWFactory = new EMFWrapperFactory();
	private UMLWrapperFactory umlWFactory = new UMLWrapperFactory();

	public UMLMetaModelResourceFactory() {
		resourceSet = new ResourceSetImpl();
		UMLUtil.init(resourceSet);

		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("uml", new UMLResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("ecore", new EcoreResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("*", new XMIResourceFactoryImpl());
	}

	@Override
	public String getHumanReadableName() {
		return "UML Metamodel Resource Factory";
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		UMLResource r = (UMLResource) resourceSet.createResource(URI.createFileURI(f.getAbsolutePath()));
		r.load(null);

		if (f.getName().endsWith(MM_EXTENSION)) {
			return new EMFMetaModelResource(r, umlWFactory, this);
		} else {
			return new EMFMetaModelResource(r, emfWFactory, this);
		}
	}

	@Override
	public Set<IHawkMetaModelResource> getStaticMetamodels() {
		Set<IHawkMetaModelResource> resources = new HashSet<>();
		resources.add(new EMFMetaModelResource(EcorePackage.eINSTANCE.eResource(), emfWFactory, this));
		resources.add(new EMFMetaModelResource(TypesPackage.eINSTANCE.eResource(), emfWFactory, this));
		resources.add(new EMFMetaModelResource(XMLTypePackage.eINSTANCE.eResource(), emfWFactory, this));
		resources.add(new EMFMetaModelResource(UMLPackage.eINSTANCE.eResource(), emfWFactory, this));
		resources.add(new EMFMetaModelResource(StandardPackage.eINSTANCE.eResource(), umlWFactory, this));

		try {
			final Resource rEcoreProfile = resourceSet.createResource(URI.createURI(UMLResource.ECORE_PROFILE_URI));
			rEcoreProfile.load(null);
			final EMFMetaModelResource hrEcoreProfile = new EMFMetaModelResource(rEcoreProfile, umlWFactory, this);
			resources.add(hrEcoreProfile);

			final Resource rUMLProfile = resourceSet.createResource(URI.createURI(UMLResource.UML2_PROFILE_URI));
			rUMLProfile.load(null);
			final EMFMetaModelResource hrUMLProfile = new EMFMetaModelResource(rUMLProfile, umlWFactory, this);
			resources.add(hrUMLProfile);
		} catch (IOException e) {
			LOGGER.error("Error while loading predefined profiles", e);
		}

		return resources;
	}

	@Override
	public void shutdown() {
		// nothing to do for now
	}

	@Override
	public boolean canParse(File f) {
		return f.getName().toLowerCase().endsWith(MM_EXTENSION);
	}

	@Override
	public Collection<String> getMetaModelExtensions() {
		return Collections.singleton(MM_EXTENSION);
	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents) throws Exception {
		if (name == null || contents == null) {
			return null;
		}

		Resource r = resourceSet.createResource(URI.createURI(name));
		InputStream input = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		r.load(input, null);

		final EMFWrapperFactory wf = name.endsWith(MM_EXTENSION) ? umlWFactory : emfWFactory;
		return new EMFMetaModelResource(r, wf, this);
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		// TODO add UML-specific logic here (profiles?)
		return emfMMFactory.dumpPackageToString(ePackage);
	}

}
