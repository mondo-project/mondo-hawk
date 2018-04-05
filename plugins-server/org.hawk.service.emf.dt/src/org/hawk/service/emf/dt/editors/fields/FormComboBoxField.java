/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.emf.dt.editors.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Combo box field that takes up two columns in the grid.
 */
public class FormComboBoxField {
	private final Label label;
	private final Combo combobox;

	public FormComboBoxField(FormToolkit toolkit, Composite sectionClient, String labelText, String[] options) {
	    label = toolkit.createLabel(sectionClient, labelText, SWT.WRAP);
	    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

	    final TableWrapData layoutData = new TableWrapData();
		layoutData.valign = TableWrapData.MIDDLE;
		label.setLayoutData(layoutData);

		combobox = new Combo(sectionClient, SWT.READ_ONLY);
		combobox.setItems(options);
		combobox.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}

	public Combo getCombo() {
		return combobox;
	}
}