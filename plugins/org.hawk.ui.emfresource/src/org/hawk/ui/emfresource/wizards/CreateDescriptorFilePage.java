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
package org.hawk.ui.emfresource.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.hawk.ui.emfresource.LocalHawkResourceFactoryImpl;

public class CreateDescriptorFilePage extends WizardNewFileCreationPage {

	private String selectedInstance;
	private boolean isSplit;
	private List<String> repositoryPatterns, filePatterns;

	public CreateDescriptorFilePage(IStructuredSelection currentSelection) {
		super("Create new local Hawk model descriptor", currentSelection);
		setTitle("Create new local Hawk model descriptor");
		setDescription("Select the destination path for the new descriptor.");
	}

	@Override
	protected InputStream getInitialContents() {
		final StringBuffer sbuf = new StringBuffer();
		sbuf.append(selectedInstance);

		sbuf.append(System.lineSeparator());
		sbuf.append(LocalHawkResourceFactoryImpl.OPTION_RPATTERNS);
		sbuf.append(LocalHawkResourceFactoryImpl.KEYVAL_SEPARATOR);
		appendAll(sbuf, LocalHawkResourceFactoryImpl.PATTERN_SEPARATOR, repositoryPatterns);

		sbuf.append(System.lineSeparator());
		sbuf.append(LocalHawkResourceFactoryImpl.OPTION_FPATTERNS);
		sbuf.append(LocalHawkResourceFactoryImpl.KEYVAL_SEPARATOR);
		appendAll(sbuf, LocalHawkResourceFactoryImpl.PATTERN_SEPARATOR, filePatterns);

		if (!isSplit) {
			sbuf.append(System.lineSeparator());
			sbuf.append(LocalHawkResourceFactoryImpl.OPTION_UNSPLIT);
		}

		byte[] bytes;
		try {
			bytes = sbuf.toString().getBytes(LocalHawkResourceFactoryImpl.FILE_ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			bytes = new byte[0];
		}
		return new ByteArrayInputStream(bytes);
	}

	private void appendAll(StringBuffer sbuf, String sep, List<String> elems) {
		boolean first = true;
		for (String elem : elems) {
			if (first) {
				first = false;
			} else {
				sbuf.append(sep);
			}
			sbuf.append(elem);
		}
	}

	public String getSelectedInstance() {
		return selectedInstance;
	}

	public void setSelectedInstance(String selectedInstance) {
		this.selectedInstance = selectedInstance;
	}

	public boolean isSplit() {
		return isSplit;
	}

	public void setSplit(boolean isSplit) {
		this.isSplit = isSplit;
	}

	public List<String> getRepositoryPatterns() {
		return repositoryPatterns;
	}

	public void setRepositoryPatterns(List<String> repositoryPatterns) {
		this.repositoryPatterns = repositoryPatterns;
	}

	public List<String> getFilePatterns() {
		return filePatterns;
	}

	public void setFilePatterns(List<String> filePatterns) {
		this.filePatterns = filePatterns;
	}
}
