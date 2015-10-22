/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Seyyed Shah - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 *     Antonio Garcia-Dominguez - add location/factory ID fields
 ******************************************************************************/
package org.hawk.ui2.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.hawk.core.IHawkFactory;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.ui2.util.HUIManager;

public class HWizardPage extends WizardPage {

	private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_]");

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
	private Table pluginTable;
	private Combo factoryIdText;
	private Text locationText;

	private HUIManager hminstance;
	private Set<String> factoriesWithLocation, factoriesWithGraph;

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
		nameText.setLayoutData(gd);
		final DialogChangeModifyListener dialogChangeListener = new DialogChangeModifyListener();
		nameText.addModifyListener(dialogChangeListener);

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		label.setText("Instance type:");

		factoryIdText = new Combo(container, SWT.READ_ONLY);
		factoriesWithLocation = new HashSet<>();
		factoriesWithGraph = new HashSet<>();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		factoryIdText.setLayoutData(gd);
		for (Map.Entry<String, IHawkFactory> factory : hminstance
				.getHawkFactoryInstances().entrySet()) {
			factoryIdText.add(factory.getKey());
			if (factory.getValue().instancesUseLocation()) {
				factoriesWithLocation.add(factory.getKey());
			}
			if (factory.getValue().instancesCreateGraph()) {
				factoriesWithGraph.add(factory.getKey());
			}
		}
		factoryIdText.select(0);
		factoryIdText.addSelectionListener(new DialogChangeSelectionListener());

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		label.setText("&Local storage folder:");

		folderText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		folderText.setLayoutData(gd);
		// folderText.setEditable(false);
		folderText.addModifyListener(dialogChangeListener);

		Button button = new Button(container, SWT.PUSH);
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
		locationText.setLayoutData(gd);
		// folderText.setEditable(false);
		locationText.addModifyListener(dialogChangeListener);

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		label.setText("&Enabled plugins:");

		pluginTable = new Table(container, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		pluginTable.setLayoutData(gd);
		pluginTable.setHeaderVisible(false);
		pluginTable.setEnabled(false);

		TableColumn column = new TableColumn(pluginTable, SWT.NULL);
		column.setText("plugin");

		for (String plugin : this.getHawkPlugins()) {
			TableItem item = new TableItem(pluginTable, SWT.NULL);
			item.setText(plugin);
			item.setText(0, plugin);
			// item.setChecked(true);
		}

		pluginTable.getColumn(0).pack();

		pluginTable.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					dialogChanged();
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		label.setText("Back-end:");

		dbidText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		// dbidText.set
		dbidText.setLayoutData(gd);
		for (String db : hminstance.getIndexTypes())
			dbidText.add(db);
		dbidText.select(0);

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		label.setText("Min/Max Delay:");

		Composite cDelayRow = new Composite(container, SWT.NULL);
		cDelayRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final Layout cDelayRowLayout = new FillLayout();
		cDelayRow.setLayout(cDelayRowLayout);

		minDelayText = new Text(cDelayRow, SWT.BORDER | SWT.SINGLE);
		minDelayText.setText(ModelIndexerImpl.DEFAULT_MINDELAY + "");
		minDelayText.setToolTipText("Minimum delay between periodic synchronisations in milliseconds.");
		minDelayText.addModifyListener(dialogChangeListener);

		maxDelayText = new Text(cDelayRow, SWT.BORDER | SWT.SINGLE);
		maxDelayText.setText(ModelIndexerImpl.DEFAULT_MAXDELAY + "");
		maxDelayText.setToolTipText("Maximum delay between periodic synchronisations in milliseconds (0 disables periodic synchronisations).");
		maxDelayText.addModifyListener(dialogChangeListener);

		Button startButton = new Button(container, SWT.CHECK);
		startButton.setText("Start with Workspace");
		startButton.setVisible(false);
		startButton.addSelectionListener(new DialogChangeSelectionListener());

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		label.setText("");

		Button deleteButton = new Button(container, SWT.CHECK);
		deleteButton.setText("Delete existing indexes");
		deleteButton.setVisible(false);
		deleteButton.addSelectionListener(new DialogChangeSelectionListener());

		initialize();
		dialogChanged();
		setControl(container);
	}

	private List<String> getHawkPlugins() {
		List<String> all = new ArrayList<String>();
		all.addAll(hminstance.getUpdaterTypes());
		all.addAll(hminstance.getIndexTypes());
		all.addAll(hminstance.getMetaModelTypes());
		all.addAll(hminstance.getModelTypes());
		all.addAll(hminstance.getVCSTypes());
		return all;
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

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
		final String factoryID = getFactoryID();
		locationText.setEnabled(factoriesWithLocation.contains(factoryID));
		dbidText.setEnabled(factoriesWithGraph.contains(factoryID));

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

		// check plugins form a valid hawk?
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null || message.equals(hawkConnectWarning));
	}

	public String getContainerName() {
		return folderText.getText();
	}

	public List<String> getPlugins() {
		List<String> selected = new ArrayList<String>();

		for (TableItem t : pluginTable.getItems()) {
			if (t.getChecked())
				selected.add(t.getText());
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
}
