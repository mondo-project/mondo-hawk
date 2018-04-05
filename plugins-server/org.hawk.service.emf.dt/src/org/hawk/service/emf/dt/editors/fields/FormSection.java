/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.emf.dt.editors.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public abstract class FormSection {
	protected Composite cContents;

	public FormSection(FormToolkit toolkit, Composite parent, String title, String description) {
	    final Section sectionContent = toolkit.createSection(parent, Section.TITLE_BAR|Section.DESCRIPTION);
	    sectionContent.setText(title);
	    sectionContent.setDescription(description);
	    TableWrapData tdSectionContent = new TableWrapData(TableWrapData.FILL_GRAB);
	    sectionContent.setLayoutData(tdSectionContent);

	    this.cContents =  toolkit.createComposite(sectionContent, SWT.WRAP);
	    sectionContent.setClient(cContents);
	}
}