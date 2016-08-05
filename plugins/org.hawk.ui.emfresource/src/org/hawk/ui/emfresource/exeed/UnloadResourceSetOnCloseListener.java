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

import org.eclipse.emf.ecore.presentation.EcoreEditor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Part listener that will unload the model when the editor is closed. This
 * is not done by default by neither the Exeed nor the Ecore editor: they
 * simply rely on the Resource being GC'ed, but that doesn't work for us if
 * we have an ongoing subscription to event changes in the indexer.
 */
public class UnloadResourceSetOnCloseListener implements IPartListener2 {
	private final IWorkbenchPage page;
	private final EcoreEditor editor;

	UnloadResourceSetOnCloseListener(IWorkbenchPage page, EcoreEditor editor) {
		this.page = page;
		this.editor = editor;
	}

	@Override public void partActivated(IWorkbenchPartReference partRef) {}
	@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {}
	@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
	@Override public void partOpened(IWorkbenchPartReference partRef) {}
	@Override public void partHidden(IWorkbenchPartReference partRef) {}
	@Override public void partVisible(IWorkbenchPartReference partRef) {}
	@Override public void partInputChanged(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef.getPage() == page) {
			ResourceSet resourceSet = editor.getEditingDomain().getResourceSet();
			for (Resource r : resourceSet.getResources()) {
				r.unload();
			}
		}
	}
}