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
 *     Antonio Garcia-Dominguez - initial API and implementation
 *     Beatriz Sanchez - some UI updates
 ******************************************************************************/
package org.hawk.ui2.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IHawkPlugin.Category;
import org.hawk.osgiserver.HManager;
import org.hawk.ui2.util.HUIManager;

public class HawkPluginSelectionBlock {

	private Composite control;

	private CheckboxTableViewer metamodelTableViewer;
	private CheckboxTableViewer modelTableViewer;
	private CheckboxTableViewer graphChangeListenerTableViewer;

	private List<IHawkPlugin> plugins;
	
	public HawkPluginSelectionBlock() {
		plugins = HUIManager.getInstance().getAvailablePlugins();
	}

	public void createControl(Composite parent) {
		control = new Composite(parent, SWT.NONE);
		Font font = parent.getFont();
		control.setFont(font);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 1;
		control.setLayout(layout);

		GridData labelGd = new GridData(GridData.FILL_BOTH);
		labelGd.horizontalSpan = 3;

		// METAMODEL PARSER
		Label label = new Label(control, SWT.NULL);
		label.setText("&Metamodel plugins:");
		label.setLayoutData(labelGd);
		
		Composite tableComposite = getTableComposite(control);
		metamodelTableViewer = newTableViewer(tableComposite);
		metamodelTableViewer.setLabelProvider(new TypedLabelProvider());
		metamodelTableViewer.setContentProvider(new TypedContentProvider(Category.METAMODEL_RESOURCE_FACTORY));
		metamodelTableViewer.setInput(HManager.getInstance().getMetamodelParserInstances().values().toArray());
		metamodelTableViewer.setAllChecked(true);

		addSelectionButtons(metamodelTableViewer, control);

		// MODEL PARSER

		label = new Label(control, SWT.NULL);
		label.setText("&Model plugins:");
		label.setLayoutData(labelGd);

		tableComposite = getTableComposite(control);
		modelTableViewer = newTableViewer(tableComposite);
		modelTableViewer.setLabelProvider(new TypedLabelProvider());
		modelTableViewer.setContentProvider(new TypedContentProvider(Category.MODEL_RESOURCE_FACTORY));
		modelTableViewer.setInput(HManager.getInstance().getModelParserInstances().values().toArray());
		modelTableViewer.setAllChecked(true);

		addSelectionButtons(modelTableViewer, control);
		
		// GRAPH CHANGE LISTENERS

		
		label = new Label(control, SWT.NULL);
		label.setText("&Graph change listeners plugins:");
		label.setLayoutData(labelGd);

		tableComposite = getTableComposite(control);
		graphChangeListenerTableViewer = newTableViewer(tableComposite);
		graphChangeListenerTableViewer.setLabelProvider(new TypedLabelProvider());
		graphChangeListenerTableViewer.setContentProvider(new TypedContentProvider(Category.GRAPH_CHANGE_LISTENER));
		graphChangeListenerTableViewer.setInput(HManager.getInstance().getModelParserInstances().values().toArray());
		graphChangeListenerTableViewer.setAllChecked(false);

		addSelectionButtons(graphChangeListenerTableViewer, control);
		
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		control.setLayoutData(data);
	}

	private Composite getTableComposite(Composite control) {
		Composite cTable = new Composite(control, SWT.NULL);
		GridData gd = new GridData(SWT.FILL, SWT.TOP , true, true);
		cTable.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		cTable.setLayout(layout);
		return cTable;
	}
	
	private Composite getButtonComposite(Composite control) {
		Composite cTable = new Composite(control, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = true;
		GridData gd = new GridData(SWT.FILL, SWT.FILL_WINDING, true, true);
		cTable.setLayoutData(gd);
		//cTable.setLayout(new FillLayout(SWT.VERTICAL));
		cTable.setLayout(layout);
		return cTable;
	}

	private void addSelectionButtons(CheckboxTableViewer tableViewer, Composite composite) {
		Composite cTableButtons = getButtonComposite(composite);
		
		Button btnEnableAll = new Button(cTableButtons, SWT.NULL);
		btnEnableAll.setText("Enable all");
		btnEnableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(true);
			}
		});
		Button btnDisableAll = new Button(cTableButtons, SWT.NULL);
		btnDisableAll.setText("Disable all");
		btnDisableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(false);
			}
		});
	}
	
	public CheckboxTableViewer getMetamodelTableViewer() {
		return metamodelTableViewer;
	}

	public CheckboxTableViewer getModelTableViewer() {
		return modelTableViewer;
	}

	public CheckboxTableViewer getGraphChangeListenerTableViewer() {
		return graphChangeListenerTableViewer;
	}

	public Composite getControl() {
		return control;
	}

	public void update(List<IHawkPlugin> plugins) {
		this.plugins = (plugins!= null) ? plugins : Collections.emptyList();
		this.metamodelTableViewer.refresh();
		this.modelTableViewer.refresh();
		this.graphChangeListenerTableViewer.refresh();
	}

	public List<String> getAllChecked() {
		List<String> checked = new ArrayList<>();
		checked.addAll(getCheckedList(metamodelTableViewer));
		checked.addAll(getCheckedList(modelTableViewer));
		checked.addAll(getCheckedList(graphChangeListenerTableViewer));
		return checked;
	}

	class TypedContentProvider implements IStructuredContentProvider {

		private Category category;

		public TypedContentProvider(Category category) {
			this.category = category;
		}

		@Override
		public Object[] getElements(Object input) {
			return plugins.parallelStream().filter(p -> p.getCategory().equals(category))
					.collect(Collectors.toList()).toArray();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		@Override
		public void dispose() {}

	}

	class TypedLabelProvider extends LabelProvider implements IToolTipProvider {

		@Override
		public String getText(Object element) {
			return ((IHawkPlugin) element).getHumanReadableName();
		}

		@Override
		public String getToolTipText(Object element) {
			return ((IHawkPlugin) element).getType();
		}

	}

	private CheckboxTableViewer newTableViewer(Composite composite) {
		CheckboxTableViewer modelTableViewer = CheckboxTableViewer.newCheckList(composite,
				SWT.BORDER | SWT.V_SCROLL | SWT.FILL);
		modelTableViewer.setUseHashlookup(true);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		modelTableViewer.getTable().setLayoutData(gd);
		modelTableViewer.getTable().setHeaderVisible(false);
		return modelTableViewer;
	}

	private List<String> getCheckedList(CheckboxTableViewer viewer) {
		return Arrays.asList(viewer.getCheckedElements()).stream().map(e -> ((IHawkPlugin) e).getType())
				.collect(Collectors.toList());
	}

}