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
package org.hawk.graph;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;

/**
 * Wraps an {@link IGraphDatabase} that has been updated by this plugin. This is
 * the starting point for the read-only abstraction of the graph populated by
 * this updater. Users of this class and any other classes of this package are
 * expected to manage their own transactions with
 * {@link IGraphDatabase#beginTransaction()}.
 *
 * TODO This is an incomplete WIP abstraction. More methods will be called as
 * the existing queries are moved into this API.
 */
public class GraphWrapper {

	private final IGraphDatabase graph;

	public GraphWrapper(IGraphDatabase graph) {
		this.graph = graph;
	}

	/**
	 * Returns all the file nodes with repository URLs that match the
	 * <code>repositoryPattern</code> and at least one of the
	 * <code>patterns</code>.
	 *
	 * @param repositoryPattern
	 *            Pattern for the repository (
	 *            <code>null<code> or <code>"*"</code>) for all repositories.
	 * @param filePatterns
	 *            Patterns for the files (<code>"*"</code> for all
	 *            repositories). Passing in a <code>null</code> {@link Iterable}
	 *            is also treated as "*".
	 */
	public Set<FileNode> getFileNodes(String repositoryPattern, Iterable<String> filePatterns) {
		final IGraphNodeIndex fileIndex = graph.getFileIndex();
		
		//NB repo needs total match or no match / files can be partial matches
		
		if (repositoryPattern == null && filePatterns == null) {
			repositoryPattern = "*";
			filePatterns = Arrays.asList("*");
		}
			
		if (repositoryPattern != null && filePatterns == null) {
			if(repositoryPattern.length()==0)repositoryPattern="*";
			filePatterns = Arrays.asList("*");
		}
		
		if (repositoryPattern != null && filePatterns != null) {
		//prey to god they formatted it correctly!
		}
		
		final Set<FileNode> files = new LinkedHashSet<>();
		
		if ((repositoryPattern == null || repositoryPattern.length()==0) && filePatterns != null) {
		//hard case need to naively find all repositories and find file matches
			
			for (IGraphNode n : fileIndex.query("*","*")) {
				for (String s : filePatterns){
					if(n.getProperty("id").toString().matches(s.trim().replaceAll("\\*","(.\\*)")))files.add(new FileNode(n));			
				}				
			}
			
			return files;
		}
		
		for (String s : filePatterns) {
			for (IGraphNode n : fileIndex.query(repositoryPattern, s.trim())) {
				files.add(new FileNode(n));
			}
		}
		return files;
	}

	/**
	 * Returns the graph wrapped by this instance.
	 */
	public IGraphDatabase getGraph() {
		return graph;
	}

	/**
	 * Retrieves a {@link ModelElementNode} by identifier.
	 *
	 * @throws NoSuchElementException
	 *             No node with that identifier exists.
	 */
	public ModelElementNode getModelElementNodeById(Object id) {
		final IGraphNode rawNode = graph.getNodeById(id);
		if (rawNode == null) {
			throw new NoSuchElementException();
		}
		return new ModelElementNode(rawNode);
	}
}
