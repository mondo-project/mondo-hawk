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
 *     Beatriz Sanchez - some UI updates
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hawk.core.IModelUpdater;
import org.hawk.osgiserver.HManager;

public class HPluginsDialog extends TitleAreaDialog {

	private CheckboxTableViewer metamodelPluginTable;
	private CheckboxTableViewer modelPluginTable;
	
	public HPluginsDialog(Shell parentShell) {
		super(parentShell);
		//this.hawkModel = hawkModel;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		HManager instance = HManager.getInstance();

		modelPluginTable = pluginTable(parent, "Updater", instance.getModelUpdaterInstances(), new LabelProvider() {
			@Override
			public String getText(Object element) {
				IModelUpdater updater = (IModelUpdater)element;
				return updater.getHumanReadableName();
			}
		});

		return parent;

	}
	
	private static final class ListContentProvider implements IStructuredContentProvider {
		@Override
		public void dispose() {
			// nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List) {
				return ((List<?>) inputElement).toArray();
			} else {
				return new Object[0];
			}
		}
	}
	
	private CheckboxTableViewer pluginTable(Composite container, String tableLabel, Map<String,?> plugins, LabelProvider labelProvider) {
		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		String formatter = "&%s plugins:";
		label.setText(String.format(formatter, tableLabel));

		final CheckboxTableViewer tableviewer = CheckboxTableViewer.newCheckList(container,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		tableviewer.setContentProvider(new ListContentProvider());
		tableviewer.setLabelProvider(labelProvider);
		tableviewer.setInput(plugins.entrySet());
		tableviewer.setAllChecked(true);
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				//dialogChanged();
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		tableviewer.getTable().setLayoutData(gd);
		tableviewer.getTable().setHeaderVisible(false);

		tableviewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				//dialogChanged();
			}
		});

		Composite cTableButtons = new Composite(container, SWT.NULL);
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		cTableButtons.setLayoutData(gd);
		cTableButtons.setLayout(new FillLayout(SWT.VERTICAL));
		Button btnEnableAll = new Button(cTableButtons, SWT.NULL);
		btnEnableAll.setText("Enable all");
		btnEnableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(true);
				//dialogChanged();
			}
		});
		Button btnDisableAll = new Button(cTableButtons, SWT.NULL);
		btnDisableAll.setText("Disable all");
		btnDisableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(false);
				//dialogChanged();
			}
		});
		return tableviewer;
	}

}
