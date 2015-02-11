/*******************************************************************************
 * Copyright (c) 2012 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.hawk.emc.dt;

import org.eclipse.epsilon.common.dt.launching.dialogs.AbstractCachedModelConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hawk.ui.HawkUIEclipseViewImpl;

public class HawkModelConfigurationDialog extends
		AbstractCachedModelConfigurationDialog {

	protected String getModelName() {
		return "Hawk";
	}

	protected String getModelType() {
		return "Hawk";
	}

	protected Label uriTextLabel;
	protected Combo selectIndexer;

	protected void createGroups(Composite control) {
		super.createGroups(control);
		createFilesGroup(control);
		createLoadStoreOptionsGroup(control);
	}

	protected Composite createFilesGroup(Composite parent) {

		final Composite groupContent = createGroupContainer(parent,
				"Files/URIs", 2);

		uriTextLabel = new Label(groupContent, SWT.NONE);
		uriTextLabel.setText("Location: ");

		selectIndexer = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		selectIndexer.setLayoutData(data);

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

		for (String i : HawkUIEclipseViewImpl.getIndexerNames())
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

	}

	protected void storeProperties() {
		super.storeProperties();
		properties.put(HawkModel.PROPERTY_INDEXER_NAME,
				selectIndexer.getItem(selectIndexer.getSelectionIndex()));
	}
}
