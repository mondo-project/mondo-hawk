/*******************************************************************************
 * Copyright (c) 2018 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.timeaware.graph;

import java.util.Collections;
import java.util.List;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Keeps track of information in the graph about the various VCS. This class
 * does not handle transactions: users are expected to do it.
 */
public class VCSManagerIndex {
	private static final String URI_PROPERTY = "uri";

	private final IGraphNodeIndex idx;
	private final IGraphDatabase db;

	/**
	 * Type-safe wrapper for the node we keep in the graph about a repository.
	 */
	public class RepositoryNode {
		private static final String LASTREV_PROPERTY = "lastRevision";
		private final IGraphNode node;

		public RepositoryNode(IGraphNode n) {
			this.node = n;
		}

		public String getURI() {
			return node.getProperty(URI_PROPERTY) + "";
		}

		/**
		 * Returns the last revision indexed for this VCS, or <code>null</code> if it
		 * has not been indexed yet.
		 */
		public String getLastRevision() {
			final Object lastRev = node.getProperty(LASTREV_PROPERTY);
			if (lastRev == null) {
				return null;
			} else {
				return lastRev.toString();
			}
		}

		/**
		 * Changes the last revision indexed for this VCS.
		 */
		public void setLastRevision(String lastRev) {
			node.setProperty(LASTREV_PROPERTY, lastRev);
		}

		public IGraphNode getNode() {
			return node;
		}
	}

	/**
	 * Creates a new instance. Retrieves the existing node index in the graph,
	 * or creates a new one if it does not exist. 
	 */
	public VCSManagerIndex(IGraphDatabase db) {
		this.db = db;
		this.idx = db.getOrCreateNodeIndex("_hawkVCSIndex");
	}

	/**
	 * Retrieves the {@link RepositoryNode} associated with a URI, creating
	 * it if it does not exist already.
	 */
	public RepositoryNode getOrCreateRepositoryNode(String repoURI) {
		IGraphIterable<? extends IGraphNode> iNode = idx.get(URI_PROPERTY, repoURI);
		if (iNode.size() > 0) {
			return new RepositoryNode(iNode.getSingle());
		} else {
			final IGraphNode node = db.createNode(
				Collections.singletonMap(URI_PROPERTY, repoURI), "_hawkRepo");
			idx.add(node, URI_PROPERTY, repoURI);
			return new RepositoryNode(node);
		}
	}

	/**
	 * Deletes the {@link RepositoryNode} associated with a URI, if it exists.
	 */
	public void removeRepositoryNode(String repoURI) {
		IGraphIterable<? extends IGraphNode> iNode = idx.get(URI_PROPERTY, repoURI);
		if (iNode.size() > 0) {
			IGraphNode node = iNode.getSingle();
			idx.remove(node);
			node.delete();
		}
	}

}
