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
package org.hawk.ui.emf.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewDescriptorWizard extends Wizard implements INewWizard {

	private SelectHawkInstancePage selectHawkPage;
	private IStructuredSelection currentSelection;
	private CreateDescriptorFilePage createFilePage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.currentSelection = selection;
	}

	@Override
	public void addPages() {
		createFilePage = new CreateDescriptorFilePage(currentSelection);
		createFilePage.setFileExtension("localhawkmodel");
		selectHawkPage = new SelectHawkInstancePage();
		addPage(selectHawkPage);
		addPage(createFilePage);
	}

	@Override
	public String getWindowTitle() {
		return "Create new local Hawk model descriptor";
	}

	@Override
	public boolean performFinish() {
		createFilePage.setSelectedInstance(selectHawkPage.getSelectedInstance());
		return createFilePage.createNewFile() != null;
	}
}