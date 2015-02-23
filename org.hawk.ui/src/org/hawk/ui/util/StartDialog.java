/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.ui.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hawk.ui.HawkUIEclipseViewImpl;

public class StartDialog extends Dialog {

	private HawkUIEclipseViewImpl view;
	private Text passwordField;

	public StartDialog(Shell parentShell, HawkUIEclipseViewImpl view) {
		super(parentShell);
		this.view = view;
	}

	protected Control createDialogArea(Composite parent) {

		Composite comp = (Composite) super.createDialogArea(parent);

		//
		GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 2;

		Label usernameLabel = new Label(comp, SWT.LEFT);
		usernameLabel.setText("Server Admin Password: ");

		passwordField = new Text(comp, SWT.SINGLE | SWT.PASSWORD);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		passwordField.setLayoutData(data);
		// default password for development
		passwordField.setText("samplepw");

		return comp;

	}

	protected void buttonPressed(int buttonId) {

		if (buttonId == 1) {

			this.close();

		} else {

			if (passwordField.getText().length() > 0) {
				view.setAdminPw(passwordField.getText().toCharArray());
				this.close();
			} else {
				showMessage("Please enter your admin password (used to load and store persisted indexers)");
			}

		}
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(this.getShell(), "Empty password field!",
				message);
	}

}
