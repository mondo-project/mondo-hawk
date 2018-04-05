/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hawk.core.IVcsManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.dialog.HConfigDialog.ClassNameLabelProvider;

final class HVCSDialog extends TitleAreaDialog {
	private final class UpdateDialogModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			updateDialog();
		}
	}

	private final IVcsManager managerToEdit;
	private final HModel hawkModel;
	private Button freeze;
	private ComboViewer cmbVCSType;
	private Text txtVCSLocation;
	private Button btnVCSBrowse;
	private Text txtUser;
	private Text txtPass;

	public HVCSDialog(Shell parentShell, HModel hawkModel, IVcsManager managerToEdit) {
		super(parentShell);
		this.hawkModel = hawkModel;
		this.managerToEdit = managerToEdit;
	}

	@Override
	public void create() {
		super.create();
		if (managerToEdit == null) {
			setTitle("Add repository");
			setMessage(
					"Select the repository type, enter the location and optionally enter the authentication credentials.");
		} else {
			setTitle("Edit repository");
			setMessage("Change the authentication credentials.");
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		final Control contents = super.createContents(parent);
		updateDialog();
		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);
		final Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout containerLayout = new GridLayout();
		containerLayout.numColumns = 3;
		container.setLayout(containerLayout);

		// Type
		final Label lblType = new Label(container, SWT.NONE);
		lblType.setText("Type:");
		cmbVCSType = new ComboViewer(container, SWT.READ_ONLY);
		cmbVCSType.setLabelProvider(new ClassNameLabelProvider());
		cmbVCSType.setContentProvider(new ArrayContentProvider());
		final List<IVcsManager> availableVCS = hawkModel.getVCSInstances();
		cmbVCSType.setInput(availableVCS.toArray());
		final GridData cmbVCSTypeLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		cmbVCSTypeLayoutData.horizontalSpan = 2;
		cmbVCSType.getCombo().setLayoutData(cmbVCSTypeLayoutData);
		if (managerToEdit != null) {
			final String managerType = managerToEdit.getType();

			int i = 0;
			for (IVcsManager kind : availableVCS) {
				final String availableType = kind.getType();
				if (availableType.equals(managerType)) {
					cmbVCSType.getCombo().select(i);
					break;
				}
				i++;
			}
			cmbVCSType.getCombo().setEnabled(false);
		} else {
			cmbVCSType.getCombo().select(0);
		}
		cmbVCSType.getCombo().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				txtVCSLocation.setText("");
				updateDialog();
			}
		});

		// Location + browse
		final Label lblLocation = new Label(container, SWT.NONE);
		lblLocation.setText("Location:");
		txtVCSLocation = new Text(container, SWT.BORDER);
		final GridData txtVCSLocationLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		txtVCSLocationLayoutData.widthHint = 300;
		txtVCSLocation.setLayoutData(txtVCSLocationLayoutData);
		if (managerToEdit != null) {
			txtVCSLocation.setText(managerToEdit.getLocation());
			txtVCSLocation.setEnabled(false);
		}
		txtVCSLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateDialog();
			}
		});

		btnVCSBrowse = new Button(container, SWT.PUSH);
		GridData gridDataB = new GridData();
		btnVCSBrowse.setLayoutData(gridDataB);
		btnVCSBrowse.setText("Browse...");
		btnVCSBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);

				dd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString());
				dd.setMessage("Select a folder to add to the indexer");
				dd.setText("Select a directory");
				String result = dd.open();

				if (result != null) {
					txtVCSLocation.setText(new File(result).toURI().toString());
				}
			}
		});
		btnVCSBrowse.setEnabled(false);

		String usernameToEdit = null;
		String passwordToEdit = null;
		if (managerToEdit != null) {
			usernameToEdit = managerToEdit.getUsername();
			passwordToEdit = managerToEdit.getPassword();
		}

		final Label lblUser = new Label(container, SWT.NONE);
		lblUser.setText("User (optional):");
		txtUser = new Text(container, SWT.BORDER | SWT.SINGLE);
		final GridData txtUserLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		txtUserLayoutData.horizontalSpan = 2;
		txtUser.setLayoutData(txtUserLayoutData);
		if (usernameToEdit != null) {
			txtUser.setText(usernameToEdit);
		}
		txtUser.addModifyListener(new UpdateDialogModifyListener());

		final Label lblPass = new Label(container, SWT.NONE);
		lblPass.setText("Pass (optional):");
		txtPass = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		final GridData txtPassLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		txtPassLayoutData.horizontalSpan = 2;
		txtPass.setLayoutData(txtPassLayoutData);
		if (passwordToEdit != null) {
			txtPass.setText(passwordToEdit);
		}
		txtPass.addModifyListener(new UpdateDialogModifyListener());

		final Label lblFreeze = new Label(container, SWT.NONE);
		lblFreeze.setText("Freeze repo:");

		freeze = new Button(container, SWT.CHECK);
		boolean isFrozen = managerToEdit == null ? false : managerToEdit.isFrozen();
		freeze.setSelection(isFrozen);

		if (managerToEdit == null)
			freeze.setEnabled(false);

		return container;
	}

	private boolean isAuthSupported() {
		final IVcsManager vcsManager = getSelectedVCSManager();
		return vcsManager != null && vcsManager.isAuthSupported();
	}

	private IVcsManager getSelectedVCSManager() {
		final IStructuredSelection selection = (IStructuredSelection) cmbVCSType.getSelection();
		return (IVcsManager) selection.getFirstElement();
	}

	private boolean isLocationValid() {
		IVcsManager vcsManager = getSelectedVCSManager();
		return vcsManager.isPathLocationAccepted() && isLocationValidPath()
				|| vcsManager.isURLLocationAccepted() && isLocationValidURI();
	}

	private void updateDialog() {
		final boolean authEnabled = isAuthSupported() && isLocationValid();
		txtUser.setEnabled(authEnabled);
		txtPass.setEnabled(authEnabled);

		if (managerToEdit == null) {
			final IVcsManager vcsManager = getSelectedVCSManager();
			btnVCSBrowse.setEnabled(vcsManager != null && vcsManager.isPathLocationAccepted());
		}

		if (!isLocationValid()) {
			setErrorMessage("The location is not valid");
		} else if (getSelectedVCSManager() == null) {
			setErrorMessage("No VCS manager type has been selected");
		} else if ("".equals(txtUser.getText()) != "".equals(txtPass.getText())) {
			setErrorMessage("The username and password must be empty or not empty at the same time");
		} else {
			setErrorMessage(null);
		}
	}

	@Override
	public void setErrorMessage(String newErrorMessage) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null)
			okButton.setEnabled(newErrorMessage == null);
		super.setErrorMessage(newErrorMessage);
	}

	private boolean isLocationValidPath() {
		File dir = new File(txtVCSLocation.getText());
		if (!isAuthSupported() && dir.exists() && dir.isDirectory() && dir.canRead())
			return true;
		return false;
	}

	private boolean isLocationValidURI() {
		try {
			URI uri = new URI(txtVCSLocation.getText());
			return uri.getScheme() != null && uri.getPath() != null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	@Override
	protected void okPressed() {

		final String vcsType = getSelectedVCSManager().getClass().getName();
		final String location = txtVCSLocation.getText();
		final String user = txtUser.getText();
		final String pass = txtPass.getText();

		if (managerToEdit == null) {
			hawkModel.addVCS(location, vcsType, user, pass, freeze.getSelection());
		} else {
			managerToEdit.setCredentials(user, pass, hawkModel.getManager().getCredentialsStore());
			managerToEdit.setFrozen(freeze.getSelection());
		}
		super.okPressed();
	}

}