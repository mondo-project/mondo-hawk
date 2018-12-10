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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class HawkInstanceManagerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	private HawkInstanceBlock hawkBlock;

	public HawkInstanceManagerPreferencePage() {
		super("Index Instances");
	}
	
	@Override
	public void init(IWorkbench workbench) { }

	@Override
	protected Control createContents(Composite parent) {
		/** Super preferences */
		initializeDialogUnits(parent);
		noDefaultButton();
		
		/** ----- */
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		
		Label DescriptionLabel = new Label(parent, SWT.WRAP);
		DescriptionLabel.setText("Add, remove or edit Hawk instances.");
		//label.setAlignment(1);
		//SWTFactory.createWrapLabel(ancestor, JREMessages.JREsPreferencePage_2, 1, 300);
		//new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);	
		
		hawkBlock = new HawkInstanceBlock();
		hawkBlock.createControl(parent);
		Control control = hawkBlock.getControl();
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		control.setLayoutData(data);
		
		return parent;
	}
	
}
