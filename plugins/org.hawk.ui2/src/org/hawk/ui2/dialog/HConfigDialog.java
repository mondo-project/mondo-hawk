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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.IStateListener;
import org.hawk.core.IVcsManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.view.HView;

public class HConfigDialog extends TitleAreaDialog implements IStateListener {

	private static final String DEFAULT_MESSAGE = "Manage metamodels, locations and indexed/derived attributes in the indexer.";

	static final class ClassNameLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return element.getClass().getName();
		}
	}

	HModel hawkModel;
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
	private Button removeVCSButton;
	HawkState hawkState;

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

		Dialog d = new HDerivedAttributeDialog(hawkModel, getShell());

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
		derivedAttributeList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				boolean running = hawkState == HawkState.RUNNING;
				removeDerivedAttributeButton.setEnabled(running
						&& derivedAttributeList.getSelection().length > 0);

			}
		});

		updateDerivedAttributeList();

		removeDerivedAttributeButton = new Button(composite, SWT.PUSH);
		removeDerivedAttributeButton.setText("Remove");
		removeDerivedAttributeButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {

						// remove action
						String[] selected = null;
						if (derivedAttributeList.getSelection().length > 0)
							selected = derivedAttributeList.getSelection();

						if (selected != null) {

							MessageBox messageBox = new MessageBox(getShell(),
									SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							messageBox
									.setMessage("Are you sure you wish to delete the chosen derived attributes?");
							messageBox.setText("Indexed Attribute deletion");
							int response = messageBox.open();
							if (response == SWT.YES) {

								hawkModel.removeDerviedAttributes(selected);

								updateDerivedAttributeList();
							}
						}

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

		Dialog d = new HIndexedAttributeDialog(hawkModel, getShell());

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
		indexedAttributeList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				boolean running = hawkState == HawkState.RUNNING;
				removeIndexedAttributeButton.setEnabled(running
						&& indexedAttributeList.getSelection().length > 0);

			}
		});

		updateIndexedAttributeList();

		removeIndexedAttributeButton = new Button(composite, SWT.PUSH);
		removeIndexedAttributeButton.setText("Remove");
		removeIndexedAttributeButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {

						// remove action
						String[] selected = null;
						if (indexedAttributeList.getSelection().length > 0)
							selected = indexedAttributeList.getSelection();

						if (selected != null) {

							MessageBox messageBox = new MessageBox(getShell(),
									SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							messageBox
									.setMessage("Are you sure you wish to delete the chosen indexed attributes?");
							messageBox.setText("Indexed Attribute deletion");
							int response = messageBox.open();
							if (response == SWT.YES) {

								hawkModel.removeIndexedAttributes(selected);

								updateIndexedAttributeList();
							}
						}

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
		final FileDialog fd = new FileDialog(getShell(), SWT.MULTI);

		final IPath workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		fd.setFilterPath(workspaceRoot.toFile().toString());
		fd.setFilterExtensions(getKnownMetamodelFilePatterns());
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

	protected String[] getKnownMetamodelFilePatterns() {
		final Set<String> extensions = hawkModel.getIndexer().getKnownMetamodelFileExtensions();
		final Set<String> patterns = new HashSet<>(extensions.size() + 1);
		for (String ext : extensions) {
			// NOTE: metamodel parsers always report metamodel file extensions with starting "."
			patterns.add("*" + ext);
		}
		patterns.add("*.*");
		final String[] aPatterns = patterns.toArray(new String[patterns.size()]);
		return aPatterns;
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
		removeMetaModelsButton.setText("Remove");

		removeMetaModelsButton.setEnabled(hawkState == HawkState.RUNNING);

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
		for (String da : hawkModel.getDerivedAttributeNames()) {
			derivedAttributeList.add(da);
		}
		String[] items = derivedAttributeList.getItems();
		java.util.Arrays.sort(items);
		derivedAttributeList.setItems(items);
	}

	private void updateIndexedAttributeList() {
		indexedAttributeList.removeAll();
		for (String ia : hawkModel.getIndexedAttributeNames()) {
			indexedAttributeList.add(ia);
		}
		String[] items = indexedAttributeList.getItems();
		java.util.Arrays.sort(items);
		indexedAttributeList.setItems(items);
	}

	private void updateMetamodelList() {
		metamodelList.removeAll();
		final java.util.List<String> mms = hawkModel.getRegisteredMetamodels();
		Collections.sort(mms);
		for (String mm : mms) {
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

		removeVCSButton = new Button(composite, SWT.NONE);
		removeVCSButton.setText("Remove");
		removeVCSButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// remove action
				IVcsManager m = getSelectedExistingVCSManager();

				if (m != null) {

					MessageBox messageBox = new MessageBox(getShell(),
							SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					messageBox
							.setMessage("Are you sure you wish to remove the chosen VCS location? This will also delete all indexed models from it, and may take a long time to complete.");
					messageBox.setText("VCS location deletion");
					int response = messageBox.open();
					if (response == SWT.YES) {
						try {
							hawkModel.removeRepository(m);
						} catch (Exception ee) {
							ee.printStackTrace();
						}
						lstVCSLocations.setInput(hawkModel
								.getRunningVCSManagers().toArray());
					}
				}
			}
		});
		removeVCSButton.setEnabled(false);

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

		lstVCSLocations.getList().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				boolean running = hawkState == HawkState.RUNNING;
				editVCSButton.setEnabled(running
						&& getSelectedExistingVCSManager() != null);
				removeVCSButton.setEnabled(running
						&& getSelectedExistingVCSManager() != null);

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
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

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

		setTitle("Configuration for indexer " + hawkModel.getName());
		setMessage(DEFAULT_MESSAGE);
		return tabFolder;
	}

	@Override
	public boolean close() {
		hawkModel.getHawk().getModelIndexer().removeStateListener(this);
		return super.close();
	}

	@Override
	public void state(HawkState state) {
		updateAsync(state);
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

	public void updateAsync(final HawkState s) {
		if (this.hawkState == s) {
			return;
		}
		this.hawkState = s;

		Shell shell = getShell();
		if (shell == null) {
			return;
		}

		Display display = shell.getDisplay();
		if (display == null) {
			return;
		}

		display.asyncExec(new Runnable() {
			public void run() {
				try {
					// primary display buttons
					final boolean running = s == HawkState.RUNNING;
					indexRefreshButton.setEnabled(running);
					removeDerivedAttributeButton.setEnabled(running);
					addDerivedAttributeButton.setEnabled(running);
					removeIndexedAttributeButton.setEnabled(running);
					addIndexedAttributeButton.setEnabled(running);
					removeMetaModelsButton.setEnabled(running);
					addMetaModelsButton.setEnabled(running);
					addVCSButton.setEnabled(running);
					editVCSButton.setEnabled(running
							&& getSelectedExistingVCSManager() != null);

					if (running) {
						setErrorMessage(null);
					} else {
						setErrorMessage(String
								.format("The index is %s - some buttons will be disabled",
										s.toString().toLowerCase()));
					}
				} catch (Exception e) {
					// suppress
				}
			}
		});
	}
}
