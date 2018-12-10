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
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
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
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.osgiserver.HManager;
import org.hawk.ui2.Activator;
import org.hawk.ui2.preferences.HawkPluginSelectionBlock;
import org.hawk.ui2.util.HUIManager;

public class HWizardPage extends WizardPage {

	private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_]");

	private static final class ListContentProvider implements IStructuredContentProvider {
		@Override
		public void dispose() {
			// nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List) {
				return ((List<?>) inputElement).toArray();
			} else {
				return new Object[0];
			}
		}
	}

	private final class DialogChangeSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			dialogChanged();
		}
	}

	private final class DialogChangeModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			dialogChanged();
		}
	}

	private static final String hawkConnectWarning = "Index storage folder must be empty -- Hawk will try to connect to an existing Hawk in this location";

	private Text minDelayText;
	private Text maxDelayText;

	private Text nameText;
	private Text folderText;
	private Combo backendNameText;

	private CheckboxTableViewer metamodelPluginTable;
	private CheckboxTableViewer modelPluginTable;
	private CheckboxTableViewer updaterPluginTable;

	private Combo factoryNameText;
	private Text remoteLocationText;
	private boolean isNew;

	private HUIManager hminstance;
	private Map<String, IHawkFactory> factories;
	private Map<String, IGraphDatabase> backends;
	
	private Map<String, IModelUpdater> updaters;
	private Map<String, ?> models;
	private Map<String, ?> metamodels;
	
	private HawkPluginSelectionBlock pluginSelectionBlock;

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

		GridData gd;
		
		pluginSelectionBlock = new HawkPluginSelectionBlock();
		pluginSelectionBlock.createControl(container);
		
		//enableOrDisable(container, pluginSelectionBlock.getMetamodelTableViewer(), pluginSelectionBlock.getModelTableViewer(), pluginSelectionBlock.getGraphChangeListenerTableViewer());

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
		final List<String> sortedFactories = factories.values().stream().map(IHawkFactory::getHumanReadableName)
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
				updateBackends();
				updatePlugins();
			}
		});

		HManager instance = HManager.getInstance();

		updaters = instance.getModelUpdaterInstances();
		metamodels = instance.getMetamodelParserInstances();
		models = instance.getModelParserInstances();
		
		// modelPluginTable = pluginTable(container, "Model",
		// instance.getModelParserInstances().entrySet().stream().collect(Collectors.toMap(k->k,
		// v->(String) ((IModelResourceFactory)v).getHumanReadableName())));
		// metamodelPluginTable = pluginTable(container, "Meta-Model",
		// toList(instance.getMetaModelTypes()));
		updaterPluginTable = pluginTable(container, "Updater", instance.getModelUpdaterInstances(), new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element !=null ) {
					String updater = (String)element;
					IModelUpdater iModelUpdater = updaters.get(updater);
					return (iModelUpdater != null) ? iModelUpdater.getHumanReadableName() : updater ;					
				} else {
					return "";
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Back-end:");
		backends = hminstance.getBackendInstances();
		backendNameText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		// dbidText.set
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
		List<String> backendNames = backends.values().stream().map(IGraphDatabase::getHumanReadableName)
				.collect(Collectors.toList());
		return backendNames;
	}

	private List<String> toList(Set<String> set) {
		List<String> list = new ArrayList<>();
		list.addAll(set);
		return list;
	}

	private CheckboxTableViewer pluginTable(Composite container, String tableLabel, Map<String,?> plugins, LabelProvider labelProvider) {
		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		String formatter = "&%s plugins:";
		label.setText(String.format(formatter, tableLabel));

		final CheckboxTableViewer tableviewer = CheckboxTableViewer.newCheckList(container,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		tableviewer.setContentProvider(new ListContentProvider());
		tableviewer.setLabelProvider(labelProvider);
		final List<String> values = new ArrayList<String>();
		plugins.keySet().forEach(e->values.add((String) e)); 
		tableviewer.setInput(values);
		
		tableviewer.setAllChecked(true);
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				dialogChanged();
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		tableviewer.getTable().setLayoutData(gd);
		tableviewer.getTable().setHeaderVisible(false);

		tableviewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				dialogChanged();
			}
		});

		enableOrDisable(container, tableviewer);
		
		return tableviewer;
	}

	private void enableOrDisable(Composite parent, final CheckboxTableViewer... tableviewers) {
		Composite cTableButtons = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		cTableButtons.setLayoutData(gd);
		cTableButtons.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button btnEnableAll = new Button(cTableButtons, SWT.NULL);
		btnEnableAll.setText("Enable all");
		btnEnableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (CheckboxTableViewer tableviewer : tableviewers) {
					tableviewer.setAllChecked(true);
				}
				dialogChanged();
			}
		});
		Button btnDisableAll = new Button(cTableButtons, SWT.NULL);
		btnDisableAll.setText("Disable all");
		btnDisableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (CheckboxTableViewer tableviewer : tableviewers) {
					tableviewer.setAllChecked(false);
				}
				dialogChanged();
			}
		});
	}

	private void initialize() {
		// set the default indexer name "MyHawk"
		nameText.setText("myhawk");
		// set the default indexer location
		folderText.setText(basePath + File.separator + "myhawk");
	}

	private String basePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString();

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
		updateBackends();
		updatePlugins();

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
				updateStatus(hawkConnectWarning);
				return;
			}
			// must be writable
			if (!f.getParentFile().exists() && !f.getParentFile().canWrite()) {
				updateStatus("Index storage folder must be writeable");
				return;
			}
			
			if (!containsAny(getSelectedModelUpdaterPlugins(), hminstance.getUpdaterTypes())) {
				updateStatus("At least one updater plugin must be enabled");
				return;
			}
			/*
			if (!containsAny(getSelectedMetaModelPlugins(), hminstance.getMetaModelTypes())) {
				updateStatus("At least one metamodel parser plugin must be enabled");
				return;
			}
			if (!containsAny(getSelectedModelPlugins(), hminstance.getModelTypes())) {
				updateStatus("At least one model parser plugin must be enabled");
				return;
			}*/
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

		// TODO timeaware Validation

		updateStatus(null);
	}

	protected boolean containsAny(final List<String> enabledPlugins, final Set<String> filter) {
		filter.retainAll(enabledPlugins);
		return !filter.isEmpty();
	}

	protected void updatePlugins() {
		HManager instance = HManager.getInstance();

		updaters = instance.getModelUpdaterInstances();
		metamodels = instance.getMetamodelParserInstances();
		models = instance.getModelParserInstances();
		try {
			final IHawkFactory factory = getSelectedFactory();
//			extractPlugins(factory, modelPluginTable, Category.MODEL_RESOURCE_FACTORY); // model resource
//			extractPlugins(factory, metamodelPluginTable, Category.METAMODEL_RESOURCE_FACTORY);
			extractPlugins(factory, updaterPluginTable, Category.MODEL_UPDATER);
		} catch (Exception ex) {
			Activator.logError("Could not refresh plugin list", ex);
		}
	}

	private final void extractPlugins(IHawkFactory factory, final CheckboxTableViewer table, Category c) throws Exception {
		List<IHawkPlugin> plugins = factory.listPlugins(remoteLocationText.getText());
		if (plugins == null) {
			plugins = HUIManager.getInstance().getAvailablePlugins();
		}
		List<String> pluginNames = new ArrayList<>();
		for (IHawkPlugin p : plugins) {
			if (p.getCategory().equals(c)) {				
				pluginNames.add(p.getType());
			};
		}
		Collections.sort(pluginNames);

		final List<IHawkPlugin> oldInput = (List<IHawkPlugin>) table.getInput();
		final List<Object> oldChecked = Arrays.asList(table.getCheckedElements());
		if (!oldInput.equals(plugins)) {
			table.setInput(plugins);
			table.setAllChecked(true);
			for (String newElem : pluginNames) {
				// Keep unchecked values across switches
				if (oldInput.contains(newElem) && !oldChecked.contains(newElem)) {
					table.setChecked(newElem, false);
				}
			}
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null || message.equals(hawkConnectWarning));
		isNew = message == null ? true : !message.equals(hawkConnectWarning);
	}

	protected void updateBackends() {
		try {
			final IHawkFactory factory = getSelectedFactory();
			List<String> backendPlugins = factory.listBackends(remoteLocationText.getText());
			if (backendPlugins == null) {
				backendPlugins = new ArrayList<>();
				backendPlugins.addAll(HUIManager.getInstance().getIndexTypes());
			}
			final List<String> plugins = backendPlugins;
			
			backends = HManager.getInstance().getBackendInstances();
			List<String> newNames = backends.entrySet().stream()
					.filter(e-> {
						plugins.contains(e.getKey());
						return plugins.contains(e.getKey());
						}).map(e->e.getValue().getHumanReadableName()).collect(Collectors.toList());
					
			
			if (!newNames.equals(Arrays.asList(backendNameText.getItems()))) {
				backendNameText.removeAll();
				for (String s : newNames) {
					backendNameText.add(s);
				}
				if (newNames.size() > 0) {
					backendNameText.select(0);
				}
			}
		} catch (Exception ex) {
			Activator.logError("Could not refresh backend list", ex);
		}
	}

	public String getContainerName() {
		return folderText.getText();
	}

	protected List<String> getSelectedModelUpdaterPlugins() {
		return Arrays.asList(updaterPluginTable.getCheckedElements()).stream().map(e -> (String) e).collect(Collectors.toList());
	}

	protected List<String> getSelectedAdvancedPlugins() {
		List<String> selected = new ArrayList<String>();
		selected.addAll(pluginSelectionBlock.getAllChecked());
		selected.addAll(getSelectedModelUpdaterPlugins());
		return selected;
	}
	

	public String getDBID() {
		return (String) backends.entrySet().stream().filter(e-> e.getValue().getHumanReadableName().equals(backendNameText.getText())).findFirst().map(e->(String) e.getKey()).get();
			//return backendNameText.getText();
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
}
