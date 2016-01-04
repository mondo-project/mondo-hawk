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
package org.hawk.ui.emfresource.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class CreateDescriptorFilePage extends WizardNewFileCreationPage {

	private String selectedInstance;

	public CreateDescriptorFilePage(IStructuredSelection currentSelection) {
		super("Create new local Hawk model descriptor", currentSelection);
		setTitle("Create new local Hawk model descriptor");
		setDescription("Select the destination path for the new descriptor.");
	}

	@Override
	protected InputStream getInitialContents() {
		return new ByteArrayInputStream(selectedInstance.getBytes());
	}

	public String getSelectedInstance() {
		return selectedInstance;
	}

	public void setSelectedInstance(String selectedInstance) {
		this.selectedInstance = selectedInstance;
	}
}
