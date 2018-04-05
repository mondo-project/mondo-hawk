/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hawk.osgiserver.HModel;

final class HIndexedAttributeDialog extends HStateBasedDialog {
	private String uri = "";
	private String type = "Modifier";
	private String name = "static";

	HIndexedAttributeDialog(HModel hModel, Shell parentShell) {
		super(hModel, parentShell);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button ok = getButton(IDialogConstants.OK_ID);
		ok.setText("OK");
		setButtonLayoutData(ok);

		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!uri.equals("") && !type.equals("")
						&& !name.equals("")) {
					try {
						hawkModel.addIndexedAttribute(uri, type, name);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

				}
			}
		});

		ok.setEnabled(false);
	}

	protected Control createDialogArea(Composite parent) {

		parent.getShell().setText("Add an indexed attribute");

		Label label = new Label(parent, SWT.NONE);
		Display display = getShell().getDisplay();
		label.setForeground(new Color(display, 255, 0, 0));
		label.setText("");

		Label l = new Label(parent, SWT.NONE);
		l.setText(" Metamodel URI: ");

		final Combo c = new Combo(parent, SWT.READ_ONLY);
		final ArrayList<String> metamodels = hawkModel.getRegisteredMetamodels();
		Collections.sort(metamodels);
		for (String s : metamodels) {
			c.add(s);
		}
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uri = c.getText();
				Button ok = getButton(IDialogConstants.OK_ID);
				if (!type.equals("") && !uri.equals("")
						&& !name.equals(""))
					enableIfRunning(hawkModel.getStatus());
				else
					ok.setEnabled(false);
			}
		});

		l = new Label(parent, SWT.NONE);
		l.setText("");

		l = new Label(parent, SWT.NONE);
		l.setText(" Type Name: ");

		final Text t = new Text(parent, SWT.BORDER);
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING,
				true, false);
		data.minimumWidth = 200;
		t.setLayoutData(data);
		t.setText(type);
		t.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				type = t.getText().trim();
				Button ok = getButton(IDialogConstants.OK_ID);
				if (!type.equals("") && !uri.equals("")
						&& !name.equals(""))
					enableIfRunning(hawkModel.getStatus());
				else
					ok.setEnabled(false);
			}
		});

		l = new Label(parent, SWT.NONE);
		l.setText("");

		l = new Label(parent, SWT.NONE);
		l.setText(" Attribute Name: ");

		final Text t2 = new Text(parent, SWT.BORDER);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		t2.setLayoutData(data);
		t2.setText(name);
		t2.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				name = t2.getText().trim();
				Button ok = getButton(IDialogConstants.OK_ID);
				if (!type.equals("") && !uri.equals("")
						&& !name.equals(""))
					enableIfRunning(hawkModel.getStatus());
				else
					ok.setEnabled(false);
			}
		});

		l = new Label(parent, SWT.NONE);
		l.setText("");

		setTitle("Create indexed attribute");
		setMessage("Specify the configuration of the new indexed attribute.");
		return parent;
	}
}