/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.view.HView;

public class HConfigDialog extends Dialog {

	private HModel index;
	private List mmList;
	private List daList;
	private List iaList;
	private List iList;

	public HConfigDialog(Shell parentShell, HModel in) {
		super(parentShell);
		setShellStyle(getShellStyle() & ~SWT.CLOSE);

		index = in;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button cancel = getButton(IDialogConstants.OK_ID);
		cancel.setText("Done");
		setButtonLayoutData(cancel);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		if (id == IDialogConstants.CANCEL_ID)
			return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	protected Control createDialogArea(Composite parent) {

		try {

			setDefaultImage(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(HView.ID)
					.getTitleImage());

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		TabFolder tabFolder = new TabFolder(parent, SWT.BORDER);

		TabItem metamodelTab = new TabItem(tabFolder, SWT.NULL);
		metamodelTab.setText("Metamodels");
		metamodelTab.setControl(mmTab(tabFolder));

		TabItem vcsTab = new TabItem(tabFolder, SWT.NULL);
		vcsTab.setText("Indexed Locations");
		vcsTab.setControl(vcsTab(tabFolder));

		TabItem dTab = new TabItem(tabFolder, SWT.NULL);
		dTab.setText("Derived Attributes");
		dTab.setControl(dTab(tabFolder));

		TabItem iTab = new TabItem(tabFolder, SWT.NULL);
		iTab.setText("Indexed Attributes");
		iTab.setControl(iTab(tabFolder));

		TabItem aTab = new TabItem(tabFolder, SWT.NULL);
		aTab.setText("All indexes");
		aTab.setControl(aTab(tabFolder));

		tabFolder.pack();
		return tabFolder;
	}

	private Composite dTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		daList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		daList.setLayoutData(gridDataQ);

		updateDAList();

		Button remove = new Button(composite, SWT.PUSH);
		remove.setText("Remove");
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// remove action
			}
		});

		Button b = new Button(composite, SWT.NONE);
		b.setText("Add...");
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dAdd();
			}
		});

		return composite;
	}

	private Composite iTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		iaList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		iaList.setLayoutData(gridDataQ);

		updateIAList();

		Button remove = new Button(composite, SWT.PUSH);
		remove.setText("Remove");
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// remove action
			}
		});

		Button b = new Button(composite, SWT.NONE);
		b.setText("Add...");
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				iAdd();
			}
		});

		return composite;
	}

	private Composite aTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);

		iList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		iList.setLayoutData(gridDataQ);

		updateIList();

		Button b = new Button(composite, SWT.NONE);
		b.setText("Refresh");
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateIList();
			}
		});

		return composite;

	}

	private void iAdd() {

		Dialog d = new Dialog(this) {

			String uri = "";
			String type = "Modifier";
			String name = "static";

			protected Control createDialogArea(Composite parent) {

				parent.getShell().setText("Add an indexed attribute");

				Label label = new Label(parent, SWT.NONE);
				Display display = getShell().getDisplay();
				label.setForeground(new Color(display, 255, 0, 0));
				label.setText("");

				Label l = new Label(parent, SWT.NONE);
				l.setText(" Metamodel URI: ");

				final Combo c = new Combo(parent, SWT.READ_ONLY);
				for (String s : index.getRegisteredMetamodels())
					c.add(s);
				c.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						uri = c.getText();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (!type.equals("") && !uri.equals("") && !name.equals(""))
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
					}
				});

				l = new Label(parent, SWT.NONE);
				l.setText("");

				l = new Label(parent, SWT.NONE);
				l.setText(" Type Name: ");

				final Text t = new Text(parent, SWT.BORDER);
				GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
				data.minimumWidth = 200;
				t.setLayoutData(data);
				t.setText(type);
				t.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						type = t.getText().trim();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (!type.equals("") && !uri.equals("") && !name.equals(""))
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
					}
				});

				// Button b = new Button(composite, SWT.NONE);
				// data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false,
				// false);
				// b.setLayoutData(data);
				// b.setText("Suggested Types");

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
						if (!type.equals("") && !uri.equals("") && !name.equals(""))
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
					}
				});

				l = new Label(parent, SWT.NONE);
				l.setText("");

				return parent;
			}

			protected void createButtonsForButtonBar(Composite parent) {
				super.createButtonsForButtonBar(parent);

				Button ok = getButton(IDialogConstants.OK_ID);
				ok.setText("OK");
				setButtonLayoutData(ok);

				ok.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (!uri.equals("") && !type.equals("") && !name.equals("")) {

							// System.err.println(">");
							// System.err.println(uri);
							// System.err.println(type);
							// System.err.println(name);
							// System.err.println("<");

							try {
								index.addIndexedAttribute(uri, type, name);
							} catch (Exception e1) {
								e1.printStackTrace();
							}

						}
					}
				});

				ok.setEnabled(false);
			}

		};

		d.setBlockOnOpen(true);
		if (d.open() == Window.OK)
			updateIAList();

	}

	private void dAdd() {

		Dialog d = new Dialog(this) {

			String uri = "";
			String type = "TypeDeclaration";
			String name = "isSingleton";
			String atttype = "";
			Boolean isMany = false;
			Boolean isOrdered = false;
			Boolean isUnique = false;
			String derivationlanguage = "";
			String derivationlogic = "self.bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public==true) and md.modifiers.exists(mod:Modifier|mod.static==true) and md.returnType.isTypeOf(SimpleType) and md.returnType.name.fullyQualifiedName == self.name.fullyQualifiedName)";
			String error = "";

			private boolean check() {

				java.util.List<String> l = index.validateExpression(derivationlanguage, derivationlogic);

				if (l.size() > 0) {

					error = "";

					for (int i = 0; i < l.size(); i++) {
						String s = l.get(i);
						error = error + (i + 1) + ") " + s + "\n";
					}

				} else
					error = "";

				// System.out.println(error);

				return !uri.equals("") && !type.equals("") && !name.equals("") && !atttype.equals("")
						&& !derivationlanguage.equals("") && !derivationlogic.equals("") && l.size() == 0;

			}

			protected Control createDialogArea(Composite parent) {

				parent.getShell().setText("Add a derived attribute");

				Composite composite = (Composite) super.createDialogArea(parent);

				GridLayout la = new GridLayout();
				la.numColumns = 2;

				composite.setLayout(la);

				// Label label = new Label(composite, SWT.NONE);
				// Display display = getShell().getDisplay();
				// label.setForeground(new Color(display, 255, 0, 0));
				// label.setText("");

				Label l = new Label(composite, SWT.NONE);
				l.setText(" Metamodel URI: ");

				final Combo c = new Combo(composite, SWT.READ_ONLY);
				for (String s : index.getRegisteredMetamodels())
					c.add(s);

				l = new Label(composite, SWT.NONE);
				l.setText(" Type Name: ");

				final Text t = new Text(composite, SWT.BORDER);
				GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
				data.minimumWidth = 200;
				t.setLayoutData(data);
				t.setText(type);

				// Button b = new Button(composite, SWT.NONE);
				// data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false,
				// false);
				// b.setLayoutData(data);
				// b.setText("Suggested Types");

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
				for (String s : index.getKnownQueryLanguages())
					cccccc.add(s);

				cccccc.select(0);
				derivationlanguage = cccccc.getText();

				l = new Label(composite, SWT.NONE);
				l.setText(" Derivation Logic: ");

				final Text t4 = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

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
				Font f = new Font(composite.getDisplay(), fd.getName(), fd.getHeight() - 1, SWT.NORMAL);
				t5.setFont(f);
				t5.setLayoutData(data);
				t5.setText("");
				t5.setEditable(false);
				t5.setVisible(true);

				c.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						uri = c.getText();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);

					}
				});

				t.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						type = t.getText().trim();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				t2.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						name = t2.getText().trim();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				cc.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						atttype = cc.getText();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				ccc.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						isMany = Boolean.parseBoolean(ccc.getText());
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				cccc.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						isOrdered = Boolean.parseBoolean(cccc.getText());
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				ccccc.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						isUnique = Boolean.parseBoolean(ccccc.getText());
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				cccccc.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						derivationlanguage = cccccc.getText();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				t4.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						derivationlogic = t4.getText().trim();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(true);
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				return composite;
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

							// System.err.println(">");
							// System.err.println(uri);
							// System.err.println(type);
							// System.err.println(name);
							// System.err.println("<");

							try {
								index.addDerivedAttribute(uri, type, name, atttype, isMany, isOrdered, isUnique,
										derivationlanguage, derivationlogic);
							} catch (Exception e1) {
								e1.printStackTrace();
							}

						}
					}
				});

				ok.setEnabled(false);
			}

		};

		d.setBlockOnOpen(true);
		if (d.open() == Window.OK)
			updateDAList();

	}

	private Composite mmTab(TabFolder parent) {

		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		mmList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		mmList.setLayoutData(gridDataQ);

		updateMMList();

		Button remove = new Button(composite, SWT.PUSH);
		remove.setText("Remove");
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// remove action
			}
		});

		Button browse = new Button(composite, SWT.PUSH);
		browse.setText("Add...");
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mmBrowse();
			}
		});

		return composite;
	}

	private void mmBrowse() {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI);

		fd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString());
		// TODO: allow selection of only parse-able/known metamodels-file-types
		fd.setFilterExtensions(new String[] { "*.ecore" });
		fd.setText("Select metamodels");
		String result = fd.open();

		if (result != null) {

			String[] metaModels = fd.getFileNames();
			File[] metaModelFiles = new File[metaModels.length];

			// System.err.println(fd.getFilterPath());
			// System.err.println(Arrays.toString(metaModels));

			boolean error = false;

			for (int i = 0; i < metaModels.length; i++) {
				File file = new File(fd.getFilterPath() + File.separator + metaModels[i]);
				if (!file.exists() || !file.canRead() || !file.isFile())
					error = true;
				else
					metaModelFiles[i] = file;

			}

			if (!error) {
				index.registerMeta(metaModelFiles);
				updateMMList();
			}
		}

	}

	private void updateMMList() {
		mmList.removeAll();
		for (String mm : index.getRegisteredMetamodels()) {
			mmList.add(mm);
		}
	}

	private void updateLocList() {
		locList.removeAll();
		for (String loc : index.getLocations()) {
			locList.add(loc);
		}
	}

	private void updateDAList() {
		daList.removeAll();
		for (String da : index.getDerivedAttributes()) {
			daList.add(da);
		}
		String[] items = daList.getItems();
		java.util.Arrays.sort(items);
		daList.setItems(items);
	}

	private void updateIAList() {
		iaList.removeAll();
		for (String ia : index.getIndexedAttributes()) {
			iaList.add(ia);
		}
		String[] items = iaList.getItems();
		java.util.Arrays.sort(items);
		iaList.setItems(items);
	}

	private void updateIList() {
		iList.removeAll();
		for (String i : index.getIndexes()) {
			iList.add(i);
		}
		String[] items = iList.getItems();
		java.util.Arrays.sort(items);
		iList.setItems(items);
	}

	private Combo combo;

	private Composite vcsTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 5;
		composite.setLayout(gridLayout);

		locList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 5;
		locList.setLayoutData(gridDataQ);
		updateLocList();

		// combo (VCS types)
		combo = new Combo(composite, SWT.READ_ONLY);
		// ask HModel for a list of supported VCS types
		combo.setItems(index.getVCSTypeNames().toArray(new String[0]));
		GridData gridDataC = new GridData();
		gridDataC.grabExcessHorizontalSpace = false;
		gridDataC.widthHint = 200;
		gridDataC.minimumWidth = 200;
		gridDataC.horizontalAlignment = GridData.FILL_BOTH;
		combo.setLayoutData(gridDataC);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				location.setText("");
				setAuthBrowseButton();
			}
		});

		location = new Text(composite, SWT.BORDER);
		GridData gridDataR = new GridData();
		gridDataR.widthHint = 250;
		gridDataR.grabExcessHorizontalSpace = true;
		gridDataR.horizontalAlignment = GridData.FILL_BOTH;
		location.setLayoutData(gridDataR);
		location.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setAuthBrowseButton();
				setAddButton();
			}
		});

		authBrowse = new Button(composite, SWT.PUSH);
		GridData gridDataA = new GridData();
		gridDataA.widthHint = 105;
		authBrowse.setLayoutData(gridDataA);

		setAuthBrowseButton();
		// just until the first event
		authBrowse.setEnabled(false);

		add = new Button(composite, SWT.PUSH);
		add.setText("Add");
		add.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				index.addVCS(location.getText(), combo.getText(), user, pass);
				location.setText("");
				// combo.setItems (index.getVCSTypeNames().toArray(new
				// String[0]));
				setAuthBrowseButton();
				setAddButton();
				updateLocList();
			}
		});

		add.setEnabled(false);

		return composite;
	}

	private Button authBrowse = null;

	private void setAuthBrowseButton() {
		if (authBrowse != null) {
			if (authRequired() && !authBrowse.getText().equals("Authenticate...")) {
				authBrowse.removeListener(SWT.Selection, browseFolder);
				authBrowse.addListener(SWT.Selection, authenticate);
				authBrowse.setText("Authenticate...");
			}
			if (!authRequired() && !authBrowse.getText().equals("Browse...")) {
				authBrowse.removeListener(SWT.Selection, authenticate);
				authBrowse.addListener(SWT.Selection, browseFolder);
				authBrowse.setText("Browse...");
			}
			if (authRequired() && !validSVNLoc())
				authBrowse.setEnabled(false);
			else
				authBrowse.setEnabled(true);
		}
	}

	private Listener browseFolder = new Listener() {
		public void handleEvent(Event e) {
			DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);

			dd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString());
			dd.setMessage("Select a folder to add to the indexer");
			dd.setText("Select a directory");
			String result = dd.open();

			if (result != null) {
				location.setText(result);
			}
			setAddButton();
		}
	};

	private boolean userPassOK = false;
	private String user = "";
	private String pass = "";

	private Listener authenticate = new Listener() {
		public void handleEvent(Event e) {

			UsernamePasswordDialog upd = new UsernamePasswordDialog(getShell());
			userPassOK = (upd.open() == Window.OK);
			if (userPassOK) {
				user = upd.getUsername();
				pass = upd.getPassword();
			}
			setAddButton();
		}
	};

	private boolean validateSVN() {
		if (authRequired() && userPassOK && !user.equals("") && !pass.equals("")) {
			// could do an SVN connect test
			return validSVNLoc();
		}
		return false;
	}

	private boolean validSVNLoc() {
		try {
			new URL(location.getText());
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private boolean validateFolder() {
		File dir = new File(location.getText());
		if (!authRequired() && dir.exists() && dir.isDirectory() && dir.canRead())
			return true;
		return false;
	}

	private void setAddButton() {
		if (validateSVN())
			add.setEnabled(true);
		else if (validateFolder())
			add.setEnabled(true);
		else
			add.setEnabled(false);
	}

	private Text location;
	private Button add;
	private List locList;

//	private void showMessage(String message) {
//		MessageDialog.openInformation(getShell(), "Hawk", message);
//	}

	private boolean authRequired() {
		if (combo.getSelectionIndex() < 0)
			return false;
		String selection = combo.getItem(combo.getSelectionIndex());
		return selection.toLowerCase().contains("svn") || selection.toLowerCase().contains("git");
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configure Indexer: " + index.getName());
	}

	public class UsernamePasswordDialog extends Dialog {
		private static final int RESET_ID = IDialogConstants.NO_TO_ALL_ID + 1;

		private Text usernameField;
		private Text passwordField;

		private String password;
		private String user;

		public UsernamePasswordDialog(Shell parentShell) {
			super(parentShell);
		}

		protected Control createDialogArea(Composite parent) {
			Composite comp = (Composite) super.createDialogArea(parent);

			GridLayout layout = (GridLayout) comp.getLayout();
			layout.numColumns = 2;

			Label usernameLabel = new Label(comp, SWT.RIGHT);
			usernameLabel.setText("Username: ");

			usernameField = new Text(comp, SWT.SINGLE);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			usernameField.setLayoutData(data);

			Label passwordLabel = new Label(comp, SWT.RIGHT);
			passwordLabel.setText("Password: ");

			passwordField = new Text(comp, SWT.SINGLE | SWT.PASSWORD);
			data = new GridData(GridData.FILL_HORIZONTAL);
			passwordField.setLayoutData(data);

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
			} else {
				super.buttonPressed(buttonId);
			}
		}

		public String getUsername() {
			return this.user;
		}

		public String getPassword() {
			return this.password;
		}

		protected void okPressed() {
			// Copy data from SWT widgets into fields on button press.
			// Reading data from the widgets later will cause an SWT
			// widget diposed exception.
			user = usernameField.getText();
			password = passwordField.getText();
			super.okPressed();
		}
	}
}
