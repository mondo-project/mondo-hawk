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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IHawkPlugin.Category;
import org.hawk.ui2.util.HUIManager;

public class HawkPluginSelectionBlock {

	private Composite control;

	private CheckboxTableViewer metamodelTableViewer;
	private CheckboxTableViewer modelTableViewer;
	private CheckboxTableViewer graphChangeListenerTableViewer;

	/*
	 * Turns out that we do need to enable/disable query engines: these
	 * are required to compute derived properties!
	 */
	private CheckboxTableViewer queryEngineTableViewer;

	private List<IHawkPlugin> plugins;
	
	public HawkPluginSelectionBlock() {
		plugins = HUIManager.getInstance().getAvailablePlugins();
	}

	public void createControl(Composite parent) {
		control = new Composite(parent, SWT.NONE);
		Font font = parent.getFont();
		control.setFont(font);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 1;
		control.setLayout(layout);

		GridData data = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(data);

		final boolean allChecked = true;
		metamodelTableViewer = createPluginTableBlock("&Metamodel parsers:", Category.METAMODEL_RESOURCE_FACTORY, allChecked);
		modelTableViewer = createPluginTableBlock("&Model parsers:", Category.MODEL_RESOURCE_FACTORY, allChecked);
		graphChangeListenerTableViewer = createPluginTableBlock("&Graph change listeners:", Category.GRAPH_CHANGE_LISTENER, !allChecked);
		queryEngineTableViewer = createPluginTableBlock("&Query engines:", Category.QUERY_ENGINE, allChecked);
	}

	private CheckboxTableViewer createPluginTableBlock(final String labelText, final Category category, boolean allChecked) {
		final GridData labelGd = new GridData(GridData.FILL_BOTH);
		labelGd.horizontalSpan = 2;

		final Label label = new Label(control, SWT.NULL);
		label.setText(labelText);
		label.setLayoutData(labelGd);

		final Composite tableComposite = getTableComposite(control);
		final CheckboxTableViewer tableViewer = newTableViewer(tableComposite);
		tableViewer.setLabelProvider(new TypedLabelProvider());
		tableViewer.setContentProvider(new TypedContentProvider(category));
		tableViewer.setInput(plugins);
		tableViewer.setAllChecked(allChecked);

		addSelectionButtons(tableViewer, control);
		return tableViewer;
	}

	private Composite getTableComposite(Composite control) {
		Composite cTable = new Composite(control, SWT.NULL);
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		cTable.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		cTable.setLayout(layout);

		return cTable;
	}
	
	private Composite getButtonComposite(Composite control) {
		Composite cTable = new Composite(control, SWT.NULL);
		Layout layout = new FillLayout(SWT.VERTICAL);
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

	public CheckboxTableViewer getQueryEngineTableViewer() {
		return queryEngineTableViewer;
	}

	public Composite getControl() {
		return control;
	}

	public void update(List<IHawkPlugin> plugins) {
		this.plugins.clear();
		this.plugins.addAll(plugins);

		this.metamodelTableViewer.refresh();
		this.modelTableViewer.refresh();
		this.graphChangeListenerTableViewer.refresh();
		this.queryEngineTableViewer.refresh();

		this.metamodelTableViewer.setAllChecked(true);
		this.modelTableViewer.setAllChecked(true);
		this.queryEngineTableViewer.setAllChecked(true);
	}

	public List<String> getAllChecked() {
		List<String> checked = new ArrayList<>();
		checked.addAll(getCheckedList(metamodelTableViewer));
		checked.addAll(getCheckedList(modelTableViewer));
		checked.addAll(getCheckedList(graphChangeListenerTableViewer));
		checked.addAll(getCheckedList(queryEngineTableViewer));
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
		CheckboxTableViewer modelTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.FILL);
		modelTableViewer.setUseHashlookup(true);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 80;
		modelTableViewer.getTable().setLayoutData(gd);
		modelTableViewer.getTable().setHeaderVisible(false);
		return modelTableViewer;
	}

	private List<String> getCheckedList(CheckboxTableViewer viewer) {
		return Arrays.asList(viewer.getCheckedElements()).stream().map(e -> ((IHawkPlugin) e).getType())
				.collect(Collectors.toList());
	}

}