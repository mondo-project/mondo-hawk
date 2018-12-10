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
import java.util.List;
import java.util.Set;

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
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class HawkInstancePluginConfigurationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Combo combo;

	private HawkPluginSelectionBlock pluginBlock;

	public HawkInstancePluginConfigurationPreferencePage() {
		super("Instance Configuration");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		/** Super preferences */
		initializeDialogUnits(parent);

		/** ----- */
		combo = new Combo(parent, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		combo.setLayoutData(gd);
		Set<HModel> hawks = HUIManager.getInstance().getHawks();
		for (HModel db : hawks) {
			combo.add(db.getName());
		}
		combo.select(0);

		pluginBlock = new HawkPluginSelectionBlock();
		pluginBlock.createControl(parent);
		pluginBlock.getMetamodelTableViewer().setCheckStateProvider(new TypedCheckStateProvider(IMetaModelResourceFactory.class));
		pluginBlock.getGraphChangeListenerTableViewer().setCheckStateProvider(new TypedCheckStateProvider(IGraphChangeListener.class));
		pluginBlock.getModelTableViewer().setCheckStateProvider(new TypedCheckStateProvider(IModelResourceFactory.class));

		

		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginBlock.update();
			}
		});

		pluginBlock.update();
		getSelectedHawkInstance();

		return parent;
	}

	@Override
	protected void performDefaults() {
		pluginBlock.update();
	}

	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	@Override
	protected void performApply() {
		try {
			HModel hawk = getSelectedHawkInstance();
			hawk.addPlugins(getAdditionalPluginsFromChecked());
			// hawk.removePlugins(getPluginsToRemoveFromUnchecked());
			HUIManager.getInstance().saveHawkToMetadata(hawk, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		pluginBlock.update();
	}

	public HModel getSelectedHawkInstance() {
		return HUIManager.getInstance().getHawkByName(combo.getItem(combo.getSelectionIndex()));
	}

	
	private List<String> getAdditionalPluginsFromChecked() {
		List<String> result = new ArrayList<>();
		List<String> enabledPlugins = getSelectedHawkInstance().getEnabledPlugins();
		pluginBlock.getAllChecked().stream().forEach(type -> {
			if (!enabledPlugins.contains(type)) {
				result.add(type);
			}
		});
		return result;
	}

	// FIXME
	private List<String> getPluginsToRemoveFromUnchecked() {
		List<String> enabled = new ArrayList<>(getSelectedHawkInstance().getEnabledPlugins());
		enabled.removeAll(pluginBlock.getAllChecked());
		return enabled;
	}


	class TypedCheckStateProvider implements ICheckStateProvider {

		private Class<?> clazz;

		public TypedCheckStateProvider(Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public boolean isGrayed(Object element) {
			return false;
		}

		@Override
		public boolean isChecked(Object element) {
			try {
				String plugin = (String) clazz.getMethod("getType").invoke(clazz.cast(element));
				return getSelectedHawkInstance().getEnabledPlugins().contains(plugin);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

}