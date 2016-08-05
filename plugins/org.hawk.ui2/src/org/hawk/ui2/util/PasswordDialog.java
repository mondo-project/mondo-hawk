/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.ui2.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PasswordDialog extends Dialog {

	private Text passwordField;
	private String passwordString;

	public PasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Please enter admin password");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 2;

		Label passwordLabel = new Label(comp, SWT.RIGHT);
		passwordLabel.setText("Password: ");
		passwordField = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);

		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		passwordField.setLayoutData(data);

		return comp;
	}

	@Override
	protected void okPressed() {
		passwordString = passwordField.getText();
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		passwordField.setText("");
		super.cancelPressed();
	}

	public char[] getPassword() {
		return passwordString.toCharArray();
	}
}
