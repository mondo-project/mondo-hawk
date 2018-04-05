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
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 *     Antonio Garcia-Dominguez - improve logging, move metadata updates
 ******************************************************************************/
package org.hawk.ui2.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawkFactory;
import org.hawk.core.util.HawkConfig;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;
import org.hawk.ui2.view.HView;

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
		try {
			final String name = page.getHawkName();
			final String folder = page.getContainerName();
			final String dbType = page.getDBID();
			final List<String> plugins = page.getPlugins();
			final String location = page.getLocation();
			final IHawkFactory factory = page.getFactory();
			final int maxDelay = page.getMaxDelay();
			final int minDelay = page.getMinDelay();
			final boolean isNew = page.isNew();
			
			final ICredentialsStore credStore = HUIManager.getInstance()
					.getCredentialsStore();

			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException {
					try {
						doFinish(name, new File(folder), location, dbType,
								plugins, monitor, credStore, factory, minDelay,
								maxDelay,isNew);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			};

			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * The worker method.
	 * 
	 * @param dbType
	 * @param factoryId
	 */
	private void doFinish(String name, File storageFolder, String location,
			String dbType, List<String> plugins, IProgressMonitor monitor,
			ICredentialsStore credStore, IHawkFactory factory, int minDelay,
			int maxDelay,boolean isNew) throws Exception {

		// set up a new Hawk with the selected plugins
		HModel hm;

		if (isNew) {
			System.out.println("creating new hawk...");
			hm = HModel.create(factory, name, storageFolder, location, dbType,
					plugins, HUIManager.getInstance(), credStore, minDelay,
					maxDelay);
		} else {
			// connect to existing Hawk
			System.out.println("loading hawk metadata...");
			HawkConfig hc = new HawkConfig(name,
					storageFolder.getCanonicalPath(), location, factory
							.getClass().getName(), plugins);
			final HUIManager manager = HUIManager.getInstance();
			hm = HModel.load(hc, manager);
			manager.addHawk(hm);
		}

		monitor.beginTask("Creating ", 2);
		monitor.worked(1);
		monitor.setTaskName("Opening Hawk interface...");
		HView.updateAsync(getShell().getDisplay());
		HUIManager.getInstance().saveHawkToMetadata(hm);
		monitor.worked(1);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
}