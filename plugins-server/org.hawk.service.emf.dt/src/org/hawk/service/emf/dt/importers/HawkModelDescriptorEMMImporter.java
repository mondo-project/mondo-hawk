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
package org.hawk.service.emf.dt.importers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.launching.dialogs.BrowseWorkspaceUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hawk.service.api.EffectiveMetamodelRuleset;
import org.hawk.service.emf.HawkModelDescriptor;
import org.hawk.service.emf.dt.Activator;

/**
 * Imports the effective metamodel from another <code>.hawkmodel</code> file.
 */
public class HawkModelDescriptorEMMImporter implements EMMImporter {

	@Override
	public void importEffectiveMetamodelInto(EffectiveMetamodelRuleset targetEMM) {
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		IFile file = BrowseWorkspaceUtil.browseFile(shell,
			"Select a .hawkmodel file",
			"Select the Hawk model descriptor to import the effective metamodel from.",
			"*.hawkmodel", null);

		if (file != null) {
			HawkModelDescriptor descriptor = new HawkModelDescriptor();
			try {
				descriptor.load(file.getContents());
				final EffectiveMetamodelRuleset sourceEMM = descriptor.getEffectiveMetamodel();
				targetEMM.importRules(sourceEMM);
			} catch (IOException | CoreException e) {
				Activator.getDefault().logError(e);
			}
		}
	}

}
