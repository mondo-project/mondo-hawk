/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.api.dt.ui;

/* Based on the following code, with modifications:
 *
 * SWT/JFace in Action
 * GUI Design with Eclipse 3.0
 * Matthew Scarpino, Stephen Holder, Stanford Ng, and Laurent Mihalkovic
 *
 * ISBN: 1932394273
 *
 * Publisher: Manning
 */

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hawk.service.api.dt.prefs.CredentialsStore.Credentials;

public class ServerConfigurationDialog extends Dialog {
	private static final int RESET_ID = IDialogConstants.NO_TO_ALL_ID + 1;

	private Text locationField;
	private Text usernameField;
	private Text passwordField;

	private final String title;
	private String username = "admin";
	private String password = "password";
	private String location;

	public ServerConfigurationDialog(Shell parentShell, String title, String location) {
		super(parentShell);
		this.title = title;
		this.location = location;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 2;

		Label locationLabel = new Label(comp, SWT.RIGHT);
		locationLabel.setText("Base URI: ");

		locationField = new Text(comp, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		locationField.setLayoutData(data);
		if (location != null) {
			locationField.setText(location);
		}

		Label usernameLabel = new Label(comp, SWT.RIGHT);
		usernameLabel.setText("Username: ");

		usernameField = new Text(comp, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		usernameField.setLayoutData(data);
		if (username != null) {
			usernameField.setText(username);
		}

		Label passwordLabel = new Label(comp, SWT.RIGHT);
		passwordLabel.setText("Password: ");

		passwordField = new Text(comp, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		passwordField.setLayoutData(data);
		if (password != null) {
			passwordField.setText(password);
		}

		return comp;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, RESET_ID, "Reset All", false);
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == RESET_ID) {
			usernameField.setText("");
			passwordField.setText("");
			locationField.setText(location);
		} else {
			super.buttonPressed(buttonId);
		}
	}

	@Override
	protected void okPressed() {
		username = usernameField.getText();
		password = passwordField.getText();
		location = locationField.getText();
		super.okPressed();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getLocation() {
		return location;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Credentials getCredentials() {
		return new Credentials(username, password);
	}

	public void setCredentials(Credentials creds) {
		setUsername(creds.getUsername());
		setPassword(creds.getPassword());
	}
}
