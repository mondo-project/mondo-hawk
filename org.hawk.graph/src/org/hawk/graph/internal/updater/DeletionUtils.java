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

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;

public class DeletionUtils {

	private IGraphDatabase graph;
	private IGraphNodeIndex filedictionary;
	private IGraphNodeIndex proxydictionary;

	public DeletionUtils(IGraphDatabase graph) {
		this.graph = graph;
		if (graph.currentMode().equals(
		// "TRANSACTIONAL_MODE"
				IGraphDatabase.transactional))
			try (IGraphTransaction t = graph.beginTransaction()) {
				filedictionary = graph.getFileIndex();
				proxydictionary = graph.getOrCreateNodeIndex("proxydictionary");
				t.success();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	protected void delete(IGraphNode modelElement) {

		try {
			// FIXME maintain indexes (create api to remove a node from all
			// indexes???)
			modelElement.delete();
		} catch (Exception e) {
			System.err.println("DELETE NODE EXCEPTION:");
			e.printStackTrace();
		}

	}

	protected void deleteAll(String repository, String filepath)
			throws Exception {

		long start = System.currentTimeMillis();

		try (IGraphTransaction transaction = graph.beginTransaction()) {

			IGraphNode file = graph
					.getFileIndex()
					.get("id",
							repository
									+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
									+ filepath).iterator().next();

			System.out.println("deleting nodes from file: "
					+ file.getProperty("id"));

			HashSet<IGraphNode> modelElements = new HashSet<IGraphNode>();

			for (IGraphEdge rel : file.getIncomingWithType("file")) {
				modelElements.add(rel.getStartNode());
				rel.delete();
			}

			for (IGraphNode node : modelElements) {
				dereference(node);
			}

			for (IGraphNode node : modelElements) {
				makeProxyRefs(node, repository, file);
			}

			for (IGraphNode node : modelElements) {
				delete(node);
			}

			modelElements = null;

			filedictionary = graph.getFileIndex();

			filedictionary.remove(file);
			delete(file);

			transaction.success();
		}

		System.out.println("deleted all, took: "
				+ (System.currentTimeMillis() - start) / 1000 + "s"
				+ (System.currentTimeMillis() - start) / 1000 + "ms");

	}

	protected String makeRelative(String base, String extension) {

		//System.err.println(base);
		//System.err.println(extension);
		
		if(!extension.startsWith(base)) return extension;
		
		String ret = extension.substring(base.length());
		
		return ret;
		
	}

	protected String[] addToElementProxies(String[] proxies,
			String fullPathURI, String edgelabel) {

		if (proxies != null) {

			String[] ret = new String[proxies.length + 2];

			for (int i = 0; i < proxies.length; i = i + 2) {

				ret[i] = proxies[i];
				ret[i + 1] = proxies[i + 1];

			}

			ret[proxies.length] = fullPathURI;
			ret[proxies.length + 1] = edgelabel;

			proxies = null;

			return ret;

		} else {
			String[] ret = new String[2];
			ret[0] = fullPathURI;
			ret[1] = edgelabel;
			return ret;
		}
	}

	protected void makeProxyRefs(IGraphNode referencedModelElement,
			String repositoryURL, IGraphNode referencedElementFileNode) {

		// handle any incoming references (after dereference, aka other file
		// ones)
		// FIXMEdone 5-11-13 check changes work
		for (IGraphEdge rel : referencedModelElement.getIncoming()) {

			IGraphNode referencingNode = rel.getStartNode();
			String referencingNodeFileID = referencingNode
					.getOutgoingWithType("file").iterator().next().getEndNode()
					.getProperty("id").toString();
			String referencedElementFileID = (String) referencedElementFileNode
					.getProperty("id");

			// System.out.println(referencingNodeFileID+" ::: "+fileID);

			if (!referencingNodeFileID.equals(referencedElementFileID)) {

				// System.err.println("relationship is one to an object in file: "
				// + rel.getStartNode()
				// .getRelationships(
				// Direction.OUTGOING,
				// new RelationshipUtil()
				// .getNewRelationshipType("file"))
				// .iterator().next().getEndNode().getProperty("id"));

				String fullReferencedElementPathFileURI = repositoryURL
						+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
						+ referencedElementFileID;

				String fullReferencedElementPathElementURI = fullReferencedElementPathFileURI
						+ "#"
						+ referencedModelElement.getProperty("id").toString();

				Object proxies = referencingNode.getProperty("_proxyRef:"
						+ fullReferencedElementPathFileURI);

				proxies = addToElementProxies((String[]) proxies,
						fullReferencedElementPathElementURI, rel.getType());

				referencingNode.setProperty("_proxyRef:"
						+ fullReferencedElementPathFileURI, proxies);

				proxydictionary = graph.getOrCreateNodeIndex("proxydictionary");
				proxydictionary.add(referencingNode, "_proxyRef",
						fullReferencedElementPathFileURI);

				rel.delete();

			} else {
				// same file so just delete
				rel.delete();
			}

		}

	}

	protected void dereference(IGraphNode modelElement) {

		for (IGraphEdge rel : modelElement.getOutgoing()) {

			// delete derived attributes stored as nodes
			if (rel.getProperty("isDerived") != null)
				rel.getEndNode().delete();

			rel.delete();

		}

	}

	public void delete(IGraphEdge rel) {

		try {
			rel.delete();
		} catch (Exception e) {
			System.err.println("DELETE NODE EXCEPTION:");
			e.printStackTrace();
		}

	}

}
