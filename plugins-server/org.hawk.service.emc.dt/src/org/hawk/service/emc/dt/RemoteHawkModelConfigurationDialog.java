/*******************************************************************************
 * Copyright (c) 2008-2015 University of York.
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
 *    Dimitrios Kolovos - initial API and implementation (EmfModelConfigurationDialog)
 *    Antonio García-Domínguez - allow loading more than one metamodel and remove findEPackages
 *******************************************************************************/
package org.hawk.service.emc.dt;

import static org.eclipse.epsilon.common.dt.util.DialogUtil.createGroupContainer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.epsilon.common.dt.launching.dialogs.AbstractCachedModelConfigurationDialog;
import org.eclipse.epsilon.common.dt.launching.dialogs.BrowseWorkspaceUtil;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.EmfUtil;
import org.eclipse.epsilon.emf.dt.BrowseEPackagesListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

/**
 * Small variation over the Epsilon EmfModelConfigurationDialog that disables
 * finding EPackages automatically (since it'd require browsing through the
 * entire model) and limits file selection to <code>.hawkmodel</code> files.
 *
 * TODO: refactor EmfModelConfigurationDialog to reuse more code directly.
 */
public class RemoteHawkModelConfigurationDialog extends AbstractCachedModelConfigurationDialog {

	@Override
	protected String getModelName() {
		return "Remote Hawk";
	}

	@Override
	protected String getModelType() {
		return "hawkmodel";
	}

	private static final class BrowseWorkspaceForDescriptorsListener extends SelectionAdapter {

		private final Text text;

		public BrowseWorkspaceForDescriptorsListener(Text text) {
			this.text = text;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			final ResourceListSelectionDialog elementSelector = new ResourceListSelectionDialog(shell, ResourcesPlugin.getWorkspace().getRoot(), IResource.DEPTH_INFINITE | IResource.FILE ) {
				@Override
				protected boolean select(IResource resource) {
					// TODO Auto-generated method stub
					return resource.getName().toLowerCase().endsWith(".hawkmodel");
				}
			};
			elementSelector.setTitle("Remote Hawk access descriptors in the workspace");
			elementSelector.setMessage("Select a remote Hawk access descriptor");
			elementSelector.open();
			
			if (elementSelector.getReturnCode() == Window.OK){
				IFile f = (IFile) elementSelector.getResult()[0];
				text.setText(f.getFullPath().toString());
			}
		}
	}

	private static final class MetamodelListLabelProvider implements ILabelProvider {
		private Image imgFile, imgURI;

		public MetamodelListLabelProvider() {
			imgFile = Activator.getDefault().getImageDescriptor("icons/ecorefile.gif").createImage();
			imgURI = Activator.getDefault().getImageDescriptor("icons/epackage.png").createImage();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			// we don't need any listeners (cells are only added or removed, not edited)
		}

		@Override
		public void dispose() {
			imgFile.dispose();
			imgURI.dispose();
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// we don't need any listeners (cells are only added or removed, not edited)
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof String) {
				return imgFile;
			}
			else if (element instanceof URI) {
				return imgURI;
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			return element + "";
		}
	}

	protected Button expandButton;
	private Text modelFileText;
	private TableViewer metamodelList;

	// May contain metamodel URIs (as URI objects) or String (file-based metamodel paths) objects
	private List<Object> metamodels = new ArrayList<Object>();

	private Button reuseUnmodifiedFileBasedMetamodelsButton;
	
	@Override
	protected void createGroups(Composite control) {
		super.createGroups(control);
		createEmfGroup(control);
		createFilesGroup(control);
		//createLoadStoreOptionsGroup(control);
	}
	
	@Override
	protected void loadProperties(){
		super.loadProperties();
		if (properties == null) return;

		metamodels.clear();

		// Restore values from legacy launch configuration
		modelFileText.setText(properties.getProperty(EmfModel.PROPERTY_MODEL_FILE));
		final String sLegacyFileMetamodels = properties.getProperty(EmfModel.PROPERTY_METAMODEL_FILE);
		for (String sPath : sLegacyFileMetamodels.trim().split("\\s*,\\s*")) {
			if (sPath.length() > 0) {
				metamodels.add(sPath);
			}
		}

		// Restore values that are used directly to construct an instance of EmfModel
		final String sURIMetamodels = properties.getProperty(EmfModel.PROPERTY_METAMODEL_URI);
		for (String sURI : sURIMetamodels.trim().split("\\s*,\\s*")) {
			if (sURI.length() > 0) {
				metamodels.add(URI.createURI(sURI));
			}
		}
		expandButton.setSelection(new Boolean(properties.getProperty(EmfModel.PROPERTY_EXPAND)).booleanValue());

		metamodelList.refresh();
	}
	
	@Override
	protected void storeProperties(){
		super.storeProperties();

		/*
		 * Compute comma-separated lists with the file paths and URIs. If we
		 * only have one metamodel (either file- or URI-based), it should be
		 * compatible with previous versions of Epsilon.
		 */
		final StringBuilder sbFileMetamodels = new StringBuilder();
		final StringBuilder sbFileMetamodelURIs = new StringBuilder();
		final StringBuilder sbURIMetamodels = new StringBuilder();
		boolean bFirstFileMetamodel = true, bFirstURIMetamodel = true;
		for (Object o : metamodels) {
			if (o instanceof String) {
				if (!bFirstFileMetamodel) {
					sbFileMetamodelURIs.append(',');
					sbFileMetamodels.append(',');
				}
				else {
					bFirstFileMetamodel = false;
				}
				sbFileMetamodels.append((String)o);
				sbFileMetamodelURIs.append(createFullyQualifiedUri((String)o));
			}
			else if (o instanceof URI) {
				if (!bFirstURIMetamodel) {
					sbURIMetamodels.append(',');
				}
				else {
					bFirstURIMetamodel = false;
				}
				sbURIMetamodels.append(o.toString());
			}
		}
		properties.put(EmfModel.PROPERTY_MODEL_FILE,     modelFileText.getText());
		properties.put(EmfModel.PROPERTY_METAMODEL_FILE, sbFileMetamodels.toString());

		// Persist values that are used directly to construct an instance of EmfModel (legacy - only one metamodel was supported)
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, sbURIMetamodels.toString());
		properties.put(EmfModel.PROPERTY_EXPAND, expandButton.getSelection() + "");
		properties.put(EmfModel.PROPERTY_IS_METAMODEL_FILE_BASED, "".equals(sbURIMetamodels.toString()));

		// Create and persist URI values that are needed to construct an instance of EmfModel
		properties.put(EmfModel.PROPERTY_MODEL_URI, createFullyQualifiedUri(modelFileText.getText()));
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, sbFileMetamodelURIs.toString());
		properties.put(EmfModel.PROPERTY_REUSE_UNMODIFIED_FILE_BASED_METAMODELS, reuseUnmodifiedFileBasedMetamodelsButton.getSelection() + "");
	}

	protected void createEmfGroup(Composite parent) {
		final Composite groupContent = createGroupContainer(parent, "EMF", 3);
	
		expandButton = new Button(groupContent, SWT.CHECK);
		expandButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		expandButton.setText("Include external references");
		expandButton.setSelection(true);
	
		GridData expandButtonData = new GridData();
		expandButtonData.horizontalSpan = 2;
		expandButton.setLayoutData(expandButtonData);

		reuseUnmodifiedFileBasedMetamodelsButton = new Button(groupContent, SWT.CHECK);
		reuseUnmodifiedFileBasedMetamodelsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		reuseUnmodifiedFileBasedMetamodelsButton.setText("Reuse unmodified file-based metamodels");
		reuseUnmodifiedFileBasedMetamodelsButton.setSelection(true);

		GridData reuseUnmodifiedFileBasedMetamodelsButtonData = new GridData();
		reuseUnmodifiedFileBasedMetamodelsButtonData.horizontalSpan = 2;
		reuseUnmodifiedFileBasedMetamodelsButton.setLayoutData(reuseUnmodifiedFileBasedMetamodelsButtonData);
		
		groupContent.layout();
		groupContent.pack();
	}

	protected Composite createFilesGroup(Composite parent) {
		final Composite groupContent = createGroupContainer(parent, "Files/URIs", 3);
		((GridData)groupContent.getParent().getLayoutData()).grabExcessVerticalSpace = true;

		final Label modelFileLabel = new Label(groupContent, SWT.NONE);
		modelFileLabel.setText("Model file: ");
		
		modelFileText = new Text(groupContent, SWT.BORDER);
		modelFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Button browseModelFile = new Button(groupContent, SWT.NONE);
		browseModelFile.setText("Browse Workspace...");
		browseModelFile.addSelectionListener(new BrowseWorkspaceForDescriptorsListener(modelFileText));

		final Label metamodelListLabel = new Label(groupContent, SWT.LEFT | SWT.TOP);
		final GridData metamodelListLabelLayout = new GridData(SWT.LEFT, SWT.TOP, false, false);
		metamodelListLabelLayout.verticalIndent = 4;
		metamodelListLabel.setLayoutData(metamodelListLabelLayout);
		metamodelListLabel.setText("Metamodels:");

		metamodelList = new TableViewer(groupContent);
		metamodelList.setContentProvider(ArrayContentProvider.getInstance());
		metamodelList.setInput(metamodels);
		metamodelList.setLabelProvider(new MetamodelListLabelProvider());
		GridData metamodelListLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		metamodelList.getControl().setLayoutData(metamodelListLayout);

		final Composite metamodelButtons = new Composite(groupContent, SWT.NONE);
		final GridData metamodelButtonsLayout = new GridData();
		metamodelButtonsLayout.horizontalAlignment = SWT.FILL;
		metamodelButtons.setLayoutData(metamodelButtonsLayout);
		metamodelButtons.setLayout(new FillLayout(SWT.VERTICAL));
		final Button addFileMetamodelButton = new Button(metamodelButtons, SWT.NONE);
		addFileMetamodelButton.setText("Add file...");
		addFileMetamodelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String path = BrowseWorkspaceUtil.browseFilePath(getShell(),
						"EMF meta-models in the workspace",
						"Select an EMF meta-model (ECore)",
						"ecore", null);
				if (path != null && !metamodels.contains(path)) {
					metamodels.add(path);
					metamodelList.refresh();
				}
			}
		});

		final Button addURIMetamodelButton = new Button(metamodelButtons, SWT.NONE);
		addURIMetamodelButton.setText("Add URI...");
		addURIMetamodelButton.addListener(SWT.Selection, new BrowseEPackagesListener() {
			@Override
			public void selectionChanged(String ePackageUri) {
				URI uri = URI.createURI(ePackageUri);
				if (!metamodels.contains(uri)) {
					metamodels.add(uri);
					metamodelList.refresh();
				}
			}
		});

		final Button removeMetamodelButton = new Button(metamodelButtons, SWT.NONE);
		removeMetamodelButton.setText("Remove");
		removeMetamodelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (metamodelList.getSelection() instanceof IStructuredSelection) {
					final IStructuredSelection sel = (IStructuredSelection)metamodelList.getSelection();
					for (Iterator<?> it = sel.iterator(); it.hasNext(); ) {
						metamodels.remove(it.next());
					}
					metamodelList.refresh();
				}
			}
		});

		final Button clearAllMetamodelsButton = new Button(metamodelButtons, SWT.NONE);
		clearAllMetamodelsButton.setText("Clear");
		clearAllMetamodelsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				metamodels.clear();
				metamodelList.refresh();
			}
		});

		groupContent.layout();
		groupContent.pack();
		return groupContent;
	}

	private String createFullyQualifiedUri(String relativePath) {
		if (relativePath == null || relativePath.isEmpty())
			return "";
		else
			return EmfUtil.createPlatformResourceURI(relativePath).toString();
	}
}
