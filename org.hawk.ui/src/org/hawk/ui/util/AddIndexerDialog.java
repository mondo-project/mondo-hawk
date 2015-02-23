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

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hawk.ui.HawkUIEclipseViewImpl;

public class AddIndexerDialog extends Dialog {

	public AddIndexerDialog(Shell parentShell, HawkUIEclipseViewImpl view) {
		super(parentShell);
		this.view = view;
	}

	private static final int RESET_ID = IDialogConstants.NO_TO_ALL_ID + 1;

	private Text usernameField;

	private Text passwordField;

	private Text locField;

	private Button locfinder;

	private Combo typeField;

	private Text indexerNameField;

	private Text indexNameField;

	private Combo indexTypeField;

	private HawkUIEclipseViewImpl view;

	File genericWorkspaceFile = new File("");
	String par = genericWorkspaceFile.getAbsolutePath().replaceAll("\\\\", "/");

	protected Control createDialogArea(final Composite parent) {

		Composite comp = (Composite) super.createDialogArea(parent);

		GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 3;

		Label usernameLabel = new Label(comp, SWT.LEFT);
		usernameLabel.setText("VCS Username: ");
		// usernameLabel.setEnabled(false);

		usernameField = new Text(comp, SWT.SINGLE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		usernameField.setLayoutData(data);
		usernameField.setText("kb");

		Label l = new Label(comp, SWT.LEFT);
		l.setText("");
		l.setEnabled(false);

		Label passwordLabel = new Label(comp, SWT.LEFT);
		passwordLabel.setText("VCS Password: ");
		// passwordLabel.setEnabled(false);

		passwordField = new Text(comp, SWT.SINGLE | SWT.PASSWORD);
		data = new GridData(GridData.FILL_HORIZONTAL);
		passwordField.setLayoutData(data);
		passwordField.setText("1");
		// passwordField.setSelection(0);

		l = new Label(comp, SWT.LEFT);
		l.setText("");
		l.setEnabled(false);

		Label locLabel = new Label(comp, SWT.LEFT);
		locLabel.setText("VCS location: ");
		// locLabel.setEnabled(false);

		locField = new Text(comp, SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		locField.setLayoutData(data);
		// locField.setText("https://svn.cs.york.ac.uk/svn/sosym/kostas/kb_temp_svn_test_folder");
		locField.setText(new File(par).getParentFile().getAbsolutePath()
				.replaceAll("\\\\", "/")
				+ "workspace/org.hawk.emf/src/org/hawk/emf/model/examples/single/0");

		locfinder = new Button(comp, SWT.NONE);
		locfinder.setText("...");
		locfinder.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {

			}

			@Override
			public void mouseDown(MouseEvent e) {

				DirectoryDialog d = new DirectoryDialog(new Shell(parent
						.getDisplay()));
				d.setMessage("Chose a local folder containing the model files (only to be used with the local folder driver)");
				d.setText("Chose local folder: ");
				// FIXMEdeprecatedui test for other operating systems
				d.setFilterPath(new File(par).getParentFile().getAbsolutePath()
						.replaceAll("\\\\", "/")
						+ "workspace/org.hawk.emf/src/org/hawk/emf/model/examples/single/0");

				String selectedfile = d.open();

				if (selectedfile != null)
					locField.setText(selectedfile);

			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {

			}
		});

		Label typeLabel = new Label(comp, SWT.LEFT);
		typeLabel.setText("VCS type: ");
		// typeLabel.setEnabled(false);

		typeField = new Combo(comp, SWT.READ_ONLY);
		// typeField.set
		for (String s : HawkUIEclipseViewImpl.vcsTypes)
			typeField.add(s);
		// typeField.addSelectionListener();

		typeField.select(0);

		l = new Label(comp, SWT.LEFT);
		l.setText("");
		l.setEnabled(false);

		Label indexer = new Label(comp, SWT.LEFT);
		indexer.setText("Indexer name: ");
		// indexer.setEnabled(false);

		indexerNameField = new Text(comp, SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indexerNameField.setLayoutData(data);
		indexerNameField.setText("indexer1");

		l = new Label(comp, SWT.LEFT);
		l.setText("");
		// l.setEnabled(false);

		Label index = new Label(comp, SWT.LEFT);
		index.setText("Index name: ");
		// index.setEnabled(false);

		indexNameField = new Text(comp, SWT.SINGLE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		indexNameField.setLayoutData(data);
		indexNameField.setText("test_db");

		l = new Label(comp, SWT.LEFT);
		l.setText("");
		// l.setEnabled(false);

		Label indexType = new Label(comp, SWT.LEFT);
		indexType.setText("Index type: ");
		// indexType.setEnabled(false);

		indexTypeField = new Combo(comp, SWT.READ_ONLY);
		for (String s : HawkUIEclipseViewImpl.indexTypes)
			indexTypeField.add(s);
		// indexTypeField.addSelectionListener();

		indexTypeField.select(0);

		// l = new Label(comp, SWT.LEFT);
		// l.setText("");
		// l.setEnabled(false);
		//
		// Label modelType = new Label(comp, SWT.LEFT);
		// modelType.setText("Model type: ");
		// // modelType.setEnabled(false);
		//
		// modelTypeField = new Combo(comp, SWT.READ_ONLY);
		// for (String s : VCSNoSQLUIViewImpl.modelTypes)
		// modelTypeField.add(s);
		//
		// modelTypeField.select(0);

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
			locField.setText("https://");
			typeField.setSelection(new Point(0, 0));
			indexNameField.setText("");
			indexNameField.setText("");
			indexTypeField.setSelection(new Point(0, 0));

		} else if (buttonId == 1) {

			view.setAddNew(false);
			this.close();

		} else {

			if (usernameField.getText().length() > 0
					&& passwordField.getText().length() > 0
					&& locField.getText().length() > 0
					&& typeField.getSelectionIndex() >= 0
					&& indexerNameField.getText().length() > 0
					&& indexNameField.getText().length() > 0
					&& indexTypeField.getSelectionIndex() >= 0) {

				view.setVCSUn(getUn());
				view.setVCSPw(getPw());
				view.setVCSLoc(getLoc());
				view.setVCSType(getVCSType());
				view.setIndexerName(getIndexer());
				view.setIndexName(getIndex());
				view.setIndexType(getIndexType());

				view.setAddNew(true);
				super.buttonPressed(buttonId);

			}

			else {
				showMessage("Please enter your VCS username, password and location as well as selecting the type of VCS, NoSQL store and Model you wish to use and the name of the store");
			}
		}
	}

	private String getVCSType() {
		return typeField.getItem(typeField.getSelectionIndex());
	}

	private String getIndexType() {
		return indexTypeField.getItem(indexTypeField.getSelectionIndex());
	}

	private String getLoc() {
		return locField.getText();
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(this.getShell(),
				"Empty username or password fields!", message);
	}

	public String getUn() {
		return usernameField.getText();
	}

	public String getPw() {
		return passwordField.getText();
	}

	public String getIndexer() {
		return indexerNameField.getText();
	}

	public String getIndex() {
		return indexNameField.getText();
	}

}