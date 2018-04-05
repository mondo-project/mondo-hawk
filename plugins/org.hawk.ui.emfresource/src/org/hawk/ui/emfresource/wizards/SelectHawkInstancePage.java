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

import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class SelectHawkInstancePage extends WizardPage {

	private String selectedInstance;
	private boolean isSplit = true;
	private String sRepoPatterns = "*";
	private String sFilePatterns = "*";

	public SelectHawkInstancePage() {
		super("Create new local Hawk model descriptor");
		setTitle("Select Local Hawk Instance");
		setDescription("Select one of the Hawk instances in this Eclipse installation.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.FILL);
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
					checkComplete();
				}
			}
		});
		final GridData viewerLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewer.getList().setLayoutData(viewerLayoutData);

		final Button btnSplit = new Button(container, SWT.CHECK);
		btnSplit.setText("Split by file");
		btnSplit.setSelection(isSplit);
		btnSplit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isSplit = btnSplit.getSelection();
			}
		});
		final GridData btnSplitLD = new GridData();
		btnSplitLD.horizontalSpan = 2;
		btnSplit.setLayoutData(btnSplitLD);

		final Label lblRepo = new Label(container, SWT.NONE);
		lblRepo.setText("Repository pattern(s):");
		final Text txtRepo = new Text(container, SWT.BORDER);
		txtRepo.setToolTipText("Patterns for the repositories that should be exposed, separated by commas. '*' means 'any 0+ characters'.");
		txtRepo.setText(sRepoPatterns);
		txtRepo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sRepoPatterns = txtRepo.getText();
				checkComplete();
			}
		});
		txtRepo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Label lblFiles = new Label(container, SWT.NONE);
		lblFiles.setText("File pattern(s):");
		final Text txtFiles = new Text(container, SWT.BORDER);
		txtFiles.setText(sFilePatterns);
		txtFiles.setToolTipText("Patterns for the files that should be exposed, separated by commas. '*' means 'any 0+ characters'.");
		txtFiles.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sFilePatterns = txtFiles.getText();
				checkComplete();
			}
		});
		txtFiles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		setControl(container);
		setPageComplete(false);
	}

	private String[] getHawkNames() {
		final HManager hManager = HUIManager.getInstance();
		final Set<HModel> hawks = hManager.getHawks();
		final List<String> hawkNames = new ArrayList<String>();
		for (HModel hawk : hawks) {
			if (hawk.getGraph() != null) {
				hawkNames.add(hawk.getName());
			}
		}
		Collections.sort(hawkNames);
		final String[] arrHawkNames = hawkNames.toArray(new String[hawkNames.size()]);
		return arrHawkNames;
	}

	public String getSelectedInstance() {
		return selectedInstance;
	}

	public boolean isSplit() {
		return isSplit;
	}

	public List<String> getRepositoryPatterns() {
		return Arrays.asList(sRepoPatterns.split(","));
	}

	public List<String> getFilePatterns() {
		return Arrays.asList(sFilePatterns.split(","));
	}

	protected void checkComplete() {
		setPageComplete(selectedInstance != null
			&& sRepoPatterns.length() > 0
			&& sFilePatterns.length() > 0);
	}
}
