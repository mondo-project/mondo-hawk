/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 *     Antonio Garcia-Dominguez - updates and maintenance, edit credentials
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.hawk.core.IStateListener;
import org.hawk.core.IVcsManager;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.view.HView;
import org.osgi.framework.FrameworkUtil;

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
	private Button addMetaModelsFromFilesystemButton;
	private Button addMetaModelsFromWorkspaceButton;
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
						final String[] selected = derivedAttributeList.getSelection();
						if (selected.length > 0) {
							MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							messageBox.setMessage("Are you sure you wish to delete the chosen derived attributes?");
							messageBox.setText("Indexed Attribute deletion");

							if (messageBox.open() == SWT.YES) {
								Job removeDerivedJob = new Job("Removing derived attribute from " + hawkModel.getName()) {
									@Override
									protected IStatus run(IProgressMonitor monitor) {
										hawkModel.removeDerivedAttributes(selected);
										Display.getDefault().asyncExec(HConfigDialog.this::updateDerivedAttributeList);
										return new Status(IStatus.OK, getBundleName(), "Done");
									}
								};
								removeDerivedJob.setRule(new HModelSchedulingRule(hawkModel));
								removeDerivedJob.schedule();
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
						final String[] selected = indexedAttributeList.getSelection();
						if (selected.length > 0) {
							MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							messageBox.setMessage("Are you sure you wish to delete the chosen indexed attributes?");
							messageBox.setText("Indexed Attribute deletion");
							if (messageBox.open() == SWT.YES) {
								Job removeIndexedJob = new Job("Removing indexed attribute from " + hawkModel.getName()) {
									@Override
									protected IStatus run(IProgressMonitor monitor) {
										hawkModel.removeIndexedAttributes(selected);
										Display.getDefault().asyncExec(HConfigDialog.this::updateIndexedAttributeList);
										return new Status(IStatus.OK, getBundleName(), "Done");
									}
								};
								removeIndexedJob.setRule(new HModelSchedulingRule(hawkModel));
								removeIndexedJob.schedule();
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

	private void metamodelBrowseFilesystem() {
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
				File file = new File(fd.getFilterPath(), metaModels[i]);
				if (!file.exists() || !file.canRead() || !file.isFile()) {
					error = true;
				}
				else {
					metaModelFiles[i] = file;
				}
			}

			if (!error) {
				hawkModel.registerMeta(metaModelFiles);
				updateMetamodelList();
			}
		}
	}

	private void metamodelBrowseWorkspace() {
		FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(getShell(), true, ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);

		String[] patterns = getKnownMetamodelFilePatterns();
		if (patterns.length > 0) {
			dialog.setInitialPattern(patterns[0], FilteredResourcesSelectionDialog.FULL_SELECTION);
		}
		dialog.setTitle("Add metamodel from workspace");
		dialog.setMessage("Select a metamodel file from the workspace");
		dialog.open();
		
		if (dialog.getReturnCode() == Window.OK){
			Object[] iFiles = dialog.getResult();
			File[] files = new File[iFiles.length];

			for (int i = 0; i < iFiles.length; i++) {
				files[i] = ((IFile)iFiles[i]).getLocation().toFile();
			}

			hawkModel.registerMeta(files);
			updateMetamodelList();
		}
	}
	
	protected String[] getKnownMetamodelFilePatterns() {
		final Set<String> extensions = hawkModel.getIndexer().getKnownMetamodelFileExtensions();

		final java.util.List<String> patterns = new ArrayList<>(extensions.size());
		for (String ext : extensions) {
			// NOTE: metamodel parsers always report metamodel file extensions with starting "."
			patterns.add("*" + ext);
		}
		Collections.sort(patterns);
		patterns.add("*.*");

		return patterns.toArray(new String[patterns.size()]);
	}

	private Composite metamodelsTab(TabFolder parent) {

		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);

		metamodelList = new List(composite, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL;
		gridDataQ.heightHint = 300;
		gridDataQ.widthHint = 600;
		gridDataQ.horizontalSpan = 3;

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

		addMetaModelsFromFilesystemButton = new Button(composite, SWT.PUSH);
		addMetaModelsFromFilesystemButton.setText("Add from &filesystem...");
		addMetaModelsFromFilesystemButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				metamodelBrowseFilesystem();
			}
		});

		addMetaModelsFromWorkspaceButton = new Button(composite, SWT.PUSH);
		addMetaModelsFromWorkspaceButton.setText("Add from &workspace...");
		addMetaModelsFromWorkspaceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				metamodelBrowseWorkspace();
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
		for (IndexedAttributeParameters da : hawkModel.getDerivedAttributes()) {
			derivedAttributeList.add(
				String.format("%s##%s##%s", da.getMetamodelUri(), da.getTypeName(), da.getAttributeName())
			);
		}
		String[] items = derivedAttributeList.getItems();
		java.util.Arrays.sort(items);
		derivedAttributeList.setItems(items);
	}

	private void updateIndexedAttributeList() {
		indexedAttributeList.removeAll();
		for (IndexedAttributeParameters ia : hawkModel.getIndexedAttributes()) {
			indexedAttributeList.add(
					String.format("%s##%s##%s", ia.getMetamodelUri(), ia.getTypeName(), ia.getAttributeName())
			);
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
					MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					messageBox.setMessage("Are you sure you wish to remove the chosen VCS location? This "
							+ "will also delete all indexed models from it, and may take a long time to "
							+ "complete in the background.");
					messageBox.setText("VCS location deletion");
					if (messageBox.open() == SWT.YES) {
						Job removeVcsJob = new Job("Removing VCS from " + hawkModel.getName()) {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									hawkModel.removeRepository(m);
								} catch (Exception ee) {
									return new Status(IStatus.ERROR, getBundleName(), "Failed to remove VCS", ee);
								}
								Display.getDefault().asyncExec(() -> {
									lstVCSLocations.setInput(hawkModel.getRunningVCSManagers().toArray());
								});
								return new Status(IStatus.OK, getBundleName(), "Removed VCS");
							}
						};
						removeVcsJob.setRule(new HModelSchedulingRule(hawkModel));
						removeVcsJob.schedule();
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
					addMetaModelsFromFilesystemButton.setEnabled(running);
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

	private String getBundleName() {
		return FrameworkUtil.getBundle(getClass()).getSymbolicName();
	}

	
}
