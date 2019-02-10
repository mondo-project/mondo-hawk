/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York, Aston University.
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
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 *     Antonio Garcia-Dominguez - add location/factory ID fields
 ******************************************************************************/
package org.hawk.ui2.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IHawkPlugin.Category;
import org.hawk.core.IModelUpdater;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.osgiserver.HManager;
import org.hawk.ui2.Activator;
import org.hawk.ui2.preferences.HawkPluginSelectionBlock;
import org.hawk.ui2.util.HUIManager;

public class HWizardPage extends WizardPage {
	private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_]");

	private final class DialogChangeSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			updatePlugins();
			dialogChanged();
		}
	}

	private final class DialogChangeModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			dialogChanged();
		}
	}

	private static final String HAWK_CONNECT_WARNING =
		"Index storage folder must be empty -- "
		+ "Hawk will try to connect to an existing Hawk in this location";

	private Text minDelayText;
	private Text maxDelayText;

	private Text nameText;
	private Text folderText;
	private Combo backendNameText;
	private Combo updaterNameText;
	private Combo factoryNameText;
	private Text remoteLocationText;
	private boolean isNew;

	private HUIManager hminstance;
	private Map<String, IHawkFactory> factories;
	private Map<String, IGraphDatabase> backends;
	private Map<String, IModelUpdater> updaters;
	
	private HawkPluginSelectionBlock pluginSelectionBlock;

	private String basePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString();

	public HWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("New Hawk Instance");
		setDescription("This wizard creates a new Hawk Instance to index model repositories.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		hminstance = HUIManager.getInstance();

		TabFolder tabFolder = new TabFolder(parent, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem mainConfigTab = new TabItem(tabFolder, SWT.NULL);
		mainConfigTab.setText("Base Configuration");
		mainConfigTab.setControl(createBaseConfig(tabFolder));
		
		TabItem advancedTab = new TabItem(tabFolder, SWT.NULL);
		advancedTab.setText("Advanced");
		advancedTab.setControl(createAdvancedConfig(tabFolder));
	}

	private Composite createAdvancedConfig(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		pluginSelectionBlock = new HawkPluginSelectionBlock();
		pluginSelectionBlock.createControl(container);
		
		return container;
	}
	
	private Composite createBaseConfig(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		GridData gd;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Name:");

		nameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		nameText.setLayoutData(gd);
		final DialogChangeModifyListener dialogChangeListener = new DialogChangeModifyListener();
		nameText.addModifyListener(dialogChangeListener);

		label = new Label(container, SWT.NULL);
		label.setText("Instance type:");

		factoryNameText = new Combo(container, SWT.READ_ONLY);
		factories = hminstance.getHawkFactoryInstances();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		factoryNameText.setLayoutData(gd);
		final List<String> sortedFactories = factories.values().stream()
			.map(IHawkFactory::getHumanReadableName)
			.collect(Collectors.toList());
		Collections.sort(sortedFactories);
		for (String factory : sortedFactories) {
			factoryNameText.add(factory);
		}
		factoryNameText.select(0);
		factoryNameText.addSelectionListener(new DialogChangeSelectionListener());

		label = new Label(container, SWT.NULL);
		label.setText("&Local storage folder:");

		folderText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		folderText.setLayoutData(gd);
		// folderText.setEditable(false);
		folderText.addModifyListener(dialogChangeListener);

		Button button = new Button(container, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		button.setLayoutData(gd);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Remote location:");

		remoteLocationText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		remoteLocationText.setLayoutData(gd);
		remoteLocationText.setText("http://localhost:8080/thrift/hawk/tuple");
		// folderText.setEditable(false);
		remoteLocationText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updatePlugins();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Updater:");
		updaters = hminstance.getModelUpdaterInstances();
		updaterNameText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		updaterNameText.setLayoutData(gd);
		List<String> updaterNames = getUpdaterNames();
		for (String db : updaterNames)
			updaterNameText.add(db);
		updaterNameText.select(0);
		
		label = new Label(container, SWT.NULL);
		label.setText("Back-end:");
		backends = hminstance.getBackendInstances();
		backendNameText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		backendNameText.setLayoutData(gd);
		List<String> backendNames = getBackendNames();
		for (String db : backendNames)
			backendNameText.add(db);
		backendNameText.select(0);

		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		label.setText("Min/Max Delay:");

		Composite cDelayRow = new Composite(container, SWT.NULL);
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		gd.horizontalSpan = 2;
		cDelayRow.setLayoutData(gd);
		final Layout cDelayRowLayout = new FillLayout();
		cDelayRow.setLayout(cDelayRowLayout);

		minDelayText = new Text(cDelayRow, SWT.BORDER | SWT.SINGLE);
		minDelayText.setText(ModelIndexerImpl.DEFAULT_MINDELAY + "");
		minDelayText.setToolTipText("Minimum delay between periodic synchronisations in milliseconds.");
		minDelayText.addModifyListener(dialogChangeListener);

		maxDelayText = new Text(cDelayRow, SWT.BORDER | SWT.SINGLE);
		maxDelayText.setText(ModelIndexerImpl.DEFAULT_MAXDELAY + "");
		maxDelayText.setToolTipText(
				"Maximum delay between periodic synchronisations in milliseconds (0 disables periodic synchronisations).");
		maxDelayText.addModifyListener(dialogChangeListener);

		initialize();
		dialogChanged();
		setControl(container);
		
		return container;
	}

	private List<String> getBackendNames() {
		final List<String> backendNames = backends.values().stream()
			.map(IHawkPlugin::getHumanReadableName)
			.collect(Collectors.toList());
		Collections.sort(backendNames);
		return backendNames;
	}

	private List<String> getUpdaterNames() {
		final List<String> updaterNames = updaters.values().stream()
			.map(IHawkPlugin::getHumanReadableName)
			.collect(Collectors.toList());

		Collections.sort(updaterNames);
		return updaterNames;
	}

	private void initialize() {
		// set the default indexer name "MyHawk"
		nameText.setText("myhawk");
		// set the default indexer location
		folderText.setText(basePath + File.separator + "myhawk");
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for the
	 * container field.
	 */
	private void handleBrowse() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);

		dd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString());
		dd.setMessage("Select a folder where the index files will be stored");
		dd.setText("Select a directory");
		String result = dd.open();

		if (result != null) {
			folderText.setText(result);
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */
	private void dialogChanged() {
		IHawkFactory factory = getSelectedFactory();
		folderText.setEnabled(!factory.isRemote());
		remoteLocationText.setEnabled(factory.isRemote());
		
		// name empty or valid chars in indexername
		if (getHawkName().trim().equals("")) {
			updateStatus("Hawk name must not be empty.");
			return;
		}
		Matcher m = PATTERN.matcher(getHawkName());
		if (m.find()) {
			updateStatus("Hawk name must contain only letters, numbers or underscores.");
			return;
		}

		if (getContainerName().length() == 0) {
			updateStatus("Indexer name/folder must be specified");
			return;
		}

		if (getContainerName().length() == 0) {
			updateStatus("Index storage folder must be specified");
			return;
		}
		if (!factory.isRemote()) {
			File f = new File(getContainerName());
			// must not already exist
			if (f.exists() && f.isDirectory() && f.listFiles().length > 0) {
				updateStatus(HAWK_CONNECT_WARNING);
				return;
			}
			// must be writable
			if (!f.getParentFile().exists() && !f.getParentFile().canWrite()) {
				updateStatus("Index storage folder must be writeable");
				return;
			}
			
		}

		// check back-end is chosen
		if (backendNameText.getText().equals("")) {
			updateStatus("Hawk back-end needs to be selected");
			return;
		}

		// check min/max delays
		int minDelay;
		try {
			minDelay = Integer.parseInt(minDelayText.getText());
		} catch (NumberFormatException ex) {
			updateStatus("Minimum delay must be an integer");
			return;
		}
		int maxDelay;
		try {
			maxDelay = Integer.parseInt(maxDelayText.getText());
		} catch (NumberFormatException ex) {
			updateStatus("Maximum delay must be an integer");
			return;
		}
		if (minDelay > maxDelay) {
			updateStatus("Minimum delay must be less than or equal to maximum delay");
			return;
		}
		if (minDelay < 0) {
			updateStatus("Minimum delay must be greater than or equal to zero");
			return;
		}
		if (maxDelay < 0) {
			updateStatus("Maximum delay must be greater than or equal to zero");
			return;
		}

		updateStatus(null);
	}

	private void updatePlugins() {
		// plugins in advanced tab (model, metamodel and graph listeners)
		if (pluginSelectionBlock != null) {			
			pluginSelectionBlock.update(getAvailablePlugins());
		}
		update(updaterNameText, Category.MODEL_UPDATER);
		update(backendNameText, Category.BACKEND);
	}

	protected boolean containsAny(final List<String> enabledPlugins, final Set<String> filter) {
		filter.retainAll(enabledPlugins);
		return !filter.isEmpty();
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null || message.equals(HAWK_CONNECT_WARNING));
		isNew = message == null ? true : !message.equals(HAWK_CONNECT_WARNING);
	}

	protected void update(Combo combo, Category category) {
		try {
			List<IHawkPlugin> plugins = getAvailablePlugins();
			
			// Select plugins of requested category
			plugins = plugins.parallelStream()
					.filter(p -> p.getCategory().equals(category))
					.collect(Collectors.toList());
			
			List<String> newNames = plugins.parallelStream()
					.map(IHawkPlugin::getHumanReadableName)
					.collect(Collectors.toList());
					
			if (!newNames.equals(Arrays.asList(combo.getItems()))){
				combo.removeAll();
				for (String s : newNames) {
					combo.add(s);
				}
				if (newNames.size() > 0) {
					combo.select(0);
				}
			}
		} catch (Exception ex) {
			Activator.logError("Could not refresh "+category.name()+" list", ex);
		}
	}

	private List<IHawkPlugin> getAvailablePlugins() {
		List<IHawkPlugin> plugins;
		try {
			final IHawkFactory factory = getSelectedFactory();
			plugins = factory.listPlugins(remoteLocationText.getText());
			plugins = HUIManager.getInstance().getAvailablePlugins();
		} catch (Exception e) {
			plugins = Collections.emptyList();
		}
		return plugins;
	}

	public String getContainerName() {
		return folderText.getText();
	}

	public String getDBID() {
		return (String) backends.entrySet().stream().filter(e-> e.getValue().getHumanReadableName().equals(backendNameText.getText())).findFirst().map(e->(String) e.getKey()).get();
	}
	
	public String getUpdater() {
		return (String) updaters.entrySet().stream().filter(e-> e.getValue().getHumanReadableName().equals(updaterNameText.getText())).findFirst().map(e->(String) e.getKey()).get();
	}

	public String getHawkName() {
		return nameText.getText();
	}
	
	protected String getSelectedFactoryPluginId(){
		String selected =  factoryNameText.getText();
		List<String> plugins = factories.entrySet().stream()
				.filter(e -> selected.equals(e.getValue().getHumanReadableName())).map(m -> m.getKey())
				.collect(Collectors.toList());
		return plugins.get(0);
	}
	
	protected IHawkFactory getSelectedFactory(){
		return factories.get(getSelectedFactoryPluginId());
	}

	public IHawkFactory getFactory() throws CoreException {
		return hminstance.createHawkFactory(getSelectedFactoryPluginId());
	}

	public String getLocation() {
		return remoteLocationText.getText();
	}

	public int getMaxDelay() {
		try {
			return Integer.parseInt(maxDelayText.getText());
		} catch (Exception e) {
			return ModelIndexerImpl.DEFAULT_MAXDELAY;
		}
	}

	public int getMinDelay() {
		try {
			return Integer.parseInt(minDelayText.getText());
		} catch (Exception e) {
			return ModelIndexerImpl.DEFAULT_MINDELAY;
		}
	}

	public boolean isNew() {
		return isNew;
	}
	
	protected List<String> getSelectedAdvancedPlugins() {
		List<String> selected = new ArrayList<String>();
		selected.addAll(pluginSelectionBlock.getAllChecked());
		selected.add(getUpdater());
		return selected;
	}
	
}
