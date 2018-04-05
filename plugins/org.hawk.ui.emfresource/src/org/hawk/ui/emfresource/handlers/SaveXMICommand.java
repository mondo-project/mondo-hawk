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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.ui.emfresource.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.FrameworkUtil;

/**
 * Dumps the contents of a <code>.localhawkmodel</code> back into XMI form. Can be useful
 * if the indexed source format is not parsable by EMF - this is the case with Modelio
 * EXML.
 */
public class SaveXMICommand extends AbstractHandler {

	public class XMIDumpJobFunction implements IJobFunction {

		private File dest;
		private IFile hawkModel;

		public XMIDumpJobFunction(File dest, IFile hawkModel) {
			this.dest = dest;
			this.hawkModel = hawkModel;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			final String pluginId = FrameworkUtil.getBundle(SaveXMICommand.class).getSymbolicName();
			try {
				dumpToXMI(hawkModel, dest, monitor);
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
			final File dest = hawkModel.getLocation().removeFileExtension().addFileExtension("xmi").toFile();

			final String jobName = "Export " + hawkModel.getName() + " to " + dest;
			final Job job = Job.create(jobName, new XMIDumpJobFunction(dest, hawkModel));
			job.schedule();
		}
		return null;
	}

	protected void dumpToXMI(IFile hawkModel, File dest, IProgressMonitor monitor) throws IOException, CoreException {
		monitor.beginTask("Saving " + hawkModel.getName() + " to XMI", 3);

		// Load .localhawkmodel as a resource
		ResourceSet rs = new ResourceSetImpl();
		Resource rSource = rs.createResource(URI.createURI(hawkModel.getLocationURI().toString()));
		Resource rTarget = null;
		try {
			monitor.subTask("Loading graph as a model");
			rSource.load(null);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			/*
			 * Create new XMI resource and add contents of the resource set to
			 * it (if the .localhawkmodel is split by file, loading it will have
			 * added a few auxiliary resources there).
			 */
			monitor.subTask("Dumping graph to XMI");
			rTarget = new XMIResourceImpl(URI.createFileURI(dest.getAbsolutePath()));
			for (Resource r : new ArrayList<>(rs.getResources())) {
				rTarget.getContents().addAll(new ArrayList<>(r.getContents()));
			}
			rTarget.save(null);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			hawkModel.getParent().refreshLocal(IResource.DEPTH_ONE, null);
		} finally {
			if (rTarget != null && rTarget.isLoaded()) {
				rTarget.unload();
			}
			if (rSource.isLoaded()) {
				rSource.unload();
			}
		}
	}

}
