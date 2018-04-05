/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
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
 *    Ran Wei - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.ifcexport.servlet.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.emf.MetaDataManager;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.ifc.BasicIfcModel;
import org.bimserver.ifc.step.serializer.Ifc2x3tc1StepSerializer;
import org.bimserver.ifc.step.serializer.Ifc4StepSerializer;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.plugins.Plugin;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginDescriptor;
import org.bimserver.plugins.PluginImplementation;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.PluginSourceType;
import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.ProjectInfo;
import org.bimserver.plugins.serializers.Serializer;
import org.bimserver.plugins.serializers.SerializerException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.hawk.emfresource.HawkResource;
import org.hawk.emfresource.impl.LocalHawkResourceImpl;
import org.hawk.ifc.IFCModelResource;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.service.api.IFCExportJob;

public class IFCExportJobExecutor extends Job{
	private static final int REPORTING_BATCH_SIZE = 1_000;
	protected IFCExportJob job;
	protected IFCExportRequest request;
	
	public IFCExportJobExecutor(IFCExportJob job, IFCExportRequest request)
	{
		super(job.getJobID());
		this.job = job;
		this.request = request;
	}
	
	public IFCExportJob getJob() {
		return job;
	}
	
	public IFCExportRequest getRequest() {
		return request;
	}
	
	protected HModel getHawkModel()
	{
		HManager manager = HManager.getInstance();
		HModel model = manager.getHawkByName(request.hawkInstance);
		return model;
	}
	
	protected HawkResource getResource()
	{
		return new LocalHawkResourceImpl(URI.createURI("hawk://"), 
				getHawkModel().getIndexer(), false, 
				Collections.singletonList(request.getRepositoryPattern()),
				request.getFilePatterns());
	}

	
	protected void exportToSTEP(final File dest, final IProgressMonitor monitor)
			throws IOException, FileNotFoundException, Exception, IfcModelInterfaceException, SerializerException,
			CoreException {
		monitor.beginTask("Exporting " + getHawkModel().getName() + " to " + dest.getName(), 4);

		monitor.subTask("Loading Hawk resource");
		final ResourceSet rs = new ResourceSetImpl();
		final HawkResource resource = getResource();
		try {
			rs.getResources().add(resource);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			monitor.subTask("Populating IFC serializer");
			Serializer serializer = greedySerialize(resource, monitor);
			serializer.getModel().generateMinimalExpressIds();
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			monitor.subTask("Writing STEP file");
			serializer.writeToFile(dest, new ProgressReporter() {
				@Override
				public void update(long progress, long max) {
					monitor.subTask(String.format("Writing STEP file (%d/%d)", progress, max));
				}
			});
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			// TODO: ask Will - why don't we unload the resource here?
			//resource.unload();
		}
	}
	
	
	protected Serializer greedySerialize(final HawkResource resource, IProgressMonitor monitor)
			throws Exception, IfcModelInterfaceException {
		final int total = resource.getContents().size();
		/*
		 * Greedy loading modes are simple: we already have everything in the
		 * model, so we can use the standard EMF facilities.
		 */
		final Serializer serializer = createSerializer(resource);
		long oid = 0;
		int current = 0, offset = 0;
		for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			final EObject eo = it.next();
			oid = addToSerializer(serializer, oid, eo);
			++current;

			if (current == REPORTING_BATCH_SIZE) {
				offset += current;
				current = 0;
				monitor.subTask(String.format("Populating IFC serializer (%d/%d)", offset, total));
			}
		}
		return serializer;
	}
	
	private Serializer createSerializer(final HawkResource resource) throws Exception {
		Serializer serializer = null;
		for (TreeIterator<EObject> it = EcoreUtil.getAllContents(resource, false); it.hasNext()
				&& serializer == null;) {
			EObject eo = it.next();
			if (eo instanceof IdEObject) {
				serializer = createSerializer(eo.eClass().getEPackage().getNsURI());
			}
		}
		return serializer;
	}


	
	protected long addToSerializer(final Serializer serializer, long oid, final EObject eo)
			throws IfcModelInterfaceException {
		if (eo.eClass().getEAnnotation("wrapped") != null) {
			// this is a wrapped object: no need to add it explicitly
		} else if (eo instanceof IdEObject) {
			final IdEObject idEObj = (IdEObject) eo;
			serializer.getModel().add(oid, idEObj);
			oid++;
		}
		return oid;
	}

	private Serializer createSerializer(final String nsURI) throws Exception {
		Serializer ser;
		String packageLowerCaseName;

		switch (nsURI) {
		case Ifc4Package.eNS_URI:
			ser = new Ifc4StepSerializer(new PluginConfiguration());
			packageLowerCaseName = Ifc4Package.eINSTANCE.getName().toLowerCase();
			break;
		default:
			ser = new Ifc2x3tc1StepSerializer(new PluginConfiguration());
			packageLowerCaseName = Ifc2x3tc1Package.eINSTANCE.getName().toLowerCase();
			break;
		}

		PluginManager bimPluginManager = createPluginManager();
		MetaDataManager bimMetaDataManager = new MetaDataManager(bimPluginManager);
		bimMetaDataManager.init();
		final PackageMetaData packageMetaData = bimMetaDataManager.getPackageMetaData(packageLowerCaseName);
		final BasicIfcModel ifcModel = new BasicIfcModel(packageMetaData, null);
		ser.init(ifcModel, new ProjectInfo(), bimPluginManager, null, packageMetaData, false);

		return ser;
	}

	@SuppressWarnings("unchecked")
	private PluginManager createPluginManager() throws Exception {
		final PluginManager bimPluginManager = new PluginManager();
		final InputStream isBIMPluginXML = IFCModelResource.class.getResourceAsStream("/plugin/plugin.xml");
		final PluginDescriptor desc = readPluginDescriptor(isBIMPluginXML);
		for (PluginImplementation impl : desc.getImplementations()) {
			final Class<? extends Plugin> interfaceClass = (Class<? extends Plugin>) Class
					.forName(impl.getInterfaceClass());
			final Class<?> implClass = Class.forName(impl.getImplementationClass());
			final Plugin plugin = (Plugin) implClass.newInstance();
			bimPluginManager.loadPlugin(interfaceClass, "", "", plugin, this.getClass().getClassLoader(),
					PluginSourceType.INTERNAL, impl);
		}
		return bimPluginManager;
	}

	private PluginDescriptor readPluginDescriptor(InputStream is) throws JAXBException {
		JAXBContext ctx = JAXBContext.newInstance(PluginDescriptor.class);
		Unmarshaller unm = ctx.createUnmarshaller();
		return (PluginDescriptor) unm.unmarshal(is);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(job.getJobID(), 1);
		File dest = new File(job.getJobID()+".ifc");
		try {
			exportToSTEP(dest, monitor);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		monitor.worked(1);
		return Status.OK_STATUS;
	}
}
