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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class SelectHawkInstancePage extends WizardPage {

	private Composite container;
	private String selectedInstance;

	public SelectHawkInstancePage() {
		super("Create new local Hawk model descriptor");
		setTitle("Select Local Hawk Instance");
		setDescription("Select one of the Hawk instances in this Eclipse installation.");
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.FILL);
		final GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;

		final Label lblInstance = new Label(container, SWT.NONE);
		lblInstance.setText("Hawk instance:");
		final GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		layoutData.verticalIndent = 5;
		lblInstance.setLayoutData(layoutData);

		final ListViewer viewer = new ListViewer(container, SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(getHawkNames());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection sel = event.getSelection();
				if (sel instanceof IStructuredSelection) {
					final IStructuredSelection ssel = (IStructuredSelection) sel;
					if (ssel.isEmpty()) {
						selectedInstance = null;
					} else {
						selectedInstance = (String) ssel.getFirstElement();
					}
					setPageComplete(selectedInstance != null);
				}
			}
		});
		final GridData viewerLayoutData = new GridData(SWT.FILL, SWT.TOP, true, true);
		viewerLayoutData.widthHint = 200;
		viewerLayoutData.heightHint = 100;
		viewer.getList().setLayoutData(viewerLayoutData);

		setControl(container);
		setPageComplete(false);
	}

	private String[] getHawkNames() {
		final HManager hManager = HUIManager.getInstance();
		final Set<HModel> hawks = hManager.getHawks();
		final List<String> hawkNames = new ArrayList<String>();
		for (HModel hawk : hawks) {
			hawkNames.add(hawk.getName());
		}
		Collections.sort(hawkNames);
		final String[] arrHawkNames = hawkNames.toArray(new String[hawkNames.size()]);
		return arrHawkNames;
	}

	public String getSelectedInstance() {
		return selectedInstance;
	}
}
