/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
package org.hawk.ui.emfresource.wizards;

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
		createFilePage.setSplit(selectHawkPage.isSplit());
		createFilePage.setRepositoryPatterns(selectHawkPage.getRepositoryPatterns());
		createFilePage.setFilePatterns(selectHawkPage.getFilePatterns());
		return createFilePage.createNewFile() != null;
	}
}