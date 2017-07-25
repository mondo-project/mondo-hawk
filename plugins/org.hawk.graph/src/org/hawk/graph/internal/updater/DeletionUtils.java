/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.internal.updater;

import java.util.HashSet;
import java.util.Iterator;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.ModelElementNode;

public class DeletionUtils {

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
				System.err.println("DELETE NODE EXCEPTION:");
				e.printStackTrace();
			}
		return false;

	}

	protected boolean deleteAll(IGraphNode file, VcsCommitItem s,
			IGraphChangeListener changeListener) throws Exception {

		long start = System.currentTimeMillis();

		boolean success = true;

		try {
			final String repository = s.getCommit().getDelta().getManager()
					.getLocation();

			// IGraphNode file = itFile.next();

			System.out.println("deleting nodes from file: "
					+ file.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

			HashSet<IGraphNode> modelElements = new HashSet<IGraphNode>();

			for (IGraphEdge rel : file
					.getIncomingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				modelElements.add(rel.getStartNode());
				rel.delete();
			}

			// if (IModelIndexer.VERBOSE)
			// System.out
			// .println("cached elements and deleted file edges");

			for (IGraphNode node : modelElements) {
				dereference(node, changeListener, s);
			}

			// if (IModelIndexer.VERBOSE)
			// System.out.println("dereferenced elements");

			for (IGraphNode node : modelElements) {
				makeProxyRefs(s, node, repository, file, changeListener);
			}

			// if (IModelIndexer.VERBOSE)
			// System.out.println("made required proxy references");

			// if (IModelIndexer.VERBOSE) {
			// System.out.println("listeners registered:");
			//
			// for (Iterator<IGraphChangeListener> it =
			// ((CompositeGraphChangeListener) changeListener)
			// .iterator(); it.hasNext();) {
			//
			// IGraphChangeListener l = it.next();
			// System.out.println(l);
			//
			// }
			// }

			for (IGraphNode node : modelElements) {
				if (delete(node))
					changeListener.modelElementRemoval(s, node, false);
			}

			// if (IModelIndexer.VERBOSE)
			// System.out.println("deleted elements");

			modelElements = null;

			changeListener.fileRemoval(s, file);
			delete(file);

			// if (IModelIndexer.VERBOSE)
			// System.out.println("ending deletion");

		} catch (Exception e) {
			success = false;
			e.printStackTrace();
		}

		System.out.println("deleted all, took: "
				+ (System.currentTimeMillis() - start) / 1000 + "s"
				+ (System.currentTimeMillis() - start) / 1000 + "ms");
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

				// System.out.println(referencingNodeFileID+" ::: "+fileID);

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
						System.err
								.println("warning dereference has null listener/vcscommit -- this should only be used for non-model elements");
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
			System.err.println("DELETE NODE EXCEPTION:");
			e.printStackTrace();
		}

	}

	public void dereference(IGraphNode metaModelElement) {
		dereference(metaModelElement, null, null);
	}

}
