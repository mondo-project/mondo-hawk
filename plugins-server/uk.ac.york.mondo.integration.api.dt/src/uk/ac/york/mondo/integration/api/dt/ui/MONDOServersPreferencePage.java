/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.api.dt.ui;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import uk.ac.york.mondo.integration.api.dt.Activator;
import uk.ac.york.mondo.integration.api.dt.prefs.CredentialsStore;
import uk.ac.york.mondo.integration.api.dt.prefs.CredentialsStore.Credentials;
import uk.ac.york.mondo.integration.api.dt.prefs.Server;
import uk.ac.york.mondo.integration.api.dt.prefs.ServerStore;

public class MONDOServersPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final class ServerLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Server) {
				return ((Server)element).getBaseURL();
			}
			return super.getText(element);
		}
	}

	private static final class ListContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			List<?> list = (List<?>) inputElement;
			return list.toArray();
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	private ServerStore serverStore;
	private List<Server> servers;
	private CredentialsStore credsStore;

	public MONDOServersPreferencePage() {
		// nothing to do
	}

	public MONDOServersPreferencePage(String title) {
		super(title);
	}

	public MONDOServersPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		final IPreferenceStore preferencesStore = Activator.getDefault().getPreferenceStore();
		this.serverStore = new ServerStore(preferencesStore);
		this.credsStore = Activator.getDefault().getCredentialsStore();
		this.noDefaultButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		servers = serverStore.readAllServers();

		final ListViewer lServerURLs = new ListViewer(composite);
		lServerURLs.setLabelProvider(new ServerLabelProvider());
		lServerURLs.setContentProvider(new ListContentProvider());
		lServerURLs.setInput(servers);
		lServerURLs.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite cButtons = new Composite(composite, SWT.NONE);
		cButtons.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		cButtons.setLayout(new FillLayout(SWT.HORIZONTAL));

		final Button btnAdd = new Button(cButtons, SWT.NULL);
		btnAdd.setText("Add...");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				final ServerConfigurationDialog scDialog = new ServerConfigurationDialog(shell, "Add server", "http://localhost:8080/");
				if (scDialog.open() == Dialog.OK) {
					final Server newServer = new Server(scDialog.getLocation());
					try {
						credsStore.put(scDialog.getLocation(), scDialog.getCredentials());
						servers.add(newServer);
						lServerURLs.setInput(servers);
					} catch (Exception ex) {
						Activator.getDefault().logError(ex);
					}
				}
			}
		});

		final Button btnRemove = new Button(cButtons, SWT.NULL);
		btnRemove.setText("Remove");
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final ISelection selection = lServerURLs.getSelection();
				if (selection instanceof IStructuredSelection) {
					final IStructuredSelection ssel = (IStructuredSelection)lServerURLs.getSelection();
					final Server server = (Server) ssel.getFirstElement();
					try {
						credsStore.remove(server.getBaseURL());
						servers.remove(server);
						lServerURLs.setInput(servers);
					} catch (Exception ex) {
						Activator.getDefault().logError(ex);
					}
				}
			}
		});

		final Button btnEdit = new Button(cButtons, SWT.NULL);
		btnEdit.setText("Edit...");
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final ISelection selection = lServerURLs.getSelection();
				if (selection instanceof IStructuredSelection) {
					final IStructuredSelection ssel = (IStructuredSelection)lServerURLs.getSelection();
					final Server server = (Server) ssel.getFirstElement();

					final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					final ServerConfigurationDialog scDialog = new ServerConfigurationDialog(shell, "Add server", "http://localhost:8080/");
					Credentials creds;
					try {
						creds = credsStore.get(server.getBaseURL());
						if (creds != null) {
							scDialog.setCredentials(creds);
						}
					} catch (Exception ex) {
						Activator.getDefault().logError(ex);
					}

					if (scDialog.open() == Dialog.OK) {
						try {
							credsStore.put(server.getBaseURL(), scDialog.getCredentials());
						} catch (Exception ex) {
							Activator.getDefault().logError(ex);
						}
					}
				}
			}
		});

		Dialog.applyDialogFont(composite);
		return composite;
	}

	@Override
	public boolean performOk() {
		try {
			credsStore.flush();
			serverStore.saveAllServers(servers);
			return super.performOk();
		} catch (Exception e) {
			Activator.getDefault().logError(e);
			return false;
		}
	}
}
