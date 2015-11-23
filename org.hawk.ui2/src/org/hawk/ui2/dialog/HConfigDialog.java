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
 *     Antonio Garcia-Dominguez - updates and maintenance, edit credentials
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.IStateListener;
import org.hawk.core.IVcsManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.view.HView;

public class HConfigDialog extends Dialog implements IStateListener {

	static final class ClassNameLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return element.getClass().getName();
		}
	}

	private HModel hawkModel;
	private List metamodelList;
	private List derivedAttributeList;
	private List indexedAttributeList;
	private List indexList;

	private ListViewer lstVCSLocations;

	private Button indexRefreshButton;
	private Button removeDerivedAttributeButton;
	private Button addDerivedAttributeButton;
	private Button removeIndexedAttributeButton;
	private Button addIndexedAttributeButton;
	private Button removeMetaModelsButton;
	private Button addMetaModelsButton;
	private Button addVCSButton;
	private Button editVCSButton;
	private HawkState s;
	
	public HConfigDialog(Shell parentShell, HModel in) {
		super(parentShell);
		setShellStyle(getShellStyle() & ~SWT.CLOSE);

		hawkModel = in;
		hawkModel.getHawk().getModelIndexer().addStateListener(this);
	}

	private Composite allIndexesTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);

		indexList = new List(composite, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		indexList.setLayoutData(gridDataQ);

		updateAllIndexesList();

		indexRefreshButton = new Button(composite, SWT.NONE);
		indexRefreshButton.setText("Refresh");
		indexRefreshButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateAllIndexesList();
			}
		});

		return composite;

	}

	private void derivedAttributeAdd() {

		Dialog d = new Dialog(this) {

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

				parent.getShell().setText("Add a derived attribute");

				Composite composite = (Composite) super
						.createDialogArea(parent);

				GridLayout la = new GridLayout();
				la.numColumns = 2;
				composite.setLayout(la);

				Label l = new Label(composite, SWT.NONE);
				l.setText(" Metamodel URI: ");

				final Combo c = new Combo(composite, SWT.READ_ONLY);
				for (String s : hawkModel.getRegisteredMetamodels())
					c.add(s);

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
						Button ok = getButton(IDialogConstants.OK_ID);
						if (check())
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
						else
							ok.setEnabled(false);
						t5.setText(error);
					}
				});

				return composite;
			}

		};

		d.setBlockOnOpen(true);
		if (d.open() == Window.OK)
			updateDerivedAttributeList();

	}

	private Composite derivedAttributeTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		derivedAttributeList = new List(composite, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		derivedAttributeList.setLayoutData(gridDataQ);

		updateDerivedAttributeList();

		removeDerivedAttributeButton = new Button(composite, SWT.PUSH);
		removeDerivedAttributeButton.setText("Remove");
		removeDerivedAttributeButton.setEnabled(false);
		removeDerivedAttributeButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						// remove action
					}
				});

		addDerivedAttributeButton = new Button(composite, SWT.NONE);
		addDerivedAttributeButton.setText("Add...");
		addDerivedAttributeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				derivedAttributeAdd();
			}
		});

		return composite;
	}

	private void indexedAttributeAdd() {

		Dialog d = new Dialog(this) {

			String uri = "";
			String type = "Modifier";
			String name = "static";

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
				for (String s : hawkModel.getRegisteredMetamodels())
					c.add(s);
				c.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						uri = c.getText();
						Button ok = getButton(IDialogConstants.OK_ID);
						if (!type.equals("") && !uri.equals("")
								&& !name.equals(""))
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
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
							ok.setEnabled(enableButton(ok));
						else
							ok.setEnabled(false);
					}
				});

				l = new Label(parent, SWT.NONE);
				l.setText("");

				return parent;
			}

		};

		d.setBlockOnOpen(true);
		if (d.open() == Window.OK)
			updateIndexedAttributeList();

	}

	private Composite indexedAttributeTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		indexedAttributeList = new List(composite, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		indexedAttributeList.setLayoutData(gridDataQ);

		updateIndexedAttributeList();

		removeIndexedAttributeButton = new Button(composite, SWT.PUSH);
		removeIndexedAttributeButton.setText("Remove");
		removeIndexedAttributeButton.setEnabled(false);
		removeIndexedAttributeButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						// remove action
					}
				});

		addIndexedAttributeButton = new Button(composite, SWT.NONE);
		addIndexedAttributeButton.setText("Add...");
		addIndexedAttributeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				indexedAttributeAdd();
			}
		});

		return composite;
	}

	private void metamodelBrowse() {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI);

		fd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile().toString());
		// TODO: allow selection of only parse-able/known metamodels-file-types
		fd.setFilterExtensions(new String[] { "*.ecore", "*.*" });
		fd.setText("Select metamodels");
		String result = fd.open();

		if (result != null) {

			String[] metaModels = fd.getFileNames();
			File[] metaModelFiles = new File[metaModels.length];

			boolean error = false;

			for (int i = 0; i < metaModels.length; i++) {
				File file = new File(fd.getFilterPath() + File.separator
						+ metaModels[i]);
				if (!file.exists() || !file.canRead() || !file.isFile())
					error = true;
				else
					metaModelFiles[i] = file;

			}

			if (!error) {
				hawkModel.registerMeta(metaModelFiles);
				updateMetamodelList();
			}
		}

	}

	private Composite metamodelsTab(TabFolder parent) {

		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		metamodelList = new List(composite, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 2;

		metamodelList.setLayoutData(gridDataQ);

		updateMetamodelList();

		removeMetaModelsButton = new Button(composite, SWT.PUSH);
		removeMetaModelsButton.setText(s==HawkState.RUNNING?"Remove":"Remove (DISABLED -- INDEX "+(s==HawkState.STOPPED?"STOPPED)":"UPDATING)"));
		removeMetaModelsButton.setEnabled(s==HawkState.RUNNING);
		removeMetaModelsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// remove action
				String[] selectedMetamodel = null;
				if (metamodelList.getSelection().length > 0)
					selectedMetamodel = metamodelList.getSelection();

				if (selectedMetamodel != null) {

					MessageBox messageBox = new MessageBox(getShell(),
							SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					messageBox
							.setMessage("Are you sure you wish to delete the chosen metamodel(s)? This will also delete any dependant metamodels/models and may take a long time to complete.");
					messageBox.setText("Metamodel deletion");
					int response = messageBox.open();
					if (response == SWT.YES) {

						hawkModel.removeMetamodels(selectedMetamodel);

						updateMetamodelList();
					}
				}
			}
		});

		addMetaModelsButton = new Button(composite, SWT.PUSH);
		addMetaModelsButton.setText("Add...");
		addMetaModelsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				metamodelBrowse();
			}
		});

		return composite;
	}

	private void updateAllIndexesList() {
		indexList.removeAll();
		for (String i : hawkModel.getIndexes()) {
			indexList.add(i);
		}
		String[] items = indexList.getItems();
		java.util.Arrays.sort(items);
		indexList.setItems(items);
	}

	private void updateDerivedAttributeList() {
		derivedAttributeList.removeAll();
		for (String da : hawkModel.getDerivedAttributes()) {
			derivedAttributeList.add(da);
		}
		String[] items = derivedAttributeList.getItems();
		java.util.Arrays.sort(items);
		derivedAttributeList.setItems(items);
	}

	private void updateIndexedAttributeList() {
		indexedAttributeList.removeAll();
		for (String ia : hawkModel.getIndexedAttributes()) {
			indexedAttributeList.add(ia);
		}
		String[] items = indexedAttributeList.getItems();
		java.util.Arrays.sort(items);
		indexedAttributeList.setItems(items);
	}

	private void updateMetamodelList() {
		metamodelList.removeAll();
		for (String mm : hawkModel.getRegisteredMetamodels()) {
			metamodelList.add(mm);
		}
	}

	private Composite vcsTab(TabFolder parent) {
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		lstVCSLocations = new ListViewer(composite, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);
		lstVCSLocations.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IVcsManager) element).getLocation();
			}
		});
		lstVCSLocations.setContentProvider(new ArrayContentProvider());
		lstVCSLocations.setInput(hawkModel.getRunningVCSManagers().toArray());
		final GridData lstVCSLocationsLayoutData = new GridData(SWT.FILL,
				SWT.FILL, true, true);
		lstVCSLocationsLayoutData.horizontalSpan = 3;
		lstVCSLocations.getList().setLayoutData(lstVCSLocationsLayoutData);

		addVCSButton = new Button(composite, SWT.NONE);
		addVCSButton.setText("Add...");
		addVCSButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new HVCSDialog(getShell(), hawkModel, null).open();
				lstVCSLocations.setInput(hawkModel.getRunningVCSManagers()
						.toArray());
			}
		});

		editVCSButton = new Button(composite, SWT.NONE);
		editVCSButton.setText("Edit...");
		editVCSButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new HVCSDialog(getShell(), hawkModel,
						getSelectedExistingVCSManager()).open();
				lstVCSLocations.refresh();
			}
		});
		editVCSButton.setEnabled(false);

		lstVCSLocations.getList().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
								
				boolean running = s==HawkState.RUNNING;
				editVCSButton.setText(running?"Edit...":"Edit... (DISABLED -- INDEX "+(s==HawkState.STOPPED?"STOPPED)":"UPDATING)"));
				editVCSButton
						.setEnabled(running && getSelectedExistingVCSManager() != null);
				
			}
		});

		return composite;
	}

	protected IVcsManager getSelectedExistingVCSManager() {
		final IStructuredSelection sel = (IStructuredSelection) lstVCSLocations
				.getSelection();
		if (sel.isEmpty()) {
			return null;
		}
		return (IVcsManager) sel.getFirstElement();
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configure Indexer: " + hawkModel.getName());
	}

	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		if (id == IDialogConstants.CANCEL_ID) {
			return null;
		}
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button done = getButton(IDialogConstants.OK_ID);
		done.setText("Done");
		setButtonLayoutData(done);
	}

	protected Control createDialogArea(Composite parent) {
		try {
			setDefaultImage(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(HView.ID).getTitleImage());

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		TabFolder tabFolder = new TabFolder(parent, SWT.BORDER);

		TabItem metamodelTab = new TabItem(tabFolder, SWT.NULL);
		metamodelTab.setText("Metamodels");
		metamodelTab.setControl(metamodelsTab(tabFolder));

		TabItem vcsTab = new TabItem(tabFolder, SWT.NULL);
		vcsTab.setText("Indexed Locations");
		vcsTab.setControl(vcsTab(tabFolder));

		TabItem derivedAttributeTab = new TabItem(tabFolder, SWT.NULL);
		derivedAttributeTab.setText("Derived Attributes");
		derivedAttributeTab.setControl(derivedAttributeTab(tabFolder));

		TabItem indexedAttributeTab = new TabItem(tabFolder, SWT.NULL);
		indexedAttributeTab.setText("Indexed Attributes");
		indexedAttributeTab.setControl(indexedAttributeTab(tabFolder));

		TabItem allIndexesTab = new TabItem(tabFolder, SWT.NULL);
		allIndexesTab.setText("All indexes");
		allIndexesTab.setControl(allIndexesTab(tabFolder));

		tabFolder.pack();
		return tabFolder;
	}

	@Override
	public boolean close() {
		hawkModel.getHawk().getModelIndexer().removeStateListener(this);
		return super.close();
	}

	@Override
	public void state(HawkState state) {
		switch (state) {
		case STOPPED:
			updateAsync(HawkState.STOPPED);
			break;
		case RUNNING:
			updateAsync(HawkState.RUNNING);
			break;
		case UPDATING:
			updateAsync(HawkState.UPDATING);
			break;
		}
	}

	@Override
	public void info(String s) {
		// not used in config dialogs
	}

	@Override
	public void error(String s) {
		// not used in config dialogs
	}

	@Override
	public void removed() {
		// used for remote message cases
	}

	public void updateAsync(HawkState s) {
	if(this.s!=s){
		this.s = s;
		Shell shell = getShell();
		if (shell != null) {
			Display display = shell.getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							//primary display buttons
							boolean running = s == HawkState.RUNNING;
							boolean stopped = s == HawkState.STOPPED;
							indexRefreshButton.setEnabled(running);
							indexRefreshButton.setText(running?"Refresh":"Refresh (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							//removeDerivedAttributeButton.setEnabled(running);
							removeDerivedAttributeButton.setText(running?"Remove":"Remove (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							addDerivedAttributeButton.setEnabled(running);
							addDerivedAttributeButton.setText(running?"Add...":"Add... (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							//removeIndexedAttributeButton.setEnabled(running);
							removeIndexedAttributeButton.setText(running?"Remove":"Remove (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							addIndexedAttributeButton.setEnabled(running);
							addIndexedAttributeButton.setText(running?"Add...":"Add... (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							removeMetaModelsButton.setEnabled(running);
							removeMetaModelsButton.setText(running?"Remove":"Remove (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							addMetaModelsButton.setEnabled(running);
							addMetaModelsButton.setText(running?"Add...":"Add... (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							addVCSButton.setEnabled(running);
							addVCSButton.setText(running?"Add...":"Add... (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
							editVCSButton.setEnabled(running && getSelectedExistingVCSManager() != null);							
							editVCSButton.setText(running?"Edit...":"Edit... (DISABLED -- INDEX "+(stopped?"STOPPED)":"UPDATING)"));
						} catch (Exception e) {
							// suppress
						}
						try{
							//optional display buttons
							enableButton(getButton(IDialogConstants.OK_ID));
						}catch (Exception e) {
							// suppress
						}
					}
				});
			}
			
		}
	}
	}

	private boolean enableButton(Button ok) {

		boolean initialEnableState = ok.isEnabled();
		boolean ret = s == HawkState.RUNNING;
		boolean stopped = s == HawkState.STOPPED;
		
		if(initialEnableState){
		
		Shell shell = getShell();
		if (shell != null) {
			Display display = shell.getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							
							ok.setEnabled(ret);
							String okText = "OK";
							String updatingText = "OK (Disabled -- INDEX UPDATING)";
							String stoppedText = "OK (Disabled -- INDEX STOPPED)";
							ok.setText(ret?okText:(stopped?stoppedText:updatingText));
							
						} catch (Exception e) {
							// suppress
						}
					}
				});
			}
			
		}
	
		}
		
		return ret;
				
	}
	
}
	
