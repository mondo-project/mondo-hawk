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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.hawk.osgiserver.HModel;

final class HDerivedAttributeDialog extends HStateBasedDialog {

	protected class EvaluateSelectionListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			evaluateOKEnabled();
		}
	}

	protected class EvaluateOKModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			evaluateOKEnabled();
		}
	}

	private static final String DEFAULT_TYPE = "TypeDeclaration";
	private static final String DEFAULT_NAME = "isSingleton";
	private static final String DEFAULT_DERIVATION_LOGIC =
			"return self.bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public==true) and md.modifiers.exists(mod:Modifier|mod.static==true) and md.returnType.isTypeOf(SimpleType) and md.returnType.name.fullyQualifiedName == self.name.fullyQualifiedName);";
	
	private Combo cmbMetamodel;
	private Text txtType;
	private Text txtName;

	private Combo cmbValueType, cmbIsMany, cmbIsOrdered, cmbIsUnique;
	private Combo cmbDerivationLanguage;
	private Text txtDerivationLogic;

	private Text txtErrorMessages;

	HDerivedAttributeDialog(HModel hawkModel, Shell parentShell) {
		super(hawkModel, parentShell);
	}

	private String getDerivationLanguage() {
		return cmbDerivationLanguage.getText().trim();
	}
	
	private String getDerivationLogic() {
		return txtDerivationLogic.getText().trim();
	}
	
	private String getMetamodelURI() {
		return cmbMetamodel.getText().trim();
	}
	
	private boolean isMany() {
		return Boolean.parseBoolean(cmbIsMany.getText().trim());
	}

	private boolean isOrdered() {
		return Boolean.parseBoolean(cmbIsOrdered.getText().trim());
	}

	private boolean isUnique() {
		return Boolean.parseBoolean(cmbIsUnique.getText().trim());
	}
	
	private String getTargetType() {
		return txtType.getText().trim();
	}
	
	private String getPropertyName() {
		return txtName.getText().trim();
	}
	
	private String getValueType() {
		return cmbValueType.getText().trim();
	}
	
	private boolean check() {
		final String derivationLanguage = getDerivationLanguage();
		final String derivationLogic = getDerivationLogic();
		List<String> l = hawkModel.validateExpression(derivationLanguage, derivationLogic);

		String error;
		if (l.size() > 0) {
			error = "";
			for (int i = 0; i < l.size(); i++) {
				String s = l.get(i);
				error = error + (i + 1) + ") " + s + "\n";
			}
		} else {
			error = "";
		}
		txtErrorMessages.setText(error);

		final String uri = getMetamodelURI();
		final String type = getTargetType();
		final String name = getPropertyName();
		final String valueType = getValueType();

		return !uri.equals("") && !type.equals("") && !name.equals("")
				&& !valueType.equals("")
				&& !derivationLanguage.equals("")
				&& !derivationLogic.equals("") && l.size() == 0;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button ok = getButton(IDialogConstants.OK_ID);
		ok.setText("OK");
		setButtonLayoutData(ok);

		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (check()) {
					final String metamodelURI = getMetamodelURI();
					final String targetType = getTargetType();
					final String propertyName = getPropertyName();
					final String valueType = getValueType();
					final boolean bIsMany = isMany();
					final boolean bIsOrdered = isOrdered();
					final boolean bIsUnique = isUnique();
					final String derivationLanguage = getDerivationLanguage();
					final String derivationLogic = getDerivationLogic();
					
					try {
						hawkModel.addDerivedAttribute(metamodelURI, targetType, propertyName,
								valueType, bIsMany, bIsOrdered, bIsUnique,
								derivationLanguage, derivationLogic);
					} catch (Exception e1) {
						org.hawk.ui2.Activator.logError(e1.getMessage(), e1);
					}
				}
			}
		});

		ok.setEnabled(false);
	}

	protected Control createDialogArea(Composite parent) {
		super.createDialogArea(parent);

		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayout la = new GridLayout();
		la.numColumns = 2;
		composite.setLayout(la);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label l = new Label(composite, SWT.NONE);
		l.setText("Metamodel URI:");
		cmbMetamodel = new Combo(composite, SWT.READ_ONLY);
		final ArrayList<String> metamodels = hawkModel.getRegisteredMetamodels();
		Collections.sort(metamodels);
		for (String s : metamodels) {
			cmbMetamodel.add(s);
		}

		l = new Label(composite, SWT.NONE);
		l.setText("Target Type:");
		txtType = new Text(composite, SWT.BORDER);
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		txtType.setLayoutData(data);
		txtType.setText(DEFAULT_TYPE);

		l = new Label(composite, SWT.NONE);
		l.setText("Property Name:");
		txtName = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		txtName.setLayoutData(data);
		txtName.setText(DEFAULT_NAME);

		l = new Label(composite, SWT.NONE);
		l.setText("Attribute Type:");
		cmbValueType = new Combo(composite, SWT.READ_ONLY);
		cmbValueType.add("String");
		cmbValueType.add("Integer");
		cmbValueType.add("Boolean");
		cmbValueType.select(0);

		l = new Label(composite, SWT.NONE);
		l.setText("isMany: ");
		cmbIsMany = new Combo(composite, SWT.READ_ONLY);
		cmbIsMany.add("True");
		cmbIsMany.add("False");
		cmbIsMany.select(1);

		l = new Label(composite, SWT.NONE);
		l.setText("isOrdered:");
		cmbIsOrdered = new Combo(composite, SWT.READ_ONLY);
		cmbIsOrdered.add("True");
		cmbIsOrdered.add("False");
		cmbIsOrdered.select(1);

		l = new Label(composite, SWT.NONE);
		l.setText("isUnique:");
		cmbIsUnique = new Combo(composite, SWT.READ_ONLY);
		cmbIsUnique.add("True");
		cmbIsUnique.add("False");
		cmbIsUnique.select(1);

		l = new Label(composite, SWT.NONE);
		l.setText("Derivation Language:");
		cmbDerivationLanguage = new Combo(composite, SWT.READ_ONLY);
		for (String s : hawkModel.getKnownQueryLanguages()) {
			cmbDerivationLanguage.add(s);
		}
		cmbDerivationLanguage.select(0);

		l = new Label(composite, SWT.NONE);
		l.setText("Derivation Logic:");
		txtDerivationLogic = new Text(composite,
				SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL
		);
		txtDerivationLogic.setText(DEFAULT_DERIVATION_LOGIC);
		data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		data.widthHint = 225;
		data.heightHint = 150;
		data.verticalSpan = 2;
		txtDerivationLogic.setLayoutData(data);
		
		Button btnLoadFromWorkspace = new Button(composite, SWT.NONE);
		btnLoadFromWorkspace.setText("Load from workspace...");
		data = new GridData(SWT.TRAIL, SWT.BEGINNING, false, false);
		data.horizontalSpan = 2;
		data.verticalSpan = 1;
		btnLoadFromWorkspace.setLayoutData(data);
		btnLoadFromWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				loadLogicFromWorkspace();
			}
		});

		txtErrorMessages = new Text(composite, SWT.MULTI | SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalSpan = 2;
		txtErrorMessages.setForeground(new Color(getShell().getDisplay(), 255, 0, 0));
		txtErrorMessages.setBackground(composite.getBackground());
		FontData fd = txtErrorMessages.getFont().getFontData()[0];
		Font f = new Font(composite.getDisplay(), fd.getName(),
				fd.getHeight() - 1, SWT.NORMAL);
		txtErrorMessages.setFont(f);
		txtErrorMessages.setLayoutData(data);
		txtErrorMessages.setText("");
		txtErrorMessages.setEditable(false);
		txtErrorMessages.setVisible(true);

		final EvaluateSelectionListener selectionListener = new EvaluateSelectionListener();
		cmbMetamodel.addSelectionListener(selectionListener);
		txtType.addModifyListener(this::evaluateOKEnabled);
		txtName.addModifyListener(this::evaluateOKEnabled);
		cmbValueType.addSelectionListener(selectionListener);
		cmbIsMany.addSelectionListener(selectionListener);
		cmbIsOrdered.addSelectionListener(selectionListener);
		cmbIsUnique.addSelectionListener(selectionListener);
		cmbDerivationLanguage.addSelectionListener(selectionListener);
		txtDerivationLogic.addModifyListener(this::evaluateOKEnabled);

		setTitle("Create derived attribute");
		setMessage("Specify the configuration of the new derived attribute.");
		return composite;
	}

	protected void loadLogicFromWorkspace() {
		FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(
			getShell(), false, ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE
		);

		dialog.setInitialPattern("*.eol");
		dialog.setTitle("Load derivation logic from workspace");
		dialog.setMessage("Select a script from the workspace");
		dialog.open();
		
		if (dialog.getReturnCode() == Window.OK){
			IFile iFile = (IFile) dialog.getResult()[0];

			StringBuilder sb = new StringBuilder();
			try (InputStreamReader isReader = new InputStreamReader(iFile.getContents(), iFile.getCharset()); BufferedReader bR = new BufferedReader(isReader)) {
				String line;
				while ((line = bR.readLine()) != null) {
					sb.append(line);
					sb.append(System.lineSeparator());
				}
			} catch (IOException | CoreException e) {
				org.hawk.ui2.Activator.logError(e.getMessage(), e);
			}

			txtDerivationLogic.setText(sb.toString());
			evaluateOKEnabled();
		}
	}

	private void evaluateOKEnabled(ModifyEvent e) {
		evaluateOKEnabled();
	}

	private void evaluateOKEnabled() {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (check()) {
			enableIfRunning(hawkModel.getStatus());
		}
		else {
			ok.setEnabled(false);
		}
	}
}