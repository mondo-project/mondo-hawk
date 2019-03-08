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
package org.hawk.bpmn.metamodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.di.BpmnDiPackage;
import org.eclipse.dd.dc.DcPackage;
import org.eclipse.dd.di.DiPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.impl.XMLTypePackageImpl;
import org.hawk.bpmn.BPMNPackage;
import org.hawk.bpmn.model.util.RegisterMeta;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;

public class BPMNMetaModelResourceFactory implements IMetaModelResourceFactory {

	private static final String TYPE = "org.hawk.emf.metamodel.BPMNMetaModelParser";
	private static final String HUMAN_READABLE_NAME = "BPMN Metamodel Resource Factory";

	ResourceSet resourceSet;

	public BPMNMetaModelResourceFactory() {
		if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
			EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI,
					EcorePackage.eINSTANCE);
		}

		resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("ecore", new EcoreResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("*", new XMIResourceFactoryImpl());

	}

	@Override
	public final String getType() {
		return TYPE;
	}

	@Override
	public String getHumanReadableName() {
		return HUMAN_READABLE_NAME;
	}

	@Override
	public void shutdown() {
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
	public Set<String> getMetaModelExtensions() {
		return Collections.emptySet();
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
	public String dumpPackageToString(IHawkPackage pkg) throws Exception {
		final BPMNPackage ePackage = (BPMNPackage) pkg;
		final Resource newResource = resourceSet.createResource(URI.createURI(IMetaModelResourceFactory.DUMPED_PKG_PREFIX + ePackage.getNsURI()));
		final EObject eob = ePackage.getEObject();
		newResource.getContents().add(EcoreUtil.copy(eob));

		final ByteArrayOutputStream bOS = new ByteArrayOutputStream();
		newResource.save(bOS, null);
		final String contents = new String(bOS.toByteArray());
		return contents;
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

		Registry globalRegistry = EPackage.Registry.INSTANCE;

		// if not running in eclipse
		if (missingPackages.size() > 0) {

			// System.err.println("missing packages detected, adding them now...");

			RegisterMeta.registerPackages(EcorePackageImpl.eINSTANCE);
			RegisterMeta.registerPackages(XMLTypePackageImpl.eINSTANCE);
			RegisterMeta.registerPackages(Bpmn2Package.eINSTANCE);
			RegisterMeta.registerPackages(BpmnDiPackage.eINSTANCE);
			RegisterMeta.registerPackages(DiPackage.eINSTANCE);
			RegisterMeta.registerPackages(DcPackage.eINSTANCE);

			missingPackages = checkRegistry();
		}

		if (missingPackages.size() == 0) {

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
