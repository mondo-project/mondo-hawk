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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hawk.core.IHawkPlugin;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class HawkInstancePluginConfigurationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo combo;

	private HawkPluginSelectionBlock pluginBlock;

	public HawkInstancePluginConfigurationPreferencePage() {
		super("Instance Configuration");
		setDescription("You may enable plugins but not disable them");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		/** Super preferences */
		initializeDialogUnits(parent);

		/** ----- */
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;

		combo = new Combo(parent, SWT.READ_ONLY);
		combo.setLayoutData(gd);
		Set<HModel> hawks = HUIManager.getInstance().getHawks();
		for (HModel db : hawks) {
			combo.add(db.getName());
		}
		combo.select(0);

		pluginBlock = new HawkPluginSelectionBlock();
		pluginBlock.createControl(parent);
		pluginBlock.getMetamodelTableViewer().setCheckStateProvider(new TypedCheckStateProvider());
		pluginBlock.getGraphChangeListenerTableViewer().setCheckStateProvider(new TypedCheckStateProvider());
		pluginBlock.getModelTableViewer().setCheckStateProvider(new TypedCheckStateProvider());

		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updatePlugins();
			}
		});

		updatePlugins();
		
		return parent;
	}

	private void updatePlugins(){
		List<IHawkPlugin> plugins = getSelectedHawkInstance().getManager().getAvailablePlugins();
		pluginBlock.update(plugins);
	}
	
	@Override
	protected void performDefaults() {
		updatePlugins();
	}

	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	@Override
	protected void performApply() {
		try {
			List<String> additionalPlugins = getAdditionalPluginsFromChecked();
			if (!additionalPlugins.isEmpty()) {
				HModel hawk = getSelectedHawkInstance();
				hawk.addPlugins(additionalPlugins);
				HUIManager.getInstance().saveHawkToMetadata(hawk, true);
				updatePlugins();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HModel getSelectedHawkInstance() {
		return HUIManager.getInstance().getHawkByName(combo.getItem(combo.getSelectionIndex()));
	}

	private List<String> getAdditionalPluginsFromChecked() {
		List<String> enabledPlugins = getSelectedHawkInstance().getEnabledPlugins();
		return pluginBlock.getAllChecked().stream()
				.filter(p -> !enabledPlugins.contains(p))
				.collect(Collectors.toList());
	}

	class TypedCheckStateProvider implements ICheckStateProvider {

		@Override
		public boolean isGrayed(Object element) {
			return false;
		}

		@Override
		public boolean isChecked(Object element) {
			return getSelectedHawkInstance().getEnabledPlugins().contains(((IHawkPlugin)element).getType());
			
		}
	}

}