/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.emf.dt.importers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.launching.dialogs.BrowseWorkspaceUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import uk.ac.york.mondo.integration.api.EffectiveMetamodelRuleset;
import uk.ac.york.mondo.integration.hawk.emf.HawkModelDescriptor;
import uk.ac.york.mondo.integration.hawk.emf.dt.Activator;

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
