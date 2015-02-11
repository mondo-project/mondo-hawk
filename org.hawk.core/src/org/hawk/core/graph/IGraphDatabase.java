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
package org.hawk.core.graph;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.VcsCommitItem;

public interface IGraphDatabase {

	String fileIndexName = "FILEINDEX";
	String metamodelIndexName = "METAMODELINDEX";

	String getPath();

	void run(String name, File parentfolder, IAbstractConsole c);

	void shutdown(boolean b) throws Exception;

	// can be used to get any index not exposed here or created by developers.
	IGraphNodeIndex getOrCreateNodeIndex(String name);

	IGraphEdgeIndex getOrCreateEdgeIndex(String name);

	IGraphNodeIndex getMetamodelIndex();

	IGraphNodeIndex getFileIndex();

	IGraphTransaction beginTransaction() throws Exception;

	// returns whether this store supports a transactional mode
	boolean isTransactional();

	// any non-transactional mode
	void enterBatchMode();

	void exitBatchMode();

	Iterable<IGraphNode> allNodes(String label);

	IGraphNode createNode(Map<String, Object> map, String string);

	IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type);

	IGraphEdge createRelationship(IGraphNode start, IGraphNode end,
			String type, Map<String, Object> props);

	Object getGraph();

	IGraphNode getNodeById(Object id);

	Map<?, ?> getConfig();

	boolean nodeIndexExists(String name);

	boolean edgeIndexExists(String name);

	String getType();

	String getHumanReadableName();

	String getTempDir();

	Set<VcsCommitItem> compareWithLocalFiles(Set<VcsCommitItem> reposItems);

	public abstract void logFull() throws Exception;

	String getName();

	String currentMode();

	Set<String> getNodeIndexNames();

	Set<String> getEdgeIndexNames();

	Set<String> getKnownMMUris();

}
