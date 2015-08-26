/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.MetaDataManager;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializer;
import org.bimserver.ifc.step.deserializer.Ifc4StepDeserializer;
import org.bimserver.ifc.xml.deserializer.Ifc2x3tc1XmlDeserializer;
import org.bimserver.ifc.xml.deserializer.Ifc4XmlDeserializer;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.plugins.Plugin;
import org.bimserver.plugins.PluginDescriptor;
import org.bimserver.plugins.PluginImplementation;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.PluginSourceType;
import org.bimserver.plugins.deserializers.Deserializer;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;

public class IFCModelResource implements IHawkModelResource {

	private IFCModelFactory factory;
	private File ifc;

	@Override
	public void unload() {
		ifc = null;
	}
	
	public IFCModelResource(File f, IFCModelFactory p) {
		factory = p;
		ifc = f;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		// TODO wouldn't it be better to use an iterable, so we don't keep everything into memory?
		Set<IHawkObject> allElements = new HashSet<IHawkObject>();

		try {
			Deserializer d = createDeserializer();
			IfcModelInterface s = d.read(ifc);
			for (IdEObject eo : s.getValues()) {
				allElements.add(new IFCObject(eo));
				for (final TreeIterator<EObject> it = eo.eAllContents(); it.hasNext(); ) {
					final EObject eo2 = it.next();
					allElements.add(new IFCObject(eo2));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return allElements;
	}

	private Deserializer createDeserializer() throws Exception {
		Deserializer d;

		switch (factory.getIFCModelType(ifc)) {
		case IFC2X3_STEP:
			d = new Ifc2x3tc1StepDeserializer(Schema.IFC2X3TC1); break;
		case IFC2X3_XML:
			d = new Ifc2x3tc1XmlDeserializer(); break;
		case IFC4_STEP:
			d = new Ifc4StepDeserializer(Schema.IFC4); break;
		case IFC4_XML:
			d = new Ifc4XmlDeserializer(); break;
		default:
			throw new IllegalArgumentException("Unknown IFC model type");
		}

		PluginManager bimPluginManager = createPluginManager();
		MetaDataManager bimMetaDataManager = new MetaDataManager(bimPluginManager);
		bimMetaDataManager.init();
		final String packageLowerCaseName = Ifc2x3tc1Package.eINSTANCE.getName().toLowerCase();
		final PackageMetaData packageMetaData = bimMetaDataManager.getPackageMetaData(packageLowerCaseName);
		d.init(packageMetaData);

		return d;
	}

	@SuppressWarnings("unchecked")
	private PluginManager createPluginManager() throws Exception {
		final PluginManager bimPluginManager = new PluginManager();
		final InputStream isBIMPluginXML = IFCModelResource.class.getResourceAsStream("/plugin/plugin.xml");
		final PluginDescriptor desc = readPluginDescriptor(isBIMPluginXML);
		for (PluginImplementation impl : desc.getImplementations()) {
			final Class<? extends Plugin> interfaceClass = (Class<? extends Plugin>) Class.forName(impl.getInterfaceClass());
			final Class<?> implClass = Class.forName(impl.getImplementationClass());
			final Plugin plugin = (Plugin) implClass.newInstance();
			bimPluginManager.loadPlugin(interfaceClass, "", "", plugin,
					this.getClass().getClassLoader(),
					PluginSourceType.INTERNAL, impl);
		}
		return bimPluginManager;
	}

	@Override
	public String getType() {
		return factory.getType();
	}

	@Override
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}

	private PluginDescriptor readPluginDescriptor(InputStream is) throws JAXBException {
		JAXBContext ctx = JAXBContext.newInstance(PluginDescriptor.class);
		Unmarshaller unm = ctx.createUnmarshaller();
		return (PluginDescriptor) unm.unmarshal(is);
	}

}
