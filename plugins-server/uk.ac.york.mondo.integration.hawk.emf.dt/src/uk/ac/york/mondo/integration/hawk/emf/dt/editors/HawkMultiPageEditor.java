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
package uk.ac.york.mondo.integration.hawk.emf.dt.editors;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

import org.apache.thrift.transport.TTransportException;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.FileEditorInput;

import uk.ac.york.mondo.integration.api.Hawk;
import uk.ac.york.mondo.integration.api.dt.http.LazyCredentials;
import uk.ac.york.mondo.integration.api.utils.APIUtils;
import uk.ac.york.mondo.integration.hawk.emf.HawkModelDescriptor;
import uk.ac.york.mondo.integration.hawk.emf.dt.Activator;

/**
 * Editor for <code>.hawkmodel</code> files. The first page is a form-based UI
 * for editing the raw text on the second page.
 */
public class HawkMultiPageEditor extends FormEditor	implements IResourceChangeListener {

	private DetailsFormPage detailsPage;
	private EffectiveMetamodelFormPage emmPage;
	private TextEditor textEditor;
	private int textEditorPageIndex;
	private boolean isDirty;

	public HawkMultiPageEditor() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (this.getActivePage() != textEditorPageIndex) {
			refreshRawText();
		}
		textEditor.doSave(monitor);
		if (this.getActivePage() == textEditorPageIndex) {
			refreshForm();
		}
		setDirty(false);
	}

	/**
	 * Saves the multi-page textEditor's document as another file. Also updates the
	 * text for page {@link #RAW_EDITOR_PAGE_INDEX}'s tab, and updates this
	 * multi-page textEditor's input to correspond to the nested textEditor's.
	 */
	@Override
	public void doSaveAs() {
		refreshRawText();
		textEditor.doSaveAs();
		setPageText(textEditorPageIndex, textEditor.getTitle());
		setInput(textEditor.getEditorInput());
		setDirty(false);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, editorInput);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Closes all project files on project close.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) textEditor.getEditorInput()).getFile().getProject().equals(event.getResource())) {
							IEditorPart editorPart = pages[i].findEditor(textEditor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	private void createFormBasedEditorPage() throws PartInitException {
		detailsPage = new DetailsFormPage(this, "details", "Descriptor");
		int index = addPage(detailsPage, getEditorInput());
		setPageText(index, detailsPage.getTitle());
	}

	private void createEffectiveMetamodelPage() throws PartInitException {
		emmPage = new EffectiveMetamodelFormPage(this, "emm", "Effective Metamodel");
		int index = addPage(emmPage);
		setPageText(index, emmPage.getTitle());
	}

	private void createRawTextEditorPage() throws PartInitException {
		textEditor = new TextEditor();
		textEditorPageIndex = addPage(textEditor, getEditorInput());
		setPageText(textEditorPageIndex, textEditor.getTitle());
		setPartName(textEditor.getTitle());
	}

	private IDocument getDocument() {
		return textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
	}

	private void refreshForm() {
		final IDocument doc = getDocument();
		final String sContents = doc.get();

		final HawkModelDescriptor descriptor = new HawkModelDescriptor();
		try {
			descriptor.load(new StringReader(sContents));

			detailsPage.getInstanceSection().setServerURL(descriptor.getHawkURL());
			detailsPage.getInstanceSection().setInstanceName(descriptor.getHawkInstance());
			detailsPage.getInstanceSection().setThriftProtocol(descriptor.getThriftProtocol());
			detailsPage.getInstanceSection().setUsername(descriptor.getUsername());
			detailsPage.getInstanceSection().setPassword(descriptor.getPassword());
			detailsPage.getContentSection().setRepositoryURL(descriptor.getHawkRepository());
			detailsPage.getContentSection().setFilePatterns(descriptor.getHawkFilePatterns());
			detailsPage.getContentSection().setLoadingMode(descriptor.getLoadingMode());
			detailsPage.getContentSection().setQueryLanguage(descriptor.getHawkQueryLanguage());
			detailsPage.getContentSection().setQuery(descriptor.getHawkQuery());
			detailsPage.getContentSection().setDefaultNamespaces(descriptor.getDefaultNamespaces());
			detailsPage.getContentSection().setSplit(descriptor.isSplit());
			detailsPage.getContentSection().setPageSize(descriptor.getPageSize());
			detailsPage.getSubscriptionSection().setSubscribed(descriptor.isSubscribed());
			detailsPage.getSubscriptionSection().setClientID(descriptor.getSubscriptionClientID());
			detailsPage.getSubscriptionSection().setDurability(descriptor.getSubscriptionDurability());

			emmPage.setEffectiveMetamodel(descriptor.getEffectiveMetamodel());
		} catch (IOException e) {
			Activator.getDefault().logError(e);
		}
	}

	private void refreshRawText() {
		final HawkModelDescriptor descriptor = buildDescriptor();
		final StringWriter sW = new StringWriter();
		try {
			descriptor.save(sW);

			final IDocument doc = getDocument();
			doc.set(sW.toString());
		} catch (IOException e) {
			Activator.getDefault().logError(e);
		}
	}

	protected HawkModelDescriptor buildDescriptor() {
		final HawkModelDescriptor descriptor = new HawkModelDescriptor();
		descriptor.setHawkURL(detailsPage.getInstanceSection().getServerURL());
		descriptor.setHawkInstance(detailsPage.getInstanceSection().getInstanceName());
		descriptor.setThriftProtocol(detailsPage.getInstanceSection().getThriftProtocol());
		descriptor.setUsername(detailsPage.getInstanceSection().getUsername());
		descriptor.setPassword(detailsPage.getInstanceSection().getPassword());
		descriptor.setHawkRepository(detailsPage.getContentSection().getRepositoryURL());
		descriptor.setHawkFilePatterns(detailsPage.getContentSection().getFilePatterns());
		descriptor.setLoadingMode(detailsPage.getContentSection().getLoadingMode());
		descriptor.setSubscribed(detailsPage.getSubscriptionSection().isSubscribed());
		descriptor.setSubscriptionClientID(detailsPage.getSubscriptionSection().getClientID());
		descriptor.setSubscriptionDurability(detailsPage.getSubscriptionSection().getDurability());
		descriptor.setHawkQueryLanguage(detailsPage.getContentSection().getQueryLanguage());
		descriptor.setHawkQuery(detailsPage.getContentSection().getQuery());
		descriptor.setDefaultNamespaces(detailsPage.getContentSection().getDefaultNamespaces());
		descriptor.setSplit(detailsPage.getContentSection().isSplit());
		descriptor.setPageSize(detailsPage.getContentSection().getPageSize());
		descriptor.setEffectiveMetamodel(emmPage.getEffectiveMetamodel());
		return descriptor;
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (isDirty()) { 
			if (newPageIndex == textEditorPageIndex && isDirty()) {
				refreshRawText();
			} else {
				refreshForm();
			}
		}
	}

	@Override
	protected void addPages() {
		try {
			createFormBasedEditorPage();
			createEffectiveMetamodelPage();
			createRawTextEditorPage();
			refreshForm();
		} catch (Exception ex) {
			Activator.getDefault().logError(ex);
		}
	}

	protected Hawk.Client connectToHawk(final HawkModelDescriptor d) throws TTransportException, URISyntaxException {
		return APIUtils.connectTo(Hawk.Client.class,
				d.getHawkURL(), d.getThriftProtocol(),
				new LazyCredentials(d.getHawkURL()));
	}

	@Override
	public boolean isDirty() {
		return super.isDirty() || isDirty;
	}

	protected void setDirty(boolean newValue) {
		if (this.isDirty != newValue) {
			this.isDirty = newValue;
			firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
		}
	}
}
