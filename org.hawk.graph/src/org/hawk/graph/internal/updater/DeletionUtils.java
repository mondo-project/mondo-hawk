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
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
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

	protected boolean deleteAll(VcsCommitItem s,
			IGraphChangeListener changeListener) throws Exception {

		long start = System.currentTimeMillis();

		boolean success = true;

		try (IGraphTransaction transaction = graph.beginTransaction()) {
			final String repository = s.getCommit().getDelta().getManager()
					.getLocation();
			final String filepath = s.getPath();

			final String fullFileID = repository
					+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + filepath;
			final Iterator<IGraphNode> itFile = graph.getFileIndex()
					.get("id", fullFileID).iterator();

			if (itFile.hasNext()) {
				IGraphNode file = itFile.next();

				System.out.println("deleting nodes from file: "
						+ file.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

				HashSet<IGraphNode> modelElements = new HashSet<IGraphNode>();

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
					delete(node);
					changeListener.modelElementRemoval(s, node, false);
				}

				modelElements = null;

				delete(file);
				changeListener.fileRemoval(s, file);
			} else {
				System.err
						.println("WARNING: could not find any file nodes for "
								+ fullFileID);
			}

			transaction.success();

		} catch (Exception e) {
			success = false;
			e.printStackTrace();
		}

		System.out.println("deleted all, took: "
				+ (System.currentTimeMillis() - start) / 1000 + "s"
				+ (System.currentTimeMillis() - start) / 1000 + "ms");
		return success;
	}

	protected String makeRelative(Set<String> bases, String extension) {
		for (final String base : bases) {
			if (extension.startsWith(base)) {
				return extension.substring(base.length());
			}
		}
		System.err
				.println(String.format(
						"WARNING: could not make '%s' into a relative path",
						extension));
		return extension;
	}

	protected String[] addToElementProxies(String[] proxies,
			String fullPathURI, String edgelabel, boolean isContainment,
			boolean isContainer) {

		// System.err.println("addtoelementproxies: " +
		// Arrays.toString(proxies));
		// System.err.println("fullpathuri " + fullPathURI);
		// System.err.println("edgelabel " + edgelabel);

		if (proxies != null) {

			String[] ret = new String[proxies.length + 4];

			for (int i = 0; i < proxies.length; i = i + 4) {

				ret[i] = proxies[i];
				ret[i + 1] = proxies[i + 1];
				ret[i + 2] = proxies[i + 2];
				ret[i + 3] = proxies[i + 3];

			}

			ret[proxies.length] = fullPathURI;
			ret[proxies.length + 1] = edgelabel;
			ret[proxies.length + 2] = isContainment + "";
			ret[proxies.length + 3] = isContainer + "";

			proxies = null;

			// System.err.println("ret " + Arrays.toString(ret));

			return ret;

		} else {
			String[] ret = new String[] { fullPathURI, edgelabel,
					isContainment + "", isContainer + "" };
			return ret;
		}
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

				proxies = addToElementProxies(
						(String[]) proxies,
						fullReferencedElementPathElementURI,
						type,
						rel.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null,
						rel.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINER) != null);

				referencingNode.setProperty(
						GraphModelUpdater.PROXY_REFERENCE_PREFIX
								+ fullReferencedElementPathFileURI, proxies);

				proxydictionary = graph.getOrCreateNodeIndex("proxydictionary");
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
		for (String indexName : graph.getNodeIndexNames())
			graph.getOrCreateNodeIndex(indexName).remove(n);
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
