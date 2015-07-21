/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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
	protected Text t;

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
				.setText("File inclusion pattern (leave blank to include all files in Hawk): ");

		uriTextLabel = new Label(groupContent, SWT.NONE);

		t = new Text(groupContent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.widthHint = GridData.GRAB_HORIZONTAL;
		//gd.minimumWidth = 320;
		t.setLayoutData(gd);

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

		for (String i : HUIManager.getInstance().getIndexerNames())
			selectIndexer.add(i);

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

		String files = properties
				.getProperty(HawkModel.PROPERTY_FILE_INCLUSION_PATTERN);

		t.setText(files);

	}

	protected void storeProperties() {
		super.storeProperties();
		String files = t.getText().trim();
		if (!files.equals(""))
			properties.put(HawkModel.PROPERTY_FILE_INCLUSION_PATTERN, files);

		if (selectIndexer.getSelectionIndex() != -1)
			properties.put(HawkModel.PROPERTY_INDEXER_NAME,
					selectIndexer.getItem(selectIndexer.getSelectionIndex()));
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
