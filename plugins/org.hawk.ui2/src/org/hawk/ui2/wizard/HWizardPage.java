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
import org.eclipse.swt.widgets.Text;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.osgiserver.HManager;
import org.hawk.ui2.Activator;
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
			}
			else {
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
	private Combo dbidText;
	private CheckboxTableViewer pluginTable;
	private Combo factoryIdText;
	private Text locationText;
	private boolean isNew;

	private HUIManager hminstance;
	private Map<String, IHawkFactory> factories;

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

		factoryIdText = new Combo(container, SWT.READ_ONLY);
		factories = hminstance.getHawkFactoryInstances();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		factoryIdText.setLayoutData(gd);
		final List<String> sortedFactories = new ArrayList<>(factories.keySet());
		Collections.sort(sortedFactories);
		for (String factory : sortedFactories) {
			factoryIdText.add(factory);
		}
		factoryIdText.select(0);
		factoryIdText.addSelectionListener(new DialogChangeSelectionListener());

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

		locationText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		locationText.setLayoutData(gd);
		locationText.setText("http://localhost:8080/thrift/hawk/tuple");
		// folderText.setEditable(false);
		locationText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateBackends();
				updatePlugins();
			}
		});

		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		label.setText("&Enabled plugins:");

		pluginTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		pluginTable.setContentProvider(new ListContentProvider());
		pluginTable.setLabelProvider(new LabelProvider());
		pluginTable.setInput(HManager.getInstance().getAvailablePlugins());
		pluginTable.setAllChecked(true);
		pluginTable.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				dialogChanged();
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		pluginTable.getTable().setLayoutData(gd);
		pluginTable.getTable().setHeaderVisible(false);

		pluginTable.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				dialogChanged();
			}
		});

		Composite cTableButtons = new Composite(container, SWT.NULL);
		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		cTableButtons.setLayoutData(gd);
		cTableButtons.setLayout(new FillLayout(SWT.VERTICAL));
		Button btnEnableAll = new Button(cTableButtons, SWT.NULL);
		btnEnableAll.setText("Enable all");
		btnEnableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginTable.setAllChecked(true);
				dialogChanged();
			}
		});
		Button btnDisableAll = new Button(cTableButtons, SWT.NULL);
		btnDisableAll.setText("Disable all");
		btnDisableAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginTable.setAllChecked(false);
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Back-end:");

		dbidText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		// dbidText.set
		dbidText.setLayoutData(gd);
		for (String db : hminstance.getIndexTypes())
			dbidText.add(db);
		dbidText.select(0);

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
		minDelayText
				.setToolTipText("Minimum delay between periodic synchronisations in milliseconds.");
		minDelayText.addModifyListener(dialogChangeListener);

		maxDelayText = new Text(cDelayRow, SWT.BORDER | SWT.SINGLE);
		maxDelayText.setText(ModelIndexerImpl.DEFAULT_MAXDELAY + "");
		maxDelayText
				.setToolTipText("Maximum delay between periodic synchronisations in milliseconds (0 disables periodic synchronisations).");
		maxDelayText.addModifyListener(dialogChangeListener);

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void initialize() {
		// set the default indexer name "MyHawk"
		nameText.setText("myhawk");
		// set the default indexer location
		folderText.setText(basePath + File.separator + "myhawk");
	}

	private String basePath = ResourcesPlugin.getWorkspace().getRoot()
			.getLocation().toFile().toString();

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */
	private void handleBrowse() {
		DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);

		dd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile().toString());
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
		final IHawkFactory factory = factories.get(getFactoryID());
		locationText.setEnabled(factory.instancesUseLocation());
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
		// must not already exist
		File f = new File(getContainerName());
		if (f.exists() && f.isDirectory() && f.listFiles().length > 0) {
			updateStatus(hawkConnectWarning);
			return;
		}
		// must be writable
		if (!f.getParentFile().exists() && !f.getParentFile().canWrite()) {
			updateStatus("Index storage folder must be writeable");
			return;
		}

		// check back-end is chosen
		if (dbidText.getText().equals("")) {
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

		// check plugins form a valid Hawk
		final List<String> enabledPlugins = getPlugins();
		if (!containsAny(enabledPlugins, hminstance.getUpdaterTypes())) {
			updateStatus("At least one updater plugin must be enabled");
			return;
		}
		if (!containsAny(enabledPlugins, hminstance.getMetaModelTypes())) {
			updateStatus("At least one metamodel parser plugin must be enabled");
			return;
		}
		if (!containsAny(enabledPlugins, hminstance.getModelTypes())) {
			updateStatus("At least one model parser plugin must be enabled");
			return;
		}

		updateStatus(null);
	}

	protected boolean containsAny(final List<String> enabledPlugins, final Set<String> filter) {
		filter.retainAll(enabledPlugins);
		return !filter.isEmpty();
	}

	@SuppressWarnings("unchecked")
	protected void updatePlugins() {
		try {
			final IHawkFactory factory = factories.get(getFactoryID());
			List<IHawkPlugin> plugins = factory.listPlugins(locationText.getText());
			if (plugins == null) {
				plugins = HUIManager.getInstance().getAvailablePlugins();
			}

			List<String> pluginNames = new ArrayList<>();
			for (IHawkPlugin p : plugins) {
				switch (p.getCategory()) {
				case METAMODEL_RESOURCE_FACTORY:
				case MODEL_RESOURCE_FACTORY:
				case MODEL_UPDATER:
				case GRAPH_CHANGE_LISTENER:
					pluginNames.add(p.getType());
					break;
				default:
					break;
				}
			}
			Collections.sort(pluginNames);

			final List<String> oldInput = (List<String>) pluginTable.getInput();
			final List<Object> oldChecked = Arrays.asList(pluginTable.getCheckedElements());
			if (!oldInput.equals(pluginNames)) {
				pluginTable.setInput(pluginNames);
				pluginTable.setAllChecked(true);
				for (String newElem : pluginNames) {
					// Keep unchecked values across switches
					if (oldInput.contains(newElem) && !oldChecked.contains(newElem)) {
						pluginTable.setChecked(newElem, false);
					}
				}
			}
		} catch (Exception ex) {
			Activator.logError("Could not refresh plugin list", ex);
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null || message.equals(hawkConnectWarning));
		isNew = message == null ? true : !message.equals(hawkConnectWarning);
	}

	protected void updateBackends() {
		try {
			final IHawkFactory factory = factories.get(getFactoryID());
			List<String> backends = factory
					.listBackends(locationText.getText());
			if (backends == null) {
				backends = new ArrayList<>();
				backends.addAll(HUIManager.getInstance().getIndexTypes());
			}
			String[] newItems = backends.toArray(new String[backends.size()]);
			if (!Arrays.equals(newItems, dbidText.getItems())) {
				dbidText.removeAll();
				for (String s : backends) {
					dbidText.add(s);
				}
				if (newItems.length > 0) {
					dbidText.select(0);
				}
			}
		} catch (Exception ex) {
			Activator.logError("Could not refresh backend list", ex);
		}
	}

	public String getContainerName() {
		return folderText.getText();
	}

	public List<String> getPlugins() {
		List<String> selected = new ArrayList<String>();
		for (Object checked : pluginTable.getCheckedElements()) {
			selected.add(checked.toString());
		}
		return selected;
	}

	public String getDBID() {
		return dbidText.getText();
	}

	public String getHawkName() {
		return nameText.getText();
	}

	public String getFactoryID() {
		return factoryIdText.getText();
	}

	public IHawkFactory getFactory() throws CoreException {
		return hminstance.createHawkFactory(getFactoryID());
	}

	public String getLocation() {
		return locationText.getText();
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
