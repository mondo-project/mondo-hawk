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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.ui.emc.dt2;

import org.eclipse.epsilon.common.dt.launching.dialogs.AbstractModelConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hawk.core.query.IQueryEngine;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class HawkModelConfigurationDialog extends
		AbstractModelConfigurationDialog {

	protected void createPerformanceGroup(Composite parent) {
	}

	protected String getModelName() {
		return "Hawk Index";
	}

	protected String getModelType() {
		return "Hawk";
	}

	protected Label uriTextLabel;
	protected Combo selectIndexer;
	protected Text t1;
	protected Text t;
	protected Text t2;
	protected Button enableFullTraversalScoping;

	protected void createGroups(Composite control) {
		super.createGroups(control);
		createFilesGroup(control);
		// createLoadStoreOptionsGroup(control);
		// super.nameText.setEditable(false);;
		// super.aliasesText.setEditable(false);
	}

	private Text getNameText() {
		return super.nameText;
	}

	private Text getAliasesText() {
		return super.aliasesText;
	}

	protected Composite createFilesGroup(Composite parent) {

		final Composite groupContent = createGroupContainer(parent, "Hawk ", 2);

		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel.setText("Indexer: ");

		selectIndexer = new Combo(groupContent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		selectIndexer.setLayoutData(data);
		selectIndexer.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (getNameText().getText().trim().equals(""))
					getNameText().setText(
							selectIndexer.getItem(selectIndexer
									.getSelectionIndex()));
				if (getAliasesText().getText().trim().equals(""))
					getAliasesText().setText(
							selectIndexer.getItem(selectIndexer
									.getSelectionIndex()));
			}
		});

		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel
				.setText("Repository inclusion pattern (leave blank to include all repositories in Hawk): ");

		uriTextLabel = new Label(groupContent, SWT.NONE);

		t1 = new Text(groupContent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		// gd.widthHint = GridData.GRAB_HORIZONTAL;
		// gd.minimumWidth = 320;
		t1.setLayoutData(gd);

		uriTextLabel = new Label(groupContent, SWT.NONE);

		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel
				.setText("File inclusion pattern (leave blank to include all files): ");

		uriTextLabel = new Label(groupContent, SWT.NONE);

		t = new Text(groupContent, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		// gd.widthHint = GridData.GRAB_HORIZONTAL;
		// gd.minimumWidth = 320;
		t.setLayoutData(gd);

		uriTextLabel = new Label(groupContent, SWT.NONE);

		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel
				.setText("Enable Full Traversal Scoping (may affect performance): ");

		uriTextLabel = new Label(groupContent, SWT.NONE);

		enableFullTraversalScoping = new Button(groupContent, SWT.CHECK);

		uriTextLabel = new Label(groupContent, SWT.NONE);
		
		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel.setText("Default namespaces (comma separated): ");

		uriTextLabel = new Label(groupContent, SWT.NONE);

		t2 = new Text(groupContent, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		// gd.widthHint = GridData.GRAB_HORIZONTAL;
		// gd.minimumWidth = 320;
		t2.setLayoutData(gd);

		populate();

		// uriText = new Text(groupContent, SWT.BORDER);
		// GridData uriTextGridData = new GridData(GridData.FILL_HORIZONTAL);
		// uriTextGridData.horizontalSpan = 2;
		// uriText.setLayoutData(uriTextGridData);

		groupContent.layout();
		groupContent.pack();
		return groupContent;
	}

	private void populate() {

		// NB only local hawks can be used in this case, for remote ones we need
		// to use the custom hawk option in the hawk UI query dialog
		
		/*for (String i : HUIManager.getInstance().getLocalIndexerNames())
			selectIndexer.add(i);*/
		for (HModel i : (HModel[]) HUIManager.getInstance().getElements(null))
			selectIndexer.add(i.getName());

	}

	protected void loadProperties() {
		super.loadProperties();
		if (properties == null)
			return;

		int found = -1;
		for (int i = 0; i < selectIndexer.getItemCount(); i++) {
			if (selectIndexer.getItem(i).equals(
					properties.getProperty(HawkModel.PROPERTY_INDEXER_NAME))) {
				found = i;
				break;
			}
		}

		selectIndexer.select(found);

		String repos = properties
				.getProperty(IQueryEngine.PROPERTY_REPOSITORYCONTEXT);

		t1.setText(repos);

		String files = properties
				.getProperty(IQueryEngine.PROPERTY_FILECONTEXT);

		t.setText(files);

		String namespaces = properties
				.getProperty(IQueryEngine.PROPERTY_DEFAULTNAMESPACES);

		t2.setText(namespaces);

		String etss = properties
				.getProperty(IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING);
		boolean ets = Boolean.parseBoolean(etss);

		enableFullTraversalScoping.setSelection(ets);

	}

	protected void storeProperties() {
		super.storeProperties();

		String namespaces = t2.getText().trim();
		if (!namespaces.equals(""))
			properties.put(IQueryEngine.PROPERTY_DEFAULTNAMESPACES, namespaces);

		String repos = t1.getText().trim();
		if (!repos.equals(""))
			properties.put(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, repos);

		String files = t.getText().trim();
		if (!files.equals(""))
			properties.put(IQueryEngine.PROPERTY_FILECONTEXT, files);

		if (selectIndexer.getSelectionIndex() != -1)
			properties.put(HawkModel.PROPERTY_INDEXER_NAME,
					selectIndexer.getItem(selectIndexer.getSelectionIndex()));

		boolean ets = enableFullTraversalScoping.getSelection();
		properties.put(IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING,
				new Boolean(ets).toString());

	}

	protected void createNameAliasGroup(Composite parent) {
		final Composite groupContent = createGroupContainer(parent,
				"Identification", 2);

		nameLabel = new Label(groupContent, SWT.NONE);
		nameLabel.setText("Name: ");

		nameText = new Text(groupContent, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		aliasesLabel = new Label(groupContent, SWT.NONE);
		aliasesLabel.setText("Aliases: ");

		aliasesText = new Text(groupContent, SWT.BORDER);
		aliasesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		groupContent.layout();
		groupContent.pack();
	}
}
