/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.listener;

import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

/**
 * Component that listens to changes in the graph managed by Hawk. These changes are
 * transactional: listeners should only react to changes after the transaction has
 * been confirmed, or be prepared to roll back if the transaction is not successful.
 */
public interface IGraphChangeListener {

	void changeStart();
	void changeSuccess();
	void changeFailure();

	void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode);
	void classAddition(IHawkClass cls, IGraphNode clsNode);
	void fileAddition(VcsCommitItem s, IGraphNode fileNode);

	void modelElementAddition(VcsCommitItem s, IHawkObject element, IGraphNode elementNode);
	void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode);

	void modelElementAttributeUpdate(VcsCommitItem s, IHawkObject eObject, String attrName, Object oldValue, Object newValue, IGraphNode elementNode);
	void modelElementAttributeRemoval(VcsCommitItem s, IHawkObject eObject, String attrName, IGraphNode node);

	void referenceAddition(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel);
	void referenceRemoval(VcsCommitItem s, IGraphNode source, IGraphNode destination);

}
