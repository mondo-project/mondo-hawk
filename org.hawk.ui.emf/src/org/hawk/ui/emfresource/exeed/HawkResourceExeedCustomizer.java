/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.ui.emfresource.exeed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.ui.viewer.ColumnViewerInformationControlToolTipSupport;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.presentation.EcoreEditorPlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.ui.celleditor.AdapterFactoryTreeEditor;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.emf.edit.ui.provider.DecoratingColumLabelProvider;
import org.eclipse.emf.edit.ui.provider.DiagnosticDecorator;
import org.eclipse.epsilon.dt.exeed.ExeedEditor;
import org.eclipse.epsilon.dt.exeed.extensions.IExeedCustomizer;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.hawk.emfresource.HawkResource;

public class HawkResourceExeedCustomizer implements IExeedCustomizer {

	private static final class SingleNodeDiagnosticDecorator extends DiagnosticDecorator {
		private SingleNodeDiagnosticDecorator(EditingDomain editingDomain,
				StructuredViewer viewer, IDialogSettings dialogSettings) {
			super(editingDomain, viewer, dialogSettings);
		}

		@Override
		protected BasicDiagnostic decorate(
				Map<Object, BasicDiagnostic> objects,
				ITreeContentProvider treeContentProvider, Set<Object> visited,
				Object object, List<Integer> path) {
			BasicDiagnostic result = decorations.get(object);
			if (visited.add(object)) {
				result = decorate(decorations, object, result, path);
			}
			return result;
		}
	}

	@Override
	public void createPages(ExeedEditor editor, Composite container, AdapterFactory adapterFactory) {
		/*
		 * This is a slightly customized version of {@link EcoreEditor#createPages()}. The problem with
		 * Ecore's version is that its DiagnosticDecorator implementation will scan the tree from leaves
		 * to roots, propagating error markers. We need to disable this behaviour to be able to provide
		 * "lazy" loading, and we have no place to do it but right where the DiagnosticDecorator instance
		 * is created, as the setInput(...) call will start the redecoration process.
		 */
		editor.setShowAllResources(true);

		// Creates the model from the editor input
	    editor.createModel();

	    // Only creates the other pages if there is something that can be edited
	    //
	    final EditingDomain editingDomain = editor.getEditingDomain();
		if (!editingDomain.getResourceSet().getResources().isEmpty())
	    {
	      // Create a page for the selection tree view.
	      //
	      Tree tree = new Tree(container, SWT.MULTI);
	      TreeViewer selectionViewer = new TreeViewer(tree);
	      editor.setCurrentViewer(selectionViewer);

	      selectionViewer.setContentProvider(new AdapterFactoryContentProvider(adapterFactory));
	      final IDialogSettings dialogSettings = EcoreEditorPlugin.getPlugin().getDialogSettings();
	      final DiagnosticDecorator labelDecorator = new SingleNodeDiagnosticDecorator(editingDomain, selectionViewer, dialogSettings);
	      selectionViewer.setLabelProvider(new DecoratingColumLabelProvider(new AdapterFactoryLabelProvider(adapterFactory), labelDecorator));
	      selectionViewer.setInput(editingDomain.getResourceSet());
	      selectionViewer.setSelection(new StructuredSelection(editingDomain.getResourceSet().getResources().get(0)), true);

	      new AdapterFactoryTreeEditor(selectionViewer.getTree(), adapterFactory);
	      new ColumnViewerInformationControlToolTipSupport(selectionViewer, new DiagnosticDecorator.EditingDomainLocationListener(editingDomain, selectionViewer));

	      editor.createContextMenuFor(selectionViewer);
	      int pageIndex = editor.addPage(tree);
	      editor.setPageText(pageIndex, EcoreEditorPlugin.INSTANCE.getString("_UI_SelectionPage_label"));
	    }

		final IWorkbenchPage page = editor.getEditorSite().getPage();
		page.addPartListener(new UnloadResourceSetOnCloseListener(page, editor));
	}

	@Override
	public boolean hasChildren(Resource r, EObject eob) {
		final HawkResource hawkResource = (HawkResource)r;
		return hawkResource.hasChildren(eob);
	}

	@Override
	public Collection<IAction> generateCreateChildActions(
			Collection<?> descriptors, ISelection selection) {
		return Collections.emptyList();
	}

	@Override
	public Collection<IAction> generateCreateSiblingActions(
			Collection<?> descriptors, ISelection selection) {
		return Collections.emptyList();
	}

	@Override
	public Collection<IAction> generateCustomizerActions(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection)selection;
			if (ssel.getFirstElement() instanceof HawkResource) {
				final HawkResource r = (HawkResource)ssel.getFirstElement();
				final List<IAction> actions = new ArrayList<>();
				final Action fetchByEClass = new FetchByEClassAction(r);
				fetchByEClass.setText("Fetch by EClass");
				actions.add(fetchByEClass);
				return actions;
			}
		}
		return Collections.emptyList();
	}

	@Override
	public boolean isEnabledFor(Resource r) {
		return r instanceof HawkResource;
	}

	@Override
	public InMemoryEmfModel createInMemoryEmfModel(Resource mainResource) {
		return new InMemoryEmfModel(mainResource, false);
	}
}
