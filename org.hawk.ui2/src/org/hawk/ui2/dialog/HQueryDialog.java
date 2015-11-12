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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.Activator;
import org.osgi.framework.FrameworkUtil;

public class HQueryDialog extends Dialog {

	private static final String QUERY_IS_FILE = "FILE QUERY: ";
	private static final String QUERY_EDITED = "[query has been edited since last results]";

	private StyledText queryField;
	private StyledText resultField;

	private HModel index;

	public HQueryDialog(Shell parentShell, HModel in) {
		super(parentShell);
		setShellStyle(getShellStyle() & ~SWT.CLOSE);
		index = in;
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
		Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);

		final Label instructionsLabel = new Label(container, SWT.NONE);
		instructionsLabel.setLayoutData(new GridData(GridData.BEGINNING,
				GridData.CENTER, false, false, 2, 1));
		instructionsLabel
				.setText("Enter a query (either as text or as a file) and click [Run Query] to get a result.");

		final Label qLabel = new Label(container, SWT.NONE);
		qLabel.setText("Query:");

		final Button file = new Button(container, SWT.PUSH);
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
		l.setText("(partial) matches using * as wildcard):");

		final StyledText contextRepo = new StyledText(container, SWT.NONE);
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 250;
		contextRepo.setLayoutData(gridData);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" Context Files (comma separated (partial)");

		l = new Label(container, SWT.READ_ONLY);

		l = new Label(container, SWT.READ_ONLY);
		l.setText(" matches using * as wildcard):");

		final StyledText contextFiles = new StyledText(container, SWT.NONE);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 250;
		contextFiles.setLayoutData(gridData);

		Button button = new Button(container, SWT.PUSH);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		gridData.minimumWidth = 555;
		button.setLayoutData(gridData);
		button.setText("Run Query");

		// return TypeDeclaration.all.size();

		// l = new Label(container, SWT.READ_ONLY);

		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				try {

					String ql = null;
					String text = queryLanguage.getText();
					ql = text.equals("") ? null : text;
					if (ql != null) {

						final String sRepo = contextRepo.getText();
						final String sFiles = contextFiles.getText();

						if (queryField.getText().startsWith(QUERY_IS_FILE)) {
							if (sRepo.trim().equals("")
									&& sFiles.trim().equals("")) {
								Object r = index.query(new File(queryField
										.getText().substring(12)), ql);
								String ret = "<null>";
								if (r != null)
									ret = r.toString();
								resultField.setText(ret);
							} else {
								Map<String, String> map = new HashMap<>();

								map.put(org.hawk.core.query.IQueryEngine.PROPERTY_FILECONTEXT,
										sFiles);
								map.put(org.hawk.core.query.IQueryEngine.PROPERTY_REPOSITORYCONTEXT,
										sRepo);
								Object r = index.contextFullQuery(new File(
										queryField.getText().substring(12)),
										ql, map);
								String ret = "<null>";
								if (r != null)
									ret = r.toString();
								resultField.setText(ret);
							}
						} else {
							if (sRepo.trim().equals("")
									&& sFiles.trim().equals("")) {
								Object ret = index.query(queryField.getText(),
										ql);
								resultField.setText(ret != null ? ret
										.toString() : "<null>");
							} else {
								Map<String, String> map = new HashMap<>();
								map.put(org.hawk.core.query.IQueryEngine.PROPERTY_FILECONTEXT,
										sFiles);
								map.put(org.hawk.core.query.IQueryEngine.PROPERTY_REPOSITORYCONTEXT,
										sRepo);
								Object r = index.contextFullQuery(
										queryField.getText(), ql, map);
								String ret = "<null>";
								if (r != null)
									ret = r.toString();
								resultField.setText(ret);
							}

						}

					}
				} catch (Exception ex) {
					final String error = "Error while running the query: "
							+ ex.getMessage();
					resultField.setText(error);
					resultField.setStyleRange(createRedBoldRange(error.length()));
				}
			}
		});

		Button button2 = new Button(container, SWT.PUSH);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 1;
		gridData.minimumWidth = 250;
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
}