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
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.osgiserver.HManager;

public class HawkPluginSelectionBlock {

	private Composite control;

	private CheckboxTableViewer metamodelTableViewer;
	private CheckboxTableViewer modelTableViewer;
	private CheckboxTableViewer graphChangeListenerTableViewer;

	public void createControl(Composite parent) {
		control = new Composite(parent, SWT.NONE);
		Font font = parent.getFont();
		control.setFont(font);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 1;
		control.setLayout(layout);

		// METAMODEL PARSER

		Label label = new Label(control, SWT.NULL);
		label.setText("&Metamodel plugins:");

		metamodelTableViewer = newTableViewer();
		metamodelTableViewer.setLabelProvider(new TypedLabelProvider(IMetaModelResourceFactory.class));
		metamodelTableViewer.setContentProvider(new TypedContentProvider("getMetamodelParserInstances"));
		metamodelTableViewer.setInput(HManager.getInstance().getMetamodelParserInstances().values().toArray());
		metamodelTableViewer.setAllChecked(true);

		// MODEL PARSER

		label = new Label(control, SWT.NULL);
		label.setText("&Model plugins:");

		modelTableViewer = newTableViewer();
		modelTableViewer.setLabelProvider(new TypedLabelProvider(IModelResourceFactory.class));
		modelTableViewer.setContentProvider(new TypedContentProvider("getModelParserInstances"));
		modelTableViewer.setInput(HManager.getInstance().getModelParserInstances().values().toArray());
		modelTableViewer.setAllChecked(true);

		// GRAPH CHANGE LISTENERS

		label = new Label(control, SWT.NULL);
		label.setText("&Graph change listeners plugins:");

		graphChangeListenerTableViewer = newTableViewer();
		graphChangeListenerTableViewer.setLabelProvider(new TypedLabelProvider(IGraphChangeListener.class));
		graphChangeListenerTableViewer.setContentProvider(new TypedContentProvider("getGraphChangeListenerInstances"));
		graphChangeListenerTableViewer.setInput(HManager.getInstance().getModelParserInstances().values().toArray());
		graphChangeListenerTableViewer.setAllChecked(false);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		control.setLayoutData(data);
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

	public void update() {
		this.metamodelTableViewer.refresh();
		this.modelTableViewer.refresh();
		this.graphChangeListenerTableViewer.refresh();
	}

	public List<String> getAllChecked() {
		List<String> checked = new ArrayList<>();
		checked.addAll(getCheckedList(metamodelTableViewer, IMetaModelResourceFactory.class));
		checked.addAll(getCheckedList(modelTableViewer, IModelResourceFactory.class));
		checked.addAll(getCheckedList(graphChangeListenerTableViewer, IGraphChangeListener.class));
		return checked;
	}

	class TypedContentProvider implements IStructuredContentProvider {

		private String method;

		public TypedContentProvider(String method) {
			this.method = method;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object[] getElements(Object input) {
			try {
				Map invoke = (Map) HManager.class.getMethod(method).invoke(HManager.getInstance());
				return invoke.values().toArray();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

	}

	class TypedLabelProvider extends LabelProvider implements IToolTipProvider {

		private Class<?> clazz;

		public TypedLabelProvider(Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public String getText(Object element) {
			try {
				return (String) clazz.getMethod("getHumanReadableName").invoke(clazz.cast(element));
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}

		@Override
		public String getToolTipText(Object element) {
			try {
				return (String) clazz.getMethod("getType").invoke(clazz.cast(element));
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}

	}

	private CheckboxTableViewer newTableViewer() {
		CheckboxTableViewer modelTableViewer = CheckboxTableViewer.newCheckList(control,
				SWT.BORDER | SWT.V_SCROLL | SWT.FILL);
		modelTableViewer.setUseHashlookup(true);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		modelTableViewer.getTable().setLayoutData(gd);
		modelTableViewer.getTable().setHeaderVisible(false);
		return modelTableViewer;
	}

	private List<String> getCheckedList(CheckboxTableViewer viewer, Class<?> clazz) {
		return Arrays.asList(viewer.getCheckedElements()).stream().map(e -> {
			try {
				return (String) clazz.getMethod("getType").invoke(clazz.cast(e));
			} catch (Exception ex) {
				return "";
			}
		}).collect(Collectors.toList());
	}

}