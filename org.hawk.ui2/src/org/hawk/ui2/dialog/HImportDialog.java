/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.ui2.dialog;


import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.hawk.core.IHawkFactory;
import org.hawk.core.util.HawkConfig;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.Activator;
import org.hawk.ui2.util.HUIManager;
import org.hawk.ui2.view.HView;

public class HImportDialog extends Dialog {

	private static final class RemoveExistingHawksFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			final IHawkFactory.InstanceInfo entry = (IHawkFactory.InstanceInfo)element;
			final HUIManager manager = HUIManager.getInstance();

			final File base = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
			final File expectedFolder = new File(base, entry.name);

			for (HModel m : manager.getHawks()) {
				if (m.getName().equals(entry.name)) {
					// There's already a Hawk index with that name: do not include it
					return false;
				}
				else if (new File(m.getFolder()).equals(expectedFolder)) {
					// There's already a Hawk index in the folder we'd use for the import: do not include it
					return false;
				}
			}

			return true;
		}
	}

	private static final class RemoveHawksWithUnknownDBTypeFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			final IHawkFactory.InstanceInfo entry = (IHawkFactory.InstanceInfo)element;
			final HUIManager manager = HUIManager.getInstance();
			return entry.dbType == null || manager.getIndexTypes().contains(entry.dbType);
		}
	}

	private static final class ClassNameLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return element.getClass().getName();
		}
	}

	private ComboViewer cmbvInstanceType;
	private Text txtLocation;
	private CheckboxTableViewer tblvInstances;
	private IHawkFactory[] factories;
	private Button btnFetch;

	public HImportDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle());

		final HUIManager manager = HUIManager.getInstance();
		final Collection<IHawkFactory> factories = manager.getHawkFactoryInstances().values();
		this.factories = factories.toArray(new IHawkFactory[0]);
	}

	private IHawkFactory getSelectedFactory() {
		final IStructuredSelection sel = (IStructuredSelection) cmbvInstanceType.getSelection();
		return (IHawkFactory) sel.getFirstElement();
	}

	private String getLocation() {
		return txtLocation.getText();
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		container.setLayout(gridLayout);

		// Instance type row //////////////////////////////////////////////////
	
		final Label lblInstanceType = new Label(container, SWT.NONE);
		lblInstanceType.setText("&Instance type:");

		final Combo cmbInstanceType = new Combo(container, SWT.READ_ONLY);
		final GridData cmbInstanceTypeLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
		cmbInstanceTypeLayout.horizontalSpan = 2;
		cmbInstanceType.setLayoutData(cmbInstanceTypeLayout);

		cmbvInstanceType = new ComboViewer(cmbInstanceType);
		cmbvInstanceType.setContentProvider(new ArrayContentProvider());
		cmbvInstanceType.setLabelProvider(new ClassNameLabelProvider());
		cmbvInstanceType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				dialogChanged();
			}
		});
		cmbvInstanceType.setInput(this.factories);
		if (this.factories.length > 0) {
			cmbInstanceType.select(0);
		}

		// Remote location ////////////////////////////////////////////////////

		final Label lblLocation = new Label(container, SWT.NONE);
		lblLocation.setText("&Remote location:");

		txtLocation = new Text(container, SWT.BORDER);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		// We do *not* add a text change listener, as we do not want to repeatedly
		// send bad requests to remote servers. Users will be expected to hit the
		// "Fetch" button to query the remote server.

		btnFetch = new Button(container, SWT.NONE);
		btnFetch.setText("Fetch");
		btnFetch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});

		// Instance list //////////////////////////////////////////////////////

		final Table tblInstances = new Table(container, SWT.BORDER|SWT.CHECK);
		final GridData tblInstancesLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		tblInstancesLayout.horizontalSpan = 3;
		tblInstancesLayout.minimumHeight = 200;
		tblInstancesLayout.minimumWidth = 400;
		tblInstances.setLayoutData(tblInstancesLayout);

		tblvInstances = new CheckboxTableViewer(tblInstances);
		tblvInstances.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				final IHawkFactory.InstanceInfo entry = (IHawkFactory.InstanceInfo)element;
				return entry.name + (entry.running ? " (running)" : "");
			}
		});
		tblvInstances.setSorter(new ViewerSorter());
		tblvInstances.setFilters(new ViewerFilter[] {
				new RemoveExistingHawksFilter(),
				new RemoveHawksWithUnknownDBTypeFilter()
		});
		tblvInstances.setContentProvider(new ArrayContentProvider());

		// "Select all" and "Deselect all" ////////////////////////////////////

		final Composite batchSelectContainer = new Composite(container, SWT.NONE);
		batchSelectContainer.setLayout(new FillLayout());
		final GridData batchSelectContainerLayout = new GridData();
		batchSelectContainerLayout.horizontalSpan = 3;
		batchSelectContainer.setLayoutData(batchSelectContainerLayout);

		final Button btnSelectAll = new Button(batchSelectContainer, SWT.NONE);
		btnSelectAll.setText("&Select All");
		btnSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tblvInstances.setAllChecked(true);
			}
		});

		final Button btnDeselectAll = new Button(batchSelectContainer, SWT.NONE);
		btnDeselectAll.setText("&Deselect All");
		btnDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tblvInstances.setAllChecked(false);
			}
		});

		dialogChanged();
		return container;
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		final Button btn = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			btn.setText("Import");
		}
		return btn;
	}

	@Override
	protected void okPressed() {
		setReturnCode(OK);
		doImport();
		close();
	}

	private void dialogChanged() {
		final IHawkFactory factory = getSelectedFactory();

		final boolean instancesUseLocation = factory != null && factory.instancesUseLocation();
		txtLocation.setEnabled(instancesUseLocation);
		btnFetch.setEnabled(instancesUseLocation);

		try {
			tblvInstances.setInput(null);
			if (!factory.instancesUseLocation() || !"".equals(getLocation())) {
				tblvInstances.setInput(factory.listInstances(getLocation()));
			}
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	private void doImport() {
		IHawkFactory factory = getSelectedFactory();
		final String location = getLocation();

		final File base = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		for (Object o : tblvInstances.getCheckedElements()) {
			final IHawkFactory.InstanceInfo instance = (IHawkFactory.InstanceInfo)o;
			File storage = new File(base, instance.name);
			try {
				HawkConfig hc = new HawkConfig(instance.name, storage.getCanonicalPath(), location, factory.getClass().getName());

				final HUIManager manager = HUIManager.getInstance();
				final HModel hm = HModel.load(hc, manager);
				manager.addHawk(hm);
				manager.saveHawkToMetadata(hm);

				HView.updateAsync(getShell());
			} catch (Exception e) {
				Activator.logError(e.getMessage(), e);
			}
		}
	}
}
