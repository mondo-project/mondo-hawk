/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.osgiserver;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.osgiserver.HModel.HModelState;

public class SyncNotificationListener implements IGraphChangeListener {

	HModel model;

	public SyncNotificationListener(HModel m) {
		model = m;
	}

	@Override
	public String getName() {
		return "SyncNotificationListener";
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		System.out
				.println("this listener does not use IModelIndexer, this method does nothing.");
	}

	@Override
	public void synchroniseStart() {
		try {
			model.setStatus(HModelState.UPDATING);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void synchroniseEnd() {
		model.setStatus(HModelState.RUNNING);
	}

	@Override
	public void changeStart() {
	}

	@Override
	public void changeSuccess() {
	}

	@Override
	public void changeFailure() {
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient) {
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient) {
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String attrName, IGraphNode elementNode,
			boolean isTransient) {
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
	}

}
