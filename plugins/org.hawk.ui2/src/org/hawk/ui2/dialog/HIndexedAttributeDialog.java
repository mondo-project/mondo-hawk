/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hawk.core.IMetaModelIntrospector;
import org.hawk.osgiserver.HModel;
import org.hawk.osgiserver.HModelSchedulingRule;
import org.hawk.ui2.util.TypeCascadeSelectionAdapter;
import org.osgi.framework.FrameworkUtil;

final class HIndexedAttributeDialog extends HStateBasedDialog {

	protected class EvaluateOKSelectionListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			evaluateOKEnabled();
		}
	}

	private Combo cmbMetamodel;
	private Combo cmbType;
	private Combo cmbAttributeName;

	private String getMetamodelURI() {
		return cmbMetamodel.getText().trim();
	}

	private String getTargetType() {
		return cmbType.getText().trim();
	}
	
	private String getAttributeName() {
		return cmbAttributeName.getText().trim();
	}

	HIndexedAttributeDialog(HModel hModel, Shell parentShell) {
		super(hModel, parentShell);
	}
	
	@Override
	protected void okPressed() {
		final String uri = getMetamodelURI();
		final String type = getTargetType();
		final String name = getAttributeName();
		
		if (!uri.isEmpty() && !type.isEmpty() && !name.isEmpty()) {
			final String jobName = "Add indexed attribute " + name + " to " + hawkModel.getName();
			Job addIndexedJob = new Job(jobName) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					final String symbolicName = FrameworkUtil.getBundle(getClass()).getSymbolicName();
					try {
						hawkModel.addIndexedAttribute(uri, type, name);
					} catch (Exception e1) {
						return new Status(
							IStatus.ERROR, symbolicName, "Failed", e1
						);
					}
					return new Status(IStatus.OK, symbolicName, "Done");
				}
			};
			addIndexedJob.setRule(new HModelSchedulingRule(hawkModel));
			addIndexedJob.schedule();
		}

		super.okPressed();
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Button ok = getButton(IDialogConstants.OK_ID);
		ok.setText("OK");
		ok.setEnabled(false);
	}

	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText("Add an indexed attribute");

		Composite container = new Composite(parent, SWT.FILL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label l = new Label(container, SWT.NONE);
		l.setText("Metamodel URI:");

		cmbMetamodel = new Combo(container, SWT.READ_ONLY);
		final List<String> metamodels = hawkModel.getRegisteredMetamodels();
		Collections.sort(metamodels);
		for (String s : metamodels) {
			cmbMetamodel.add(s);
		}
		cmbMetamodel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		l = new Label(container, SWT.NONE);
		l.setText("Target Type:");

		cmbType = new Combo(container, SWT.BORDER);
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		cmbType.setLayoutData(data);

		l = new Label(container, SWT.NONE);
		l.setText("Attribute Name:");

		cmbAttributeName = new Combo(container, SWT.BORDER);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		cmbAttributeName.setLayoutData(data);

		cmbMetamodel.addSelectionListener(new TypeCascadeSelectionAdapter(hawkModel, cmbType));
		cmbMetamodel.addSelectionListener(new EvaluateOKSelectionListener());
		cmbType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String metamodelURI = getMetamodelURI();
				final String typeName = getTargetType();

				cmbAttributeName.removeAll();
				final IMetaModelIntrospector introspector = hawkModel.getIntrospector();
				if (introspector != null) {
					List<String> attributes = introspector.getAttributes(metamodelURI, typeName);
					for (String sAttribute : attributes) {
						cmbAttributeName.add(sAttribute);
					}
					cmbAttributeName.select(0);
				}
			}
		});
		cmbType.addModifyListener(this::evaluateOKEnabled);
		cmbAttributeName.addModifyListener(this::evaluateOKEnabled);

		setTitle("Create indexed attribute");
		setMessage("Specify the configuration of the new indexed attribute.");
		return parent;
	}

	private void evaluateOKEnabled(ModifyEvent e) {
		evaluateOKEnabled();
	}

	private void evaluateOKEnabled() {
		final String uri = getMetamodelURI();
		final String type = getTargetType();
		final String name = getAttributeName();

		Button ok = getButton(IDialogConstants.OK_ID);
		if (!uri.isEmpty() && !type.isEmpty() && !name.isEmpty()) {
			enableIfRunning(hawkModel.getStatus());
		} else {
			ok.setEnabled(false);
		}
	}
}