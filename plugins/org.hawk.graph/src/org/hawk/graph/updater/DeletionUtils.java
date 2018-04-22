/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - switch to logging through SLF4J
 ******************************************************************************/
package org.hawk.graph.updater;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletionUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeletionUtils.class);

	private IGraphDatabase graph;
	private IGraphNodeIndex singletonIndex;

	public DeletionUtils(IGraphDatabase graph) {
		this.graph = graph;
		singletonIndex = graph
				.getOrCreateNodeIndex(GraphModelBatchInjector.FRAGMENT_DICT_NAME);
	}

	protected boolean delete(IGraphNode modelElement) {

		// only try to delete nodes with no edges
		if (!modelElement.getEdges().iterator().hasNext())
			try {
				removeFromIndexes(modelElement);
				modelElement.delete();
				return true;
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		return false;

	}

	protected boolean deleteAll(IGraphNode file, VcsCommitItem s,
			IGraphChangeListener changeListener) throws Exception {

		long start = System.currentTimeMillis();

		boolean success = true;

		try {
			final String repository = s.getCommit().getDelta().getManager().getLocation();
			LOGGER.debug("deleting nodes from file: {}", file.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

			Set<IGraphNode> modelElements = new HashSet<IGraphNode>();
			for (IGraphEdge rel : file
					.getIncomingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				modelElements.add(rel.getStartNode());
				rel.delete();
			}

			for (IGraphNode node : modelElements) {
				dereference(node, changeListener, s);
			}
			for (IGraphNode node : modelElements) {
				makeProxyRefs(s, node, repository, file, changeListener);
			}
			for (IGraphNode node : modelElements) {
				if (delete(node))
					changeListener.modelElementRemoval(s, node, false);
			}

			modelElements = null;
			changeListener.fileRemoval(s, file);
			delete(file);

			LOGGER.debug("deleted all, took: {}s", (System.currentTimeMillis() - start) / 1000.0);
		} catch (Exception e) {
			success = false;
			LOGGER.error(e.getMessage(), e);
		}

		return success;
	}

	/*
	 * Should be called after dereference
	 */
	protected void makeProxyRefs(VcsCommitItem commitItem,
			IGraphNode referencedModelElement, String repositoryURL,
			IGraphNode referencedElementFileNode, IGraphChangeListener listener) {

		IGraphNodeIndex proxydictionary = graph
				.getOrCreateNodeIndex("proxydictionary");

		// track nodes with multiple sources (singletons)
		boolean isOrphan = true;

		Iterator<IGraphNode> singletonMatches = singletonIndex.get(
				"id",
				referencedModelElement
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY))
				.iterator();
		if (singletonMatches.hasNext())
			if (singletonMatches.next()
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)
					.iterator().hasNext())
				isOrphan = false;

		if (isOrphan)
			for (IGraphEdge rel : referencedModelElement.getIncoming()) {

				final IGraphNode referencingNode = rel.getStartNode();
				final IGraphNode endNode = rel.getEndNode();
				
				// handle incoming edges from derived references
				if (referencingNode.getProperty("derivationlanguage") != null
						&& referencingNode.getProperty("derivationlogic") != null) {
					rel.delete();
					return;
				}
								
				String referencingNodeFileID = referencingNode
						.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)
						.iterator().next().getEndNode()
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.toString();
				String referencedElementFileID = (String) referencedElementFileNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);

				String type = rel.getType();
				if (!referencingNodeFileID.equals(referencedElementFileID)) {
					String fullReferencedElementPathFileURI = repositoryURL
							+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
							+ referencedElementFileID;

					String fullReferencedElementPathElementURI = fullReferencedElementPathFileURI
							+ "#"
							+ referencedModelElement.getProperty(
									IModelIndexer.IDENTIFIER_PROPERTY)
									.toString();

					Object proxies = referencingNode
							.getProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX
									+ fullReferencedElementPathFileURI);

					proxies = new Utils()
							.addToElementProxies(
									(String[]) proxies,
									fullReferencedElementPathElementURI,
									type,
									rel.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null,
									rel.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINER) != null);

					referencingNode
							.setProperty(
									GraphModelUpdater.PROXY_REFERENCE_PREFIX
											+ fullReferencedElementPathFileURI,
									proxies);

					proxydictionary.add(referencingNode,
							GraphModelUpdater.PROXY_REFERENCE_PREFIX,
							fullReferencedElementPathFileURI);

					rel.delete();
				} else {
					// same file so just delete
					rel.delete();
				}
				listener.referenceRemoval(commitItem, referencingNode, endNode,
						type, false);
			}
	}

	/*
	 * Should be called before makeproxyrefs
	 */
	protected void dereference(IGraphNode modelElement, IGraphChangeListener l,
			VcsCommitItem s) {

		boolean safeToDereference = true;

		// track nodes with multiple sources (singletons)
		if (s != null)
			for (IGraphEdge rel : modelElement
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				IGraphNode fileNode = rel.getEndNode();

				if (s.getPath()
						.equals(fileNode
								.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)))
					rel.delete();
				else
					safeToDereference = false;

			}

		if (safeToDereference)
			for (IGraphEdge rel : modelElement.getOutgoing()) {

				// delete derived attributes stored as nodes
				if (rel.getProperty("isDerived") != null) {
					if (l == null && s == null) {
						LOGGER.warn("warning dereference has null listener/vcscommit -- this should only be used for non-model elements");
						break;
					}
					IGraphNode n = rel.getEndNode();
					l.modelElementRemoval(s, n, true);
					removeFromIndexes(n);

					// remove outgoing edges (would exist if this is a derived
					// reference node)
					for (IGraphEdge e : n.getOutgoing())
						e.delete();
					
					n.delete();
				}

				rel.delete();

			}

	}

	private void removeFromIndexes(IGraphNode n) {

		for (String indexname : graph.getNodeIndexNames())
			graph.getOrCreateNodeIndex(indexname).remove(n);

	}

	public void delete(IGraphEdge rel) {
		try {
			rel.delete();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public void dereference(IGraphNode metaModelElement) {
		dereference(metaModelElement, null, null);
	}

}
