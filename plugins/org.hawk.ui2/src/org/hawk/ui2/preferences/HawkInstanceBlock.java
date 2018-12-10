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

import java.util.Set;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.Activator;
import org.hawk.ui2.dialog.HConfigDialog;
import org.hawk.ui2.dialog.HImportDialog;
import org.hawk.ui2.util.HUIManager;
import org.hawk.ui2.wizard.HWizard;
import org.osgi.service.prefs.BackingStoreException;

public class HawkInstanceBlock {

	private Button createButton;
	private Button importButton;
	private Button removeButton;
	private Button configButton;
	private Button startButton;
	private Button stopButton;

	private Composite control;

	private TableViewer instanceListTableViewer;
	private Table fTable;
		
	class HawkIndexContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object input) {
			return (Object[]) getIndexes();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }

		@Override
		public void dispose() { }

	}

	/** FROM VIEW */
	class HawkIndexLabelProvider extends LabelProvider implements ITableLabelProvider {

		protected Image runningImage = null;
		protected Image stoppedImage = null;

		public String getColumnText(Object obj, int index) {
			HModel hModel = (HModel) obj;
			if (index == 0)
				return hModel.getName();
			else if (index == 1)
				return hModel.getFolder();
			else 
				return hModel.getStatus().toString();
		}

		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return getImage(obj);
			else
				return null;
		}

		public Image getImage(Object obj) {

			if (runningImage == null) {
				runningImage = Activator.getImageDescriptor("icons/hawk-running.png").createImage();
				stoppedImage = Activator.getImageDescriptor("icons/hawk-stopped.png").createImage();
			}

			HModel hModel = (HModel) obj;

			if (hModel.isRunning()) {
				return runningImage;
			} else {
				return stoppedImage;
			}
		}

	}
	
	public Control getControl() {
		return control;
	}
	
	protected Shell getShell() {
		return getControl().getShell();
	}
	
	public void createControl(Composite ancestor) {

		Font font = ancestor.getFont();
	
		Composite parent = new Composite(ancestor, SWT.NONE);
		parent.setFont(font);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 1;
		parent.setLayout(layout);
		
		control = parent;

		/*Label label = new Label(parent, 2);
		label.setText("Instanced &Indexes:");*/

		fTable= new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		gd.widthHint = 350;
		fTable.setLayoutData(gd);
		fTable.setFont(font);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);

		int defaultwidth = 350/3 +1;

		TableColumn column = new TableColumn(fTable, SWT.NULL);
		column.setText("Name");
		column.setWidth(defaultwidth);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//sortByName(); FIXME
				instanceListTableViewer.refresh(true);
			}
		});

		column = new TableColumn(fTable, SWT.NULL);
		column.setText("Location");
		column.setWidth(defaultwidth);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// sortByLocation(); FIXME
				instanceListTableViewer.refresh(true);
			}
		});

		column = new TableColumn(fTable, SWT.NULL);
		column.setText("Status");
		column.setWidth(defaultwidth);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// sortByType(); FIXME
				instanceListTableViewer.refresh(true);
			}
		});

		instanceListTableViewer = new TableViewer(fTable);
		instanceListTableViewer.setLabelProvider(new HawkIndexLabelProvider());
		instanceListTableViewer.setContentProvider(new HawkIndexContentProvider());
		instanceListTableViewer.setUseHashlookup(true);
		// by default, sort by name
		// sortByName(); // FIXME

		instanceListTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent evt) {
				ISelection selection = evt.getSelection();
				HModel element = (HModel) ((IStructuredSelection)instanceListTableViewer.getSelection()).getFirstElement();
				if (!selection.isEmpty()) {
					toggleHawk(element);
					removeButton.setEnabled(true);
				} else {
					removeButton.setEnabled(false);
				}
			}			
		});

		
		instanceListTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent e) {
				if (!e.getSelection().isEmpty()) {
					HModel selection = (HModel) ((IStructuredSelection)instanceListTableViewer.getSelection()).getFirstElement();
					if (selection.isRunning()) {						
						configIndex();
					} else {
						selection.start(selection.getManager());
						instanceListTableViewer.refresh();
					}
					toggleHawk(selection);
				}
			}
		});
		fTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					if (removeButton.isEnabled()){
						removeIndex();
					}
				}
			}
		});

		Composite buttons = new Composite(parent, GridData.VERTICAL_ALIGN_BEGINNING);
		buttons.setFont(font);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttons.setLayout(layout);

		GridData horizontalFill = new GridData(SWT.FILL, SWT.FILL_WINDING, true, true);
		
		createButton = new Button(buttons, SWT.PUSH);
		createButton.setText("&Create...");
		createButton.setEnabled(true);
		createButton.setLayoutData(horizontalFill);
		createButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				createIndex();
			}
		});
		
		importButton = new Button(buttons, SWT.PUSH);
		importButton.setText("&Import...");
		importButton.setEnabled(true);
		importButton.setLayoutData(horizontalFill);
		importButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				importIndex();
			}
		});
		

		startButton = new Button(buttons, SWT.PUSH);
		startButton.setText("&Start");
		startButton.setEnabled(false);
		startButton.setLayoutData(horizontalFill);
		startButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				HModel element = (HModel) ((IStructuredSelection)instanceListTableViewer.getSelection()).getFirstElement();
				if (!element.isRunning()) {						
					element.start(element.getManager());
				}
				toggleRunning(true);
				instanceListTableViewer.refresh();
			}			
		});
		
		stopButton = new Button(buttons, SWT.PUSH);
		stopButton.setText("&Stop");
		stopButton.setEnabled(false);
		stopButton.setLayoutData(horizontalFill);
		stopButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				HModel element = (HModel) ((IStructuredSelection)instanceListTableViewer.getSelection()).getFirstElement();
				if (element.isRunning()) {						
					element.stop(ShutdownRequestType.ALWAYS);
				}
				toggleRunning(false);
				instanceListTableViewer.refresh();
			}
		});

		configButton = new Button(buttons, SWT.PUSH);
		configButton.setText("&Configure...");
		configButton.setEnabled(false);
		configButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				configIndex();
			}

		});

		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText("&Remove");
		removeButton.setEnabled(false);
		removeButton.setLayoutData(horizontalFill);
		removeButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				removeIndex();
			}
		});
		

	    /*Label separator = new Label(buttons, SWT.SEPARATOR | SWT.VERTICAL);
	    separator.setOrientation(1);*/

		
		updateIndexes();
		//JavaRuntime.getVMInstallTypes().length > 0); FIXME
		
		instanceListTableViewer.refresh();
	}
	
	private void toggleHawk(HModel element) {
		boolean running = element.isRunning();
		toggleRunning(running);
	}
	
	private void toggleRunning(boolean running){
		startButton.setEnabled(!running);
		configButton.setEnabled(running);
		stopButton.setEnabled(running);
	}
	
	public HModel[] getIndexes() {
		Set<HModel> hawks = HUIManager.getInstance().getHawks();
		return hawks.toArray(new HModel[hawks.size()]);
	}
	
	
	protected void updateIndexes() {
		// fill with JREs
		HModel[] array = getIndexes();
		instanceListTableViewer.setInput(array);
		instanceListTableViewer.refresh();
	}
	
	
	private void createIndex() {
		HWizard wizard = new HWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if (dialog.open() == Window.OK) {
		HModel result = wizard.getResult();
			if (result != null) {
				//refresh from model
				instanceListTableViewer.refresh();
				instanceListTableViewer.setSelection(new StructuredSelection(result));
				//ensure labels are updated
				instanceListTableViewer.refresh(true);
			}
		}
	}
	
	private void configIndex() {
		IStructuredSelection selection= (IStructuredSelection)instanceListTableViewer.getSelection();
		HModel firstElement = (HModel) selection.getFirstElement();
		if (firstElement == null) {
			return;
		}
		HConfigDialog dialog = new HConfigDialog(PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getShell(),
				firstElement);
		dialog.open();
		
	}
	
	private void removeIndex() {
		IStructuredSelection selected = (IStructuredSelection) instanceListTableViewer.getSelection();
		if (selected.size() >= 1) {
			HModel hawkmodel = (HModel) selected.getFirstElement();
			try {
				HUIManager.getInstance().delete(hawkmodel, hawkmodel.exists());
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
		}
		instanceListTableViewer.refresh();
	}

	private void importIndex() {
		final HImportDialog dialog = new HImportDialog(getShell());
		dialog.setBlockOnOpen(true);
		dialog.open();
		instanceListTableViewer.refresh();
	}
}
