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
package org.hawk.ui.emfresource.exeed;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hawk.emfresource.HawkResource;
import org.hawk.ui.emfresource.Activator;

public class FetchByEClassAction extends Action {
	private final HawkResource resource;

	public FetchByEClassAction(HawkResource r) {
		this.resource = r;
	}

	@Override
	public void run() {
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final EClassSelectionDialog dlg = new EClassSelectionDialog(shell, resource);
		if (dlg.open() == Dialog.OK) {
			try {
				EList<EObject> eobjs = resource.fetchNodes(dlg.getEClass());
				Activator.logInfo("Fetched " + eobjs.size() + " nodes");
			} catch (Exception e) {
				Activator.logError("Failed to fetch nodes by class", e);
			}
		}
	}
}