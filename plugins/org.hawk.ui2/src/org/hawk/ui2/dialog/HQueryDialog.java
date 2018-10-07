/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - redo layout into FormLayout, expose more options
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.hawk.core.IStateListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.osgiserver.HModel;
import org.hawk.osgiserver.HModelSchedulingRule;
import org.hawk.ui2.Activator;
import org.osgi.framework.FrameworkUtil;

public class HQueryDialog extends TitleAreaDialog implements IStateListener {

	protected class SyncSelectionAdapter extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			try {
				index.sync();
			} catch (Exception ee) {
				Activator.logError("Failed to invoke manual sync", ee);
			}
		}
	}

	protected class QueryModifyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			String res = resultField.getText();
			if (!res.startsWith(QUERY_EDITED)) {
				resultField.setText(QUERY_EDITED + "\n" + res);
				resultField.setStyleRange(createRedBoldRange(QUERY_EDITED.length()));
			}
		}
	}

	protected class ResetButtonSelectionAdapter extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			queryField.setText("");
			queryField.setEditable(true);
			resultField.setText("");
			contextFiles.setText("");
		}
	}

	protected class QueryExecutionSelectionAdapter extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			if (currentQueryMonitor == null) {
				final String query = queryLanguage.getText().trim();
				if (!query.isEmpty()) {
					runQuery(query);
				}
			} else {
				currentQueryMonitor.setCanceled(true);
			}
		}

		private class CompletedQueryRunnable implements Runnable {
			private final Object result;
			private long startMillis;

			public CompletedQueryRunnable(long startMillis, Object result) {
				this.startMillis = startMillis;
				this.result = result;
			}

			@Override
			public void run() {
				boolean bCancelled = currentQueryMonitor.isCanceled();
				currentQueryMonitor = null;
				if (queryButton.isDisposed()) {
					return;
				}

				queryButton.setText("Run Query");
				getButton(IDialogConstants.OK_ID).setEnabled(true);

				final long endMillis = System.currentTimeMillis();
				final long elapsedMillis = endMillis - startMillis;
				if (bCancelled) {
					setMessage(String.format("Query cancelled after %d s %d ms",
							elapsedMillis / 1000, elapsedMillis % 1000));
				} else if (result instanceof Collection) {
					setMessage(String.format("Query returned %d results in %d s %d ms",
						((Collection<?>) result).size(), elapsedMillis / 1000, elapsedMillis % 1000), 0);
				} else {
					setMessage(String.format("Query completed in %d s %d ms",
						elapsedMillis / 1000, elapsedMillis % 1000), 0);
				}
			}
		}

		protected void runQuery(final String query) {
			final Map<String, Object> context = createContext();
			final String queryText = queryField.getText();
			resultField.setText("");
			setMessage("Running query...");

			Job runQueryJob = new Job("Running query in " + index.getName()) {
				private Runnable doCancel;

				@Override
				protected void canceling() {
					if (doCancel != null) {
						doCancel.run();
					}
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// If the user cancels the job, then the query engine cancel callback should be invoked
					context.put(IQueryEngine.PROPERTY_CANCEL_CONSUMER, (Consumer<Runnable>) (doCancel) -> {
						this.doCancel = doCancel;
					}); 
					Display.getDefault().syncExec(() -> {
						currentQueryMonitor = monitor;
						queryButton.setText("Stop Query");
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					});

					final long start = System.currentTimeMillis();
					Object result = null;
					try {
						if (queryText.startsWith(QUERY_IS_EDITOR)) {
							result = index.query(queryText.substring(QUERY_IS_EDITOR.length()), query, context);
						} else if (queryText.startsWith(QUERY_IS_FILE)) {
							result = index.query(new File(queryText.substring(QUERY_IS_FILE.length())), query, context);
						} else {
							result = index.query(queryText, query, context);
						}
						final Object endResult = result;
						Display.getDefault().syncExec(() -> {
							if (!resultField.isDisposed()) {
								resultField.setText(endResult != null ? endResult.toString()
									: "<null> returned (if this unexpected, check console for errors)");
							}
						});
					} catch (Exception ex) {
						final String error = "Error while running the query: " + ex.getMessage();
						Activator.logError(error, ex);
						Display.getDefault().syncExec(() -> {
							if (!resultField.isDisposed()) {
								resultField.setText(error);
								resultField.setStyleRange(createRedBoldRange(error.length()));
							}
						});
					} finally {
						Display.getDefault().syncExec(new CompletedQueryRunnable(start, result));
					}

					return new Status(IStatus.OK, getBundleName(), "Done");
				}

				private String getBundleName() {
					return FrameworkUtil.getBundle(getClass()).getSymbolicName();
				}
			};
			runQueryJob.setRule(new HModelSchedulingRule(index));
			runQueryJob.schedule();
		}

		protected Map<String, Object> createContext() {
			final String sRepo = contextRepo.getText().trim();
			final String sFiles = contextFiles.getText().trim();
			final String defaultNamespace = defaultNamespaces.getText().trim();
			final String sSubtree = subtreeText.getText().trim();
			final boolean bFileFirst = fileFirstButton.getSelection();
			final boolean bFullTraversalScoping = enableFullTraversalScopingButton.getSelection();
			final boolean bSubtreeDerived = useDerivedForSubtreeButton.getSelection();

			Map<String, Object> context = new HashMap<>();
			if (!sFiles.equals("")) { 
				context.put(IQueryEngine.PROPERTY_FILECONTEXT, sFiles);
			}
			if (!sRepo.equals("")) {
				context.put(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, sRepo);
			}
			if (!defaultNamespace.equals("")) {
				context.put(IQueryEngine.PROPERTY_DEFAULTNAMESPACES, defaultNamespace);
			}
			if (!sSubtree.equals("")) {
				context.put(IQueryEngine.PROPERTY_SUBTREECONTEXT, sSubtree);
			}
			context.put(IQueryEngine.PROPERTY_FILEFIRST, bFileFirst + "");
			context.put(IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING, bFullTraversalScoping + "");
			context.put(IQueryEngine.PROPERTY_SUBTREE_DERIVEDALLOF, bSubtreeDerived + "");

			return context;
		}
	}

	protected class QueryFileSelectionAdapter extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			String s = fileQueryBrowse();
			if (s != null) {
				queryField.setEditable(false);
				queryField.setText(QUERY_IS_FILE + s);
				queryField.setStyleRange(createBoldRange(QUERY_IS_FILE.length()));
			}
		}
	}

	protected class UseEditorSelectionAdapter extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			String s = "<ERROR: retrieving query from editor>";
			queryField.setEditable(false);
			IEditorPart part;
			part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor();

			if (part instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) part;
				s = (QUERY_IS_EDITOR + editor.getDocumentProvider()
						.getDocument(editor.getEditorInput()).get());
			} else {
				s = ("<ERROR: selected editor is not a Text editor>");
			}
			queryField.setText(s);
			if (s.startsWith(QUERY_IS_EDITOR)) {
				queryField.setStyleRange(createBoldRange(QUERY_IS_EDITOR.length()));
			} else {
				queryField.setStyleRange(createBoldRange(s.length()));
			}
		}
	}

	private static final String QUERY_IS_FILE = "FILE QUERY:\n";
	private static final String QUERY_IS_EDITOR = "EDITOR QUERY:\n";
	private static final String QUERY_EDITED = "[query has been edited since last results]";

	private HModel index;
	private IProgressMonitor currentQueryMonitor;

	private StyledText queryField, resultField, contextRepo, contextFiles, defaultNamespaces;
	private Button enableFullTraversalScopingButton;
	private Button queryButton;
	private Combo queryLanguage;
	private StyledText subtreeText;
	private Button useDerivedForSubtreeButton;
	private Button fileFirstButton;
	private TabFolder tabFolder;

	public HQueryDialog(Shell parentShell, HModel in) {
		super(parentShell);
		setShellStyle(getShellStyle() & ~SWT.CLOSE);
		index = in;
		index.getHawk().getModelIndexer().addStateListener(this);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button cancel = getButton(IDialogConstants.OK_ID);
		cancel.setText("Done");
		setButtonLayoutData(cancel);
	}

	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		if (id == IDialogConstants.CANCEL_ID)
			return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	protected Control createDialogArea(Composite parent) {
		super.createDialogArea(parent);
		setTitle("Query Hawk index");
		setMessage("Enter a query (either as text or as a file) and click [Run Query] to get a result.");

		final Composite container = new Composite(parent, SWT.NONE);
		final FormLayout formLayout = new FormLayout();
		container.setLayout(formLayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		tabFolder = new TabFolder(container, SWT.BORDER);
		createTab("Query", (cmp) -> {
			createQueryArea(cmp);
			createQueryLanguageSelector(cmp);
			return null;
		});
		createTab("Path-based scope", (cmp) -> {
			createContextRepository(cmp);
			createContextFiles(cmp);
			return null;
		});
		createTab("Tree-based scope", (cmp) -> {
			createSubtree(cmp);
			return null;
		});
		createTab("Namespaces", (cmp) -> {
			createDefaultNamespaces(cmp);
			return null;
		});
		createTab("Traversal", (cmp) -> {
			createFullTraversal(cmp);
			return null;
		});

		final FormData tabFolderFD = new FormData();
		tabFolderFD.left = new FormAttachment(0, 0);
		tabFolderFD.right = new FormAttachment(100, 0);
		tabFolder.setLayoutData(tabFolderFD);

		createButtons(container);

		return container;
	}

	private void createTab(final String tabText, final Function<Composite, Void> tabFiller) {
		final TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
		tabItem.setText(tabText);

		final Composite cmp = new Composite(tabFolder, SWT.NULL);
		final FormLayout layout = new FormLayout();
		layout.marginBottom = 20;
		cmp.setLayout(layout);

		tabFiller.apply(cmp);
		tabItem.setControl(cmp);
	}

	protected void createSubtree(Composite container) {
		Label lSubtree = new Label(container, SWT.WRAP | SWT.LEFT);
		lSubtree.setText("Subtree root context (for fragmented models - path within repository):");
		FormData lSubtreeFD = new FormData();
		lSubtreeFD.left = new FormAttachment(5, 0);
		lSubtreeFD.right = new FormAttachment(95, 0);
		lSubtreeFD.top = new FormAttachment(5, 0);
		lSubtree.setLayoutData(lSubtreeFD);

		subtreeText = new StyledText(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		FormData subtreeTextFD = new FormData();
		subtreeTextFD.left = new FormAttachment(5, 0);
		subtreeTextFD.right = new FormAttachment(95, 0);
		subtreeTextFD.height = 60;
		subtreeTextFD.top = new FormAttachment(lSubtree, 10);
		subtreeText.setLayoutData(subtreeTextFD);

		Label lSubtreeDerived = new Label(container, SWT.WRAP | SWT.LEFT);
		lSubtreeDerived.setText("Use derived edges to speed up Type.all in subtree queries:");
		FormData lSubtreeDerivedFD = new FormData();
		lSubtreeDerivedFD.left = new FormAttachment(5, 0);
		lSubtreeDerivedFD.top = new FormAttachment(subtreeText, 10);
		lSubtreeDerived.setLayoutData(lSubtreeDerivedFD);

		useDerivedForSubtreeButton = new Button(container, SWT.CHECK);
		FormData useDerivedFD = new FormData();
		useDerivedFD.left = new FormAttachment(lSubtreeDerived, 5);
		useDerivedFD.top = new FormAttachment(subtreeText, 10);
		useDerivedForSubtreeButton.setLayoutData(useDerivedFD);
	}

	protected void createButtons(final Composite container) {
		queryButton = new Button(container, SWT.PUSH);
		queryButton.setText("Run Query");
		FormData queryButtonFD = new FormData();
		queryButtonFD.left = new FormAttachment(6, 0);
		queryButtonFD.right = new FormAttachment(34, 0);
		queryButtonFD.top = new FormAttachment(tabFolder, 20);
		queryButton.setLayoutData(queryButtonFD);
		queryButton.addSelectionListener(new QueryExecutionSelectionAdapter());
		
		Button resetButton = new Button(container, SWT.PUSH);
		resetButton.setText("Reset Query");
		FormData resetButtonFD = new FormData();
		resetButtonFD.left = new FormAttachment(36, 0);
		resetButtonFD.right = new FormAttachment(64, 0);
		resetButtonFD.top = new FormAttachment(tabFolder, 20);
		resetButton.setLayoutData(resetButtonFD);
		resetButton.addSelectionListener(new ResetButtonSelectionAdapter());

		Button syncButton = new Button(container, SWT.PUSH);
		syncButton.setText("Request Immediate Sync");
		syncButton.setImage(ImageDescriptor.createFromURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path("icons/refresh.gif"), null)).createImage());
		FormData syncButtonFD = new FormData();
		syncButtonFD.left = new FormAttachment(66, 0);
		syncButtonFD.right = new FormAttachment(94, 0);
		syncButtonFD.top = new FormAttachment(tabFolder, 20);
		syncButton.setLayoutData(syncButtonFD);
		syncButton.addSelectionListener(new SyncSelectionAdapter());
	}

	protected void createDefaultNamespaces(final Composite container) {
		Label lDefaultNamespaces = new Label(container, SWT.READ_ONLY);
		lDefaultNamespaces.setText("Default Namespaces (comma separated):");
		FormData lDefaultNamespacesFD = new FormData();
		lDefaultNamespacesFD.left = new FormAttachment(5, 0);
		lDefaultNamespacesFD.right = new FormAttachment(95, 0);
		lDefaultNamespacesFD.top = new FormAttachment(5, 0);
		lDefaultNamespaces.setLayoutData(lDefaultNamespacesFD);

		defaultNamespaces = new StyledText(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		FormData defaultNamespacesFD = new FormData();
		defaultNamespacesFD.left = new FormAttachment(5, 0);
		defaultNamespacesFD.top = new FormAttachment(lDefaultNamespaces, 10);
		defaultNamespacesFD.right = new FormAttachment(95, 0);
		defaultNamespacesFD.height = 60;
		defaultNamespaces.setLayoutData(defaultNamespacesFD);
	}

	protected void createFullTraversal(final Composite container) {
		Label lFullTraversal = new Label(container, SWT.WRAP | SWT.LEFT);
		lFullTraversal.setText("Enable Full Traversal Scoping (may affect performance -- only for scoped queries):");
		FormData lFullTraversalFD = new FormData();
		lFullTraversalFD.left = new FormAttachment(5, 0);
		lFullTraversalFD.top = new FormAttachment(defaultNamespaces, 10);
		lFullTraversalFD.width = 700;
		lFullTraversal.setLayoutData(lFullTraversalFD);

		enableFullTraversalScopingButton = new Button(container, SWT.CHECK);
		FormData enableFTFD = new FormData();
		enableFTFD.left = new FormAttachment(lFullTraversal, 5);
		enableFTFD.top = new FormAttachment(defaultNamespaces, 10);
		enableFullTraversalScopingButton.setLayoutData(enableFTFD);
	}

	protected void createContextFiles(final Composite container) {
		Label lFiles = new Label(container, SWT.WRAP | SWT.LEFT);
		lFiles.setText("Context Files (comma separated (partial) matches using * as wildcard):");
		FormData lFilesFD = new FormData();
		lFilesFD.left = new FormAttachment(5, 0);
		lFilesFD.right = new FormAttachment(95, 0);
		lFilesFD.top = new FormAttachment(contextRepo, 10);
		lFiles.setLayoutData(lFilesFD);

		contextFiles = new StyledText(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		FormData contextFilesFD = new FormData();
		contextFilesFD.left = new FormAttachment(5, 0);
		contextFilesFD.right = new FormAttachment(95, 0);
		contextFilesFD.top = new FormAttachment(lFiles, 10);
		contextFilesFD.height = 60;
		contextFiles.setLayoutData(contextFilesFD);

		Label lFileFirst = new Label(container, SWT.WRAP);
		lFileFirst.setText("Start with files rather than types for Type.all (faster for small fragments in large graphs):");
		FormData lFileFirstFD = new FormData();
		lFileFirstFD.left = new FormAttachment(5, 0);
		lFileFirstFD.top = new FormAttachment(contextFiles, 10);
		lFileFirst.setLayoutData(lFileFirstFD);

		fileFirstButton = new Button(container, SWT.CHECK);
		FormData fileFirstButtonFD = new FormData();
		fileFirstButtonFD.left = new FormAttachment(lFileFirst, 10);
		fileFirstButtonFD.top = new FormAttachment(contextFiles, 10);
		fileFirstButton.setLayoutData(fileFirstButtonFD);
	}

	protected void createContextRepository(final Composite container) {
		Label lRepositories = new Label(container, SWT.WRAP | SWT.LEFT);
		lRepositories.setText("Context Repositories (comma separated (partial) matches using * as wildcard):");
		FormData lRepositoriesFD = new FormData();
		lRepositoriesFD.left = new FormAttachment(5, 0);
		lRepositoriesFD.top = new FormAttachment(5, 0);
		lRepositoriesFD.right = new FormAttachment(95, 0);
		lRepositories.setLayoutData(lRepositoriesFD);

		contextRepo = new StyledText(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		FormData contextRepoFD = new FormData();
		contextRepoFD.left = new FormAttachment(5, 0);
		contextRepoFD.right = new FormAttachment(95, 0);
		contextRepoFD.top = new FormAttachment(lRepositories, 10);
		contextRepoFD.height = 60;
		contextRepo.setLayoutData(contextRepoFD);
	}

	protected void createQueryLanguageSelector(final Composite container) {
		Label lQueryEngine = new Label(container, SWT.READ_ONLY);
		lQueryEngine.setText("Query Engine:");
		FormData lQueryEngineFD = new FormData();
		lQueryEngineFD.left = new FormAttachment(5, 0);
		lQueryEngineFD.top = new FormAttachment(resultField, 14);
		lQueryEngine.setLayoutData(lQueryEngineFD);

		queryLanguage = new Combo(container, SWT.READ_ONLY);
		for (String s : index.getKnownQueryLanguages()) {
			queryLanguage.add(s);
		}
		if (queryLanguage.getItems().length > 0) {
			queryLanguage.select(0);
		}
		FormData queryLanguageFD = new FormData();
		queryLanguageFD.left = new FormAttachment(lQueryEngine, 10);
		queryLanguageFD.right = new FormAttachment(95, 0);
		queryLanguageFD.top = new FormAttachment(resultField, 10);
		
		queryLanguage.setLayoutData(queryLanguageFD);
	}

	protected void createQueryArea(final Composite container) {
		final Label qLabel = new Label(container, SWT.NONE);
		qLabel.setText("Query:");
		FormData qLabelFD = new FormData();
		qLabelFD.top = new FormAttachment(5, 0);
		qLabelFD.left = new FormAttachment(5, 0);
		qLabel.setLayoutData(qLabelFD);

		queryField = new StyledText(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		FormData queryFieldFD = new FormData();
		queryFieldFD.top = new FormAttachment(qLabel, 10);
		queryFieldFD.left = new FormAttachment(5, 0);
		queryFieldFD.right = new FormAttachment(95, 0);
		queryFieldFD.height = 100;
		queryField.setLayoutData(queryFieldFD);
		queryField.addModifyListener(new QueryModifyListener());

		final Button editor = new Button(container, SWT.PUSH);
		editor.setText("Query Current Editor");
		editor.addSelectionListener(new UseEditorSelectionAdapter());
		FormData editorFD = new FormData();
		editorFD.bottom = new FormAttachment(queryField, -10);
		editorFD.right = new FormAttachment(95, 0);
		editor.setLayoutData(editorFD);

		final Button file = new Button(container, SWT.PUSH);
		file.setText("Query File");
		file.addSelectionListener(new QueryFileSelectionAdapter());
		FormData fileFD = new FormData();
		fileFD.bottom = new FormAttachment(queryField, -10);
		fileFD.right = new FormAttachment(editor, -5);
		file.setLayoutData(fileFD);

		final Label rLabel = new Label(container, SWT.NONE);
		rLabel.setText("Result:");
		FormData rLabelFD = new FormData();
		rLabelFD.left = new FormAttachment(5, 0);
		rLabelFD.top = new FormAttachment(queryField, 10);
		rLabel.setLayoutData(rLabelFD);

		resultField = new StyledText(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		resultField.setEditable(false);
		FormData resultFieldFD = new FormData();
		resultFieldFD.left = new FormAttachment(5, 0);
		resultFieldFD.top = new FormAttachment(rLabel, 10);
		resultFieldFD.right = new FormAttachment(95, 0);
		resultFieldFD.height = queryFieldFD.height;
		resultField.setLayoutData(resultFieldFD);
	}

	private String fileQueryBrowse() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final File rootFile = root.getLocation().toFile();
		fd.setFilterPath(rootFile.toString());
		// fd.setFilterExtensions(new String [] {"*.ecore"});
		fd.setText("Select a file to query");

		return fd.open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Query: " + index.getName());
	}

	private StyleRange createRedBoldRange(final int length) {
		Display display = getShell().getDisplay();
		StyleRange styleRange = new StyleRange();
		styleRange.start = 0;
		styleRange.length = length;
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = new Color(display, 255, 0, 0);
		return styleRange;
	}

	private StyleRange createBoldRange(int length) {
		Display display = getShell().getDisplay();
		StyleRange styleRange = new StyleRange();
		styleRange.start = 0;
		styleRange.length = length;
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = new Color(display, 0, 0, 0);
		return styleRange;
	}

	@Override
	public boolean close() {
		index.getHawk().getModelIndexer().removeStateListener(this);
		return super.close();
	}

	@Override
	public void state(HawkState state) {
		switch (state) {
		case STOPPED:
			updateAsync(HawkState.STOPPED);
			break;
		case RUNNING:
			updateAsync(HawkState.RUNNING);
			break;
		case UPDATING:
			updateAsync(HawkState.UPDATING);
			break;
		}
	}

	public void updateAsync(final HawkState s) {
		Shell shell = getShell();
		if (shell != null) {
			Display display = shell.getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							if (queryButton != null) {
								boolean enable = s == HawkState.RUNNING;
								queryButton.setEnabled(enable);

								if (enable) {
									setErrorMessage(null);
								} else {
									setErrorMessage(String
											.format("The index is %s - querying will be disabled",
													s.toString().toLowerCase()));
								}
							}
						} catch (Exception e) {
							Activator.logError(e.getMessage(), e);
						}
					}
				});
			}
		}
	}

	@Override
	public void info(String s) {
		// not used in query dialogs
	}

	@Override
	public void error(String s) {
		// not used in query dialogs
	}

	@Override
	public void removed() {
		// used for remote message cases
	}
}
