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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hawk.ui2.Activator;

public class HawkPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore store;
	
    public HawkPreferencePage() {
        super("Default Enabled Plugins");
        setDescription("Welcome to the Hawk Preferences sections");
    	store = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(store);
    }
    
    @Override
    public void init(IWorkbench workbench) {
    	//  new StringFieldEditor(HawkPreferenceConstants.X, "Server:", getFieldEditorParent()));
    }


	@Override	
	protected Control createContents(Composite parent) {
		return parent;
	}
   


}
