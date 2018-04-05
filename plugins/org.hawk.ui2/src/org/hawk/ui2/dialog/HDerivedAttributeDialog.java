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
import org.hawk.osgiserver.HModel;

final class HDerivedAttributeDialog extends HStateBasedDialog {
	String uri = "";
	String type = "TypeDeclaration";
	String name = "isSingleton";
	String atttype = "";
	Boolean isMany = false;
	Boolean isOrdered = false;
	Boolean isUnique = false;
	String derivationlanguage = "";
	String derivationlogic = "return self.bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public==true) and md.modifiers.exists(mod:Modifier|mod.static==true) and md.returnType.isTypeOf(SimpleType) and md.returnType.name.fullyQualifiedName == self.name.fullyQualifiedName);";
	String error = "";

	HDerivedAttributeDialog(HModel hawkModel, Shell parentShell) {
		super(hawkModel, parentShell);
	}

	private boolean check() {

		java.util.List<String> l = hawkModel.validateExpression(
				derivationlanguage, derivationlogic);

		if (l.size() > 0) {

			error = "";

			for (int i = 0; i < l.size(); i++) {
				String s = l.get(i);
				error = error + (i + 1) + ") " + s + "\n";
			}

		} else
			error = "";

		// System.out.println(error);

		return !uri.equals("") && !type.equals("") && !name.equals("")
				&& !atttype.equals("")
				&& !derivationlanguage.equals("")
				&& !derivationlogic.equals("") && l.size() == 0;

	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button ok = getButton(IDialogConstants.OK_ID);
		ok.setText("OK");
		setButtonLayoutData(ok);

		Button ca = getButton(IDialogConstants.CANCEL_ID);

		ca.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				// System.out.println(uri + " " + type + " " + name +
				// " "
				// + atttype + " " + isMany + " " + isOrdered
				// + " " + isUnique + " " + derivationlanguage
				// + " " + derivationlogic);

			}
		});

		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (check()) {
					try {
						hawkModel.addDerivedAttribute(uri, type, name,
								atttype, isMany, isOrdered, isUnique,
								derivationlanguage, derivationlogic);
					} catch (Exception e1) {
						e1.printStackTrace();
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

		Label l = new Label(composite, SWT.NONE);
		l.setText(" Metamodel URI: ");

		final Combo c = new Combo(composite, SWT.READ_ONLY);
		final ArrayList<String> metamodels = hawkModel.getRegisteredMetamodels();
		Collections.sort(metamodels);
		for (String s : metamodels) {
			c.add(s);
		}

		l = new Label(composite, SWT.NONE);
		l.setText(" Type Name: ");

		final Text t = new Text(composite, SWT.BORDER);
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING,
				true, false);
		data.minimumWidth = 200;
		t.setLayoutData(data);
		t.setText(type);

		l = new Label(composite, SWT.NONE);
		l.setText(" Attribute Name: ");

		final Text t2 = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		t2.setLayoutData(data);
		t2.setText(name);

		l = new Label(composite, SWT.NONE);
		l.setText(" Attribute Type: ");

		final Combo cc = new Combo(composite, SWT.READ_ONLY);
		cc.add("String");
		cc.add("Integer");
		cc.add("Boolean");

		cc.select(0);
		atttype = cc.getText();

		l = new Label(composite, SWT.NONE);
		l.setText(" isMany: ");

		final Combo ccc = new Combo(composite, SWT.READ_ONLY);
		ccc.add("True");
		ccc.add("False");

		ccc.select(1);
		isMany = Boolean.parseBoolean(ccc.getText());

		l = new Label(composite, SWT.NONE);
		l.setText(" isOrdered: ");

		final Combo cccc = new Combo(composite, SWT.READ_ONLY);
		cccc.add("True");
		cccc.add("False");

		cccc.select(1);
		isOrdered = Boolean.parseBoolean(cccc.getText());

		l = new Label(composite, SWT.NONE);
		l.setText(" isUnique: ");

		final Combo ccccc = new Combo(composite, SWT.READ_ONLY);
		ccccc.add("True");
		ccccc.add("False");

		ccccc.select(1);
		isUnique = Boolean.parseBoolean(ccccc.getText());

		l = new Label(composite, SWT.NONE);
		l.setText(" Derivation Language: ");

		final Combo cccccc = new Combo(composite, SWT.READ_ONLY);
		for (String s : hawkModel.getKnownQueryLanguages())
			cccccc.add(s);

		cccccc.select(0);
		derivationlanguage = cccccc.getText();

		l = new Label(composite, SWT.NONE);
		l.setText(" Derivation Logic: ");

		final Text t4 = new Text(composite, SWT.MULTI | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);

		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.minimumWidth = 200;
		data.widthHint = 225;
		data.heightHint = 150;
		data.verticalSpan = 2;
		t4.setLayoutData(data);
		t4.setText(derivationlogic);

		final Text t5 = new Text(composite, SWT.MULTI | SWT.WRAP);

		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		data.widthHint = 115;
		data.heightHint = 135;
		t5.setForeground(new Color(getShell().getDisplay(), 255, 0, 0));
		t5.setBackground(composite.getBackground());
		FontData fd = t5.getFont().getFontData()[0];
		Font f = new Font(composite.getDisplay(), fd.getName(),
				fd.getHeight() - 1, SWT.NORMAL);
		t5.setFont(f);
		t5.setLayoutData(data);
		t5.setText("");
		t5.setEditable(false);
		t5.setVisible(true);

		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uri = c.getText();
				evaluateOKEnabled(t5);

			}
		});

		t.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				type = t.getText().trim();
				evaluateOKEnabled(t5);
			}
		});

		t2.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				name = t2.getText().trim();
				evaluateOKEnabled(t5);
			}
		});

		cc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				atttype = cc.getText();
				evaluateOKEnabled(t5);
			}
		});

		ccc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				isMany = Boolean.parseBoolean(ccc.getText());
				evaluateOKEnabled(t5);
			}
		});

		cccc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				isOrdered = Boolean.parseBoolean(cccc.getText());
				evaluateOKEnabled(t5);
			}
		});

		ccccc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				isUnique = Boolean.parseBoolean(ccccc.getText());
				enableIfRunning(hawkModel.getStatus());
				t5.setText(error);
			}
		});

		cccccc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				derivationlanguage = cccccc.getText();
				evaluateOKEnabled(t5);
			}
		});

		t4.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				derivationlogic = t4.getText().trim();
				evaluateOKEnabled(t5);
			}
		});

		setTitle("Create derived attribute");
		setMessage("Specify the configuration of the new derived attribute.");
		return composite;
	}

	private void evaluateOKEnabled(final Text t5) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (check())
			enableIfRunning(hawkModel.getStatus());
		else
			ok.setEnabled(false);
		t5.setText(error);
	}
}