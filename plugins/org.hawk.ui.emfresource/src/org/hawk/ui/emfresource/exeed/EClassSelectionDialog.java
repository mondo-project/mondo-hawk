/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.ui.emfresource.exeed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hawk.emfresource.HawkResource;
import org.hawk.ui.emfresource.Activator;

public class EClassSelectionDialog extends Dialog {
	private final HawkResource resource;
	private EPackage ePackage;
	private EClass eClass;

	EClassSelectionDialog(Shell parentShell, HawkResource hawkResource) {
		super(parentShell);
		this.resource = hawkResource;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		final Label lblPackage = new Label(container, SWT.NONE);
		lblPackage.setText("Package:");

		final ComboViewer lPackages = new ComboViewer(container, SWT.BORDER);
		lPackages.setLabelProvider(new LabelProvider());
		lPackages.setContentProvider(new ArrayContentProvider());
		Object[] packages;
		try {
			packages = resource.getRegisteredMetamodels().toArray();
		} catch (Exception e) {
			Activator.logError("Could not retrieve registered metamodels", e);
			packages = new Object[0];
		}
		Arrays.sort(packages);
		lPackages.setInput(packages);
		lPackages.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label lblClass = new Label(container, SWT.NONE);
		lblClass.setText("EClass:");

		final ComboViewer lClasses = new ComboViewer(container, SWT.DROP_DOWN);
		lClasses.setLabelProvider(new LabelProvider());
		lClasses.setContentProvider(new ArrayContentProvider());
		lClasses.setInput(new Object[0]);
		lClasses.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		lPackages.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				eClass = null;
				if (event.getSelection() instanceof IStructuredSelection) {
					final IStructuredSelection ssel = (IStructuredSelection)event.getSelection();
					if (ssel.getFirstElement() != null) {
						final String packageURI = (String)ssel.getFirstElement();
						ePackage = resource.getResourceSet().getPackageRegistry().getEPackage(packageURI);

						final List<String> eClassNames = new ArrayList<>();
						if (ePackage != null) {
							for (EClassifier classifier : ePackage.getEClassifiers()) {
								if (classifier instanceof EClass) {
									eClassNames.add(classifier.getName());
								}
							}
						}
						final Object[] arrClassNames = eClassNames.toArray();
						Arrays.sort(arrClassNames);
						lClasses.setInput(arrClassNames);
					}
				}
				checkOKEnabled();
			}
		});

		lClasses.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					final IStructuredSelection ssel = (IStructuredSelection)event.getSelection();
					if (ssel.getFirstElement() != null) {
						final String className = (String)ssel.getFirstElement();
						eClass = (EClass)ePackage.getEClassifier(className);
					}
				}

				checkOKEnabled();
			}
		});

		return container;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		checkOKEnabled();
		return control;
	}

	public EClass getEClass() {
		return eClass;
	}

	private void checkOKEnabled() {
		getButton(IDialogConstants.OK_ID).setEnabled(eClass != null);
	}
}