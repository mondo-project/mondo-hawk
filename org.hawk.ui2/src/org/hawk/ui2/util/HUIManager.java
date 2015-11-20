/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
			PlatformUI.getWorkbench().addWorkbenchListener(inst);
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
		HView.updateAsync(PlatformUI.getWorkbench().getDisplay());
	}

	@Override
	protected void infoChanged(HModel m) {
		HView.updateAsync(PlatformUI.getWorkbench().getDisplay());
	}

}
