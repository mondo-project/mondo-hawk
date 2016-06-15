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
 ******************************************************************************/
package org.hawk.ui2.dialog;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.hawk.core.IStateListener;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.Activator;
import org.osgi.framework.FrameworkUtil;

public class HQueryDialog extends TitleAreaDialog implements IStateListener {

	private static final String QUERY_IS_FILE = "FILE QUERY:\n";
	private static final String QUERY_IS_EDITOR = "EDITOR QUERY:\n";
	private static final String QUERY_EDITED = "[query has been edited since last results]";

	private StyledText queryField;
	private StyledText resultField;
	Button enableFullTraversalScopingButton;

	private HModel index;

	private Button queryButton;

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

		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);

		setTitle("Query Hawk index");
		setMessage("Enter a query (either as text or as a file) and click [Run Query] to get a result.");

		final Label qLabel = new Label(container, SWT.NONE);
		qLabel.setText("Query:");

		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayout(gridLayout);

		buttons.setLayoutData(new GridData(GridData.END, GridData.END, true,
				true, 1, 1));

		final Button editor = new Button(buttons, SWT.PUSH);
		editor.setLayoutData(new GridData(GridData.END, GridData.END, true,
				true, 1, 1));
		editor.setText("Query Current Editor");

		editor.addSelectionListener(new SelectionAdapter() {
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
				if (s.startsWith(QUERY_IS_EDITOR))
					queryField.setStyleRange(createBoldRange(QUERY_IS_EDITOR
							.length()));
				else
					queryField.setStyleRange(createBoldRange(s.length()));
			}
		});

		final Button file = new Button(buttons, SWT.PUSH);
		file.setLayoutData(new GridData(GridData.END, GridData.END, true, true,
				1, 1));
		file.setText("Query File");

		queryField = new StyledText(container, SWT.MULTI | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		GridData gridDataQ = new GridData();
		gridDataQ.grabExcessHorizontalSpace = true;
		gridDataQ.horizontalAlignment = GridData.FILL_BOTH;
		gridDataQ.minimumWidth = 600;
		gridDataQ.minimumHeight = 300;
		gridDataQ.heightHint = 100;
		gridDataQ.horizontalSpan = 2;
		queryField.setLayoutData(gridDataQ);

		file.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String s = filequeryBrowse();
				if (s != null) {
					queryField.setEditable(false);
					queryField.setText(QUERY_IS_FILE + s);
					queryField.setStyleRange(createBoldRange(QUERY_IS_FILE
							.length()));
				}
			}

		});

		final Label rLabel = new Label(container, SWT.NONE);
		rLabel.setText("Result:");

		final Label dummy2 = new Label(container, SWT.NONE);
		dummy2.setText("");

		resultField = new StyledText(container, SWT.MULTI | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		GridData gridDataR = new GridData();
		gridDataR.grabExcessHorizontalSpace = true;
		gridDataR.horizontalAlignment = GridData.FILL_BOTH;
		gridDataR.minimumWidth = 600;
		gridDataR.minimumHeight = 300;
		gridDataR.heightHint = 100;
		gridDataR.horizontalSpan = 2;
		resultField.setLayoutData(gridDataR);
		resultField.setEditable(false);

		Label l = new Label(container, SWT.READ_ONLY);
		l.setText(" Query Engine:");

		final Combo queryLanguage = new Combo(container, SWT.READ_ONLY);
		for (String s : index.getKnownQueryLanguages())
			queryLanguage.add(s);

		if (queryLanguage.getItems().length > 0)
			queryLanguage.select(0);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" Context Repositories (comma separated");

		l = new Label(container, SWT.READ_ONLY);

		l = new Label(container, SWT.READ_ONLY);
		l.setText("   (partial) matches using * as wildcard):");

		final StyledText contextRepo = new StyledText(container, SWT.NONE);
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 250;
		contextRepo.setLayoutData(gridData);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" Context Files (comma separated (partial)");

		l = new Label(container, SWT.READ_ONLY);

		l = new Label(container, SWT.READ_ONLY);
		l.setText("   matches using * as wildcard):");

		final StyledText contextFiles = new StyledText(container, SWT.NONE);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 250;
		contextFiles.setLayoutData(gridData);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" Enable Full Traversal Scoping (may affect");

		l = new Label(container, SWT.READ_ONLY);

		l = new Label(container, SWT.READ_ONLY);
		l.setText("   performance -- only for scoped queries):");

		enableFullTraversalScopingButton = new Button(container, SWT.CHECK);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" Default Namespaces (comma separated)");

		final StyledText defaultNamespaces = new StyledText(container, SWT.NONE);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 250;
		defaultNamespaces.setLayoutData(gridData);

		queryButton = new Button(container, SWT.PUSH);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		gridData.minimumWidth = 555;
		queryButton.setLayoutData(gridData);
		queryButton.setText("Run Query");

		// return TypeDeclaration.all.size();

		// l = new Label(container, SWT.READ_ONLY);

		queryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				long start = System.currentTimeMillis();
				
				Object result = null;
				try {
					String ql = null;
					String text = queryLanguage.getText();
					ql = text.equals("") ? null : text;
					if (ql != null) {

						final String sRepo = contextRepo.getText();
						final String sFiles = contextFiles.getText();
						final String defaultNamespace = defaultNamespaces
								.getText();

						Map<String, String> map = new HashMap<>();
						if (sFiles != null && !sFiles.trim().equals(""))
							map.put(org.hawk.core.query.IQueryEngine.PROPERTY_FILECONTEXT,
									sFiles);
						if (sRepo != null && !sRepo.trim().equals(""))
							map.put(org.hawk.core.query.IQueryEngine.PROPERTY_REPOSITORYCONTEXT,
									sRepo);
						if (defaultNamespace != null
								&& !defaultNamespace.trim().equals(""))
							map.put(org.hawk.core.query.IQueryEngine.PROPERTY_DEFAULTNAMESPACES,
									defaultNamespace);
						map.put(org.hawk.core.query.IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING,
								new Boolean(enableFullTraversalScopingButton
										.getSelection()).toString());
						if (map.size() == 0)
							map = null;

						if (queryField.getText().startsWith(QUERY_IS_EDITOR)) {

							result = index.query(queryField.getText()
									.substring(QUERY_IS_EDITOR.length()), ql,
									map);
							String ret = "<null>";
							if (result != null)
								ret = result.toString();
							resultField.setText(ret);

						} else if (queryField.getText().startsWith(QUERY_IS_FILE)) {

							result = index.query(new File(queryField
									.getText()
									.substring(QUERY_IS_FILE.length())), ql,
									map);
							String ret = "<null>";
							if (result != null)
								ret = result.toString();
							resultField.setText(ret);

						} else {
							result = index.query(queryField.getText(), ql, map);
							resultField.setText(result != null ? result.toString() : "<null>");
						}

					}
				} catch (Exception ex) {
					final String error = "Error while running the query: "
							+ ex.getMessage();
					resultField.setText(error);
					resultField.setStyleRange(createRedBoldRange(error.length()));
					ex.printStackTrace();
				}
				
				long end = System.currentTimeMillis();
				long time = end-start;

				if (result instanceof Collection) {
					setMessage(String.format("Query returned %d results in %d s %d ms", ((Collection)result).size(), time/1000,  time % 1000), 0);
				} else {
					setMessage(String.format("Query completed in %d s %d ms", time/1000,  time % 1000), 0);
				}
				
			}
		});

		Button button2 = new Button(container, SWT.PUSH);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		gridData.minimumWidth = 252;
		button2.setLayoutData(gridData);
		button2.setText("Reset Query");

		button2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				queryField.setText("");
				queryField.setEditable(true);
				resultField.setText("");
				contextFiles.setText("");
			}
		});

		queryField.setText("");
		queryField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String res = resultField.getText();
				if (!res.startsWith(QUERY_EDITED)) {
					resultField.setText(QUERY_EDITED + "\n" + res);
					resultField.setStyleRange(createRedBoldRange(QUERY_EDITED
							.length()));
				}
			}

		});

		// l = new Label(container, SWT.READ_ONLY);

		Button button3 = new Button(container, SWT.PUSH);
		button3.setText("Request Immediate Sync");
		button3.setImage(ImageDescriptor.createFromURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path("icons/refresh.gif"), null)).createImage());
		button3.setLayoutData(gridData);
		button3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					index.sync();
				} catch (Exception ee) {
					Activator.logError("Failed to invoke manual sync", ee);
				}
			}
		});

		return container;
	}

	private String filequeryBrowse() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

		fd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile().toString());
		// fd.setFilterExtensions(new String [] {"*.ecore"});
		fd.setText("Select a file to query");
		return fd.open();

	}

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
