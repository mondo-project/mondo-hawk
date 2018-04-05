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
 *     Konstantinos Barmpis - initial API and implementation, updates and maintenance
 *     Seyyed Shah - adaption to *.ui2 plugin
 ******************************************************************************/
package org.hawk.ui2.util;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.view.HView;

public class HUIManager extends HManager implements IStructuredContentProvider,
		IWorkbenchListener {

	private static HUIManager inst;

	public static HUIManager getInstance() {
		if (inst == null) {
			inst = new HUIManager();
			if (PlatformUI.isWorkbenchRunning()) {
				PlatformUI.getWorkbench().addWorkbenchListener(inst);
			} else {
				System.err.println(
					"No workbench is open: running without a workbench listener.\n"+
					"Please ensure preShutdown is called before shutting down this application!");
			}
		}
		return inst;
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		if (firstRun)
			loadHawksFromMetadata();
		return all.toArray();
	}

	@Override
	public boolean preShutdown(IWorkbench workbench, boolean forced) {
		System.out.println("(PRE SHUTDOWN) Shutting down Hawk");
		HUIManager.getInstance().stopAllRunningInstances(
				ShutdownRequestType.ONLY_LOCAL);
		return true;
	}

	@Override
	public void postShutdown(IWorkbench workbench) {
		System.out.println("(POST SHUTDOWN) Hawk shut down");
	}

	@Override
	protected void stateChanged(HModel m) {
		if (PlatformUI.isWorkbenchRunning()) {
			HView.updateAsync(PlatformUI.getWorkbench().getDisplay());
		}
	}

	@Override
	protected void infoChanged(HModel m) {
		if (PlatformUI.isWorkbenchRunning()) {
			HView.updateAsync(PlatformUI.getWorkbench().getDisplay());
		}
	}
}
