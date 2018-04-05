/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - simplified, removed Neo4j-specific bits
 ******************************************************************************/
package org.hawk.core.graph;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IConsole;

public interface IGraphDatabase {

	/**
	 * Returns the path to the main directory storing the database.
	 */
	String getPath();

	/**
	 * Starts the database.
	 * 
	 * @param parentfolder
	 *            Folder that will store the database.
	 * @param c
	 *            Console to print messages on.
	 */
	void run(File parentfolder, IConsole c);

	/**
	 * Shuts down the database, ready to be started again with {@link #run(File, IConsole)}.
	 */
	void shutdown() throws Exception;

	/**
	 * Deletes the database and all the data within.
	 */
	void delete() throws Exception;

	/**
	 * Returns the node index with the specified name. If it does not exist, it is
	 * created on the fly.
	 * 
	 * @param name
	 *            Name of the index to be returned.
	 */
	IGraphNodeIndex getOrCreateNodeIndex(String name);

	/**
	 * Returns a node index for the metamodel nodes.
	 */
	IGraphNodeIndex getMetamodelIndex();

	/**
	 * Returns a node index for the file nodes.
	 */
	IGraphNodeIndex getFileIndex();

	/**
	 * Starts a transaction on the database. The database is only expected
	 * to support a single transaction at a time (happens often: e.g. Neo4j,
	 * Greycat).
	 */
	IGraphTransaction beginTransaction() throws Exception;

	/**
	 * Returns <code>true</code> iff this store supports a transactional mode.
	 */
	boolean isTransactional();

	/**
	 * Moves the database into batch mode, where all changes are immediate and
	 * the database is optimised for mass insertion. Rollbacks are not supported.
	 */
	void enterBatchMode();

	/**
	 * Moves the database into transactional mode, where changes are not permanent
	 * until the transaction is committed.
	 */
	void exitBatchMode();

	/**
	 * Returns all the nodes of a certain type.
	 */
	IGraphIterable<IGraphNode> allNodes(String label);

	IGraphNode createNode(Map<String, Object> props, String label);

	IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type);

	IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props);

	Object getGraph();

	IGraphNode getNodeById(Object id);

	boolean nodeIndexExists(String name);

	String getType();

	String getHumanReadableName();

	String getTempDir();

	public enum Mode {
		TX_MODE, NO_TX_MODE, UNKNOWN
	};

	/**
	 * Returns whether the current database is in transactional (update) or
	 * non-transactional (batch insert) mode.
	 */
	Mode currentMode();

	Set<String> getNodeIndexNames();

	Set<String> getKnownMMUris();

}
