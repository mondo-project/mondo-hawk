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
package org.hawk.core.graph;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

/**
 * <p>
 * Component that listens to changes in the graph managed by Hawk. These changes
 * are TX_MODE: listeners should only react to changes after the
 * transaction has been confirmed, or be prepared to roll back if the
 * transaction is not successful.
 * </p>
 *
 * <p>
 * Event sequences are usually one of two types:
 * </p>
 *
 * <ul>
 * <li>synchroniseStart - (changeStart - rest of events -
 * changeSuccess/changeFailure)* - synchroniseEnd, for synchronisations between
 * Hawk and its monitored VCS repositories.</li>
 * <li>changeStart - rest of events - changeSuccess/changeFailure, for other
 * changes (such as registering a metamodel).</li>
 * </ul>
 */
public interface IGraphChangeListener {

	String getName();

	void setModelIndexer(IModelIndexer m);

	/**
	 * The synchronisation between the model indexer and its VCS repositories has started.
	 */
	void synchroniseStart();

	/**
	 * The synchronisation between the model indexer and its VCS repositories has ended.
	 */
	void synchroniseEnd();

	/**
	 * A transaction for changing the graph has been started.
	 */
	void changeStart();

	/**
	 * A transaction for changing the graph has been completed successfully.
	 */
	void changeSuccess();

	/**
	 * A transaction for changing the graph has been rolled back.
	 */
	void changeFailure();

	/**
	 * A metamodel has been added to the graph.
	 */
	void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode);

	/**
	 * A class has been added to the graph.
	 */
	void classAddition(IHawkClass cls, IGraphNode clsNode);

	/**
	 * A model file has been added to the graph.
	 */
	void fileAddition(VcsCommitItem s, IGraphNode fileNode);

	/**
	 * A model file has been removed from the graph.
	 */
	void fileRemoval(VcsCommitItem s, IGraphNode fileNode);

	/**
	 * A model element has been added to the graph.
	 */
	void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient);

	/**
	 * A model element has been removed from the graph.
	 */
	void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient);

	/**
	 * An attribute of a model element in the graph has been set.
	 */
	void modelElementAttributeUpdate(VcsCommitItem s, IHawkObject eObject,
			String attrName, Object oldValue, Object newValue,
			IGraphNode elementNode, boolean isTransient);

	/**
	 * An attribute of a model element in the graph has been unset.
	 */
	void modelElementAttributeRemoval(VcsCommitItem s, IHawkObject eObject,
			String attrName, IGraphNode elementNode, boolean isTransient);

	/**
	 * An reference between two model elements in the graph has been added.
	 */
	void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient);

	/**
	 * An reference between two model elements in the graph has been removed.
	 */
	void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient);

}
