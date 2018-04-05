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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Paired label and checkbox field.
 */
public class FormCheckBoxField {
	private final Label label;
	private final Button checkbox;

	public FormCheckBoxField(FormToolkit toolkit, Composite sectionClient, String labelText, boolean defaultValue) {
	    label = toolkit.createLabel(sectionClient, labelText, SWT.WRAP);
	    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

	    final TableWrapData layoutData = new TableWrapData();
		layoutData.valign = TableWrapData.MIDDLE;
		label.setLayoutData(layoutData);

		checkbox = toolkit.createButton(sectionClient, "", SWT.CHECK);
		checkbox.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		checkbox.setSelection(defaultValue);
	}

	public Button getCheck() {
		return checkbox;
	}
}