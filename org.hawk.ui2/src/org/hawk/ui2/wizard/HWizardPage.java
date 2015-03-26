package org.hawk.ui2.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.hawk.ui2.util.HManager;

/**
 * 
 */

public class HWizardPage extends WizardPage {
	private Text folderText;

	private Combo dbidText;

	private Table pluginTable;

	/**
	 * Constructor for .
	 * 
	 * @param pageName
	 */
	public HWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("New Hawk Instance");
		setDescription("This wizard creates a new Hawk Instance to index models repositories.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		Label label = new Label(container, SWT.NULL);
		label.setText("&Local storage folder:");

		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		folderText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		folderText.setLayoutData(gd);
		// folderText.setEditable(false);

		initialize();

		// ModifyListener ml = new ModifyListener() {
		//
		// public void modifyText(ModifyEvent e) {
		//
		// try {
		// String[] nm = folderText.getText().split(
		// File.separator.equals("\\") ? "\\\\"
		// : File.separator);
		// if (!nm[nm.length - 1].equals(getIndexerName())) {
		// // String temp = "";
		// // for (int i = 0; i < nm.length - 1; i++) {
		// // temp += nm[i] + File.separator;
		// // }
		// // temp = temp.substring(0, temp.length() - 1);
		// // folderText.setText(temp);
		// indexerNameText.setText(nm[nm.length - 1]);
		//
		// }
		// } catch (Exception e2) {
		// System.err.println(getIndexerName());
		// System.err.println(folderText.getText());
		// e2.printStackTrace();
		// }
		//
		// dialogChanged();
		// }
		// };

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		label = new Label(container, SWT.NULL);

		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		label.setText("&Hawk plugins:");

		pluginTable = new Table(container, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		pluginTable.setLayoutData(gd);
		pluginTable.setHeaderVisible(false);

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
		label.setText("Selected Hawk Back-end:");

		dbidText = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		// dbidText.set
		dbidText.setLayoutData(gd);
		for (String db : HManager.getIndexTypes())
			dbidText.add(db);
		dbidText.select(0);

		Button startButton = new Button(container, SWT.CHECK);
		startButton.setText("Start with Workspace");
		startButton.setVisible(false);
		startButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("");

		label = new Label(container, SWT.NULL);
		label.setText("");

		Button deleteButton = new Button(container, SWT.CHECK);
		deleteButton.setText("Delete existing indexes");
		deleteButton.setVisible(false);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});

		dialogChanged();
		setControl(container);
	}

	private List<String> getHawkPlugins() {
		List<String> all = new ArrayList<String>();
		all.addAll(HManager.getUpdaterTypes());
		all.addAll(HManager.getIndexTypes());
		all.addAll(HManager.getMetaModelTypes());
		all.addAll(HManager.getModelTypes());
		all.addAll(HManager.getVCSTypes());
		return all;
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {

		// set the default indexer name "MyHawk"
		// indexerNameText.setText("myhawk");
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
			updateStatus("Index storage folder must be empty");
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

		// check plugins form a valid hawk?

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
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
}