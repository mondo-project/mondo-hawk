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

	public DeletionUtils(IGraphDatabase graph) {
		this.graph = graph;
	}

	protected void delete(IGraphNode modelElement) {

		try {
			removeFromIndexes(modelElement);
			modelElement.delete();
		} catch (Exception e) {
			System.err.println("DELETE NODE EXCEPTION:");
			e.printStackTrace();
		}

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
				changeListener.modelElementRemoval(s, node, false);
				delete(node);
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

	protected void makeProxyRefs(VcsCommitItem commitItem,
			IGraphNode referencedModelElement, String repositoryURL,
			IGraphNode referencedElementFileNode, IGraphChangeListener listener) {

		IGraphNodeIndex proxydictionary = graph
				.getOrCreateNodeIndex("proxydictionary");

		// handle any incoming references (after dereference, aka other file
		// ones)
		// FIXMEdone 5-11-13 check changes work
		for (IGraphEdge rel : referencedModelElement.getIncoming()) {

			final IGraphNode referencingNode = rel.getStartNode();
			final IGraphNode endNode = rel.getEndNode();
			String referencingNodeFileID = referencingNode
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)
					.iterator().next().getEndNode()
					.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
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
								IModelIndexer.IDENTIFIER_PROPERTY).toString();

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

				referencingNode.setProperty(
						GraphModelUpdater.PROXY_REFERENCE_PREFIX
								+ fullReferencedElementPathFileURI, proxies);

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

	protected void dereference(IGraphNode modelElement, IGraphChangeListener l,
			VcsCommitItem s) {

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
