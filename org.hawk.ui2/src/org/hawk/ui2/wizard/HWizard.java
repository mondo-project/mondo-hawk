/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 ******************************************************************************/
package org.hawk.ui2.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.util.HawkConfig;
import org.hawk.core.util.HawksConfig;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;
import org.hawk.ui2.view.HView;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * This is a sample new wizard.
 */

public class HWizard extends Wizard implements INewWizard {
	private HWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for HawkNewWizard.
	 */
	public HWizard() {
		super();
		setNeedsProgressMonitor(false);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new HWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		final String name = page.getHawkName();
		final String folder = page.getContainerName();
		final String dbType = page.getDBID();
		final List<String> plugins = page.getPlugins();
		final char[] apw = page.getApw();

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(name, folder, dbType, plugins, monitor, apw);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method.
	 * 
	 * @param dbType
	 */

	private void doFinish(String name, String folder, String dbType, List<String> plugins, IProgressMonitor monitor,
			char[] apw) throws CoreException {

		// set up a new Hawk with the selected plugins

		try {
			// create a new hawk index at containerName folder with name
			// fileName
			HModel.create(name, new File(folder), dbType, plugins, HUIManager.getInstance(), apw);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// open hawk view, tell the view about the new index

		monitor.beginTask("Creating ", 2);

		monitor.worked(1);
		monitor.setTaskName("Opening Hawk interface...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					HView view = (HView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.findView(HView.ID);
					view.update();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// add this new hawk to metadata
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.hawk.ui2");

		String xml = preferences.get("config", null);

		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawksConfig.class);
		stream.processAnnotations(HawkConfig.class);
		stream.setClassLoader(HawksConfig.class.getClassLoader());

		HawksConfig hc = null;

		try {

			if (xml != null) {
				hc = (HawksConfig) stream.fromXML(xml);
			}

			HashSet<HawkConfig> locs = new HashSet<HawkConfig>();

			if (hc != null)
				locs.addAll(hc.getConfigs());

			locs.add(new HawkConfig(name, folder));

			xml = stream.toXML(new HawksConfig(locs));

			preferences.put("config", xml);

		} catch (Exception e) {
			e.printStackTrace();
			preferences.remove("config");
		}

		monitor.worked(1);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
}