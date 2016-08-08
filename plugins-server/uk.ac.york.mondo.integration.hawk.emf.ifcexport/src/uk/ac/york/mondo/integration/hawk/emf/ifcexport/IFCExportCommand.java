/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.emf.ifcexport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hawk.ifc.IFCModelResource;
import org.osgi.framework.FrameworkUtil;

import uk.ac.york.mondo.integration.hawk.emf.HawkModelDescriptor;
import uk.ac.york.mondo.integration.hawk.emf.impl.HawkResourceImpl;

public class IFCExportCommand extends AbstractHandler {

	private static final int REPORTING_BATCH_SIZE = 1_000;

	private final class IFCExportJobFunction implements IJobFunction {
		private final File dest;
		private final IFile hawkModel;

		private IFCExportJobFunction(File dest, IFile hawkModel) {
			this.dest = dest;
			this.hawkModel = hawkModel;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			final String pluginId = FrameworkUtil.getBundle(IFCExportCommand.class).getSymbolicName();
			try {
				exportToSTEP(hawkModel, dest, monitor);
				return new Status(IStatus.OK, pluginId, "Completed export");
			} catch (Exception e) {
				e.printStackTrace();
				return new Status(IStatus.ERROR, pluginId, e.getMessage());
			} finally {
				monitor.done();
			}
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection0 = HandlerUtil.getCurrentSelection(event);
		if (selection0 instanceof IStructuredSelection) {
			final IStructuredSelection selection = (IStructuredSelection) selection0;
			final IFile hawkModel = (IFile) selection.getFirstElement();
			final File dest = hawkModel.getLocation().removeFileExtension().addFileExtension("ifc").toFile();

			final String jobName = "Export " + hawkModel.getName() + " to " + dest;
			final Job job = Job.create(jobName, new IFCExportJobFunction(dest, hawkModel));
			job.schedule();
		}

		return null;
	}

	protected void exportToSTEP(final IFile hawkModel, final File dest, final IProgressMonitor monitor)
			throws IOException, FileNotFoundException, Exception, IfcModelInterfaceException, SerializerException,
			CoreException {
		monitor.beginTask("Exporting " + hawkModel.getName() + " to " + dest.getName(), 4);

		monitor.subTask("Loading descriptor");
		final HawkModelDescriptor desc = new HawkModelDescriptor();
		desc.load(new FileReader(hawkModel.getLocation().toFile()));
		desc.setSplit(false);
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return;
		}

		monitor.subTask("Loading remote Hawk resource");
		final URI emfURI = URI.createURI(hawkModel.getLocationURI().toString());
		final ResourceSet rs = new ResourceSetImpl();
		final HawkResourceImpl resource = new HawkResourceImpl(emfURI, desc);
		try {
			rs.getResources().add(resource);
			resource.doLoad(desc, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			monitor.subTask("Populating IFC serializer");
			Serializer serializer;
			if (desc.getLoadingMode().isGreedyElements()) {
				serializer = greedySerialize(resource, monitor);
			} else {
				serializer = lazySerialize(resource, monitor);
			}
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

			hawkModel.getParent().refreshLocal(IResource.DEPTH_ONE, null);
		} finally {
			resource.unload();
		}
	}

	protected Serializer greedySerialize(final HawkResourceImpl resource, IProgressMonitor monitor)
			throws Exception, IfcModelInterfaceException {
		final int total = resource.getContents().size();
		/*
		 * Greedy loading modes are simple: we already have everything in the
		 * model, so we can use the standard EMF facilities.
		 */
		final Serializer serializer = createSerializer(resource);
		long oid = 0;
		int current = 0, offset = 0;
		for (TreeIterator<EObject> it = EcoreUtil.getAllContents(resource, false); it.hasNext();) {
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

	protected Serializer lazySerialize(HawkResourceImpl resource, IProgressMonitor monitor) throws Exception {
		/*
		 * Lazy loading modes need to follow all cross references between
		 * objects to gradually rebuild the full graph on the client.
		 * getAllContents won't work, since it relies on using containment (and
		 * IFC models are flat).
		 */
		final Set<EObject> encoded = new HashSet<>();
		final Deque<EObject> pending = new LinkedList<>();
		for (EObject eob : resource.getContents()) {
			pending.add(eob);
		}

		final Serializer serializer = createSerializer(resource);
		long oid = 0, current = 0, offset = 0;
		while (!pending.isEmpty()) {
			final EObject eob = pending.remove();
			if (!encoded.contains(eob)) {
				oid = addToSerializer(serializer, oid, eob);
				encoded.add(eob);
				++current;
				for (EObject ref : eob.eCrossReferences()) {
					pending.add(ref);
				}

				if (current == REPORTING_BATCH_SIZE) {
					offset += current;
					current = 0;
					monitor.subTask(String.format("Populating IFC serializer (%d objects so far, %d in the queue)",
							offset, pending.size()));
				}
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

	private Serializer createSerializer(final HawkResourceImpl resource) throws Exception {
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

}
