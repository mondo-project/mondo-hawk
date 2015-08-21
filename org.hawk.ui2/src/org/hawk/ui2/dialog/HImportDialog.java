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


import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
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
import org.hawk.ui2.util.HUIManager;

public class HImportDialog extends Dialog {

	private static final class MapContentProvider implements
			IStructuredContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((Map<?, ?>)inputElement).entrySet().toArray();
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

	public IHawkFactory getSelectedFactory() {
		final IStructuredSelection sel = (IStructuredSelection) cmbvInstanceType.getSelection();
		return (IHawkFactory) sel.getFirstElement();
	}

	public String getLocation() {
		return txtLocation.getText();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getSelectedNames() {
		final Set<String> names = new HashSet<>();
		for (Object o : tblvInstances.getCheckedElements()) {
			final Map.Entry<String, Boolean> entry = (Map.Entry<String, Boolean>)o;
			names.add(entry.getKey());
		}
		return names;
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
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				final Map.Entry<String, Boolean> entry = (Map.Entry<String, Boolean>)element;
				return entry.getKey() + (entry.getValue() ? " (running)" : "");
			}
		});
		tblvInstances.setContentProvider(new MapContentProvider());
		// TODO provide some filter for instances we already have

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

	private void dialogChanged() {
		final IHawkFactory factory = getSelectedFactory();

		final boolean instancesUseLocation = factory != null && factory.instancesUseLocation();
		txtLocation.setEnabled(instancesUseLocation);
		btnFetch.setEnabled(instancesUseLocation);

		tblvInstances.setInput(factory.listInstances(txtLocation.getText()));
	}
}
