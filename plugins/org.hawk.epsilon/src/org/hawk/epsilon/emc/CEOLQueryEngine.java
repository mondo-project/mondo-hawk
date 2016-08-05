/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - extract queries into GraphWrapper
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;

public class CEOLQueryEngine extends EOLQueryEngine {

	Set<IGraphNode> files = null;
	protected boolean enableTraversalScoping = true;

	public void setContext(Map<String, Object> context) {

		final GraphWrapper gw = new GraphWrapper(graph);
		String sFilePatterns = null;
		String sRepoPatterns = null;

		if (context != null) {
			sFilePatterns = (String) context.get(PROPERTY_FILECONTEXT);
			sRepoPatterns = (String)context.get(PROPERTY_REPOSITORYCONTEXT);
			setDefaultNamespaces((String) context.get(PROPERTY_DEFAULTNAMESPACES));
			String etss = (String)context.get(PROPERTY_ENABLE_TRAVERSAL_SCOPING);
			if (etss != null)
				enableTraversalScoping = Boolean.parseBoolean(etss);
		}

		System.err.println(sFilePatterns);
		System.err.println(sRepoPatterns);
		System.err.println(defaultnamespaces);

		// if (sFilePatterns != null) {
		final String[] filePatterns = (sFilePatterns != null && sFilePatterns.trim().length() != 0)
				? sFilePatterns.split(",") : null;
		final String[] repoPatterns = (sRepoPatterns != null && sRepoPatterns.trim().length() != 0)
				? sRepoPatterns.split(",") : null;

		this.files = new HashSet<>();

		List<String> fplist = (filePatterns != null) ? Arrays.asList(filePatterns) : null;
		List<String> rplist = (repoPatterns != null) ? Arrays.asList(repoPatterns) : null;

		try (IGraphTransaction tx = graph.beginTransaction()) {

			final Set<FileNode> fileNodes = gw.getFileNodes(rplist, fplist);
			for (FileNode fn : fileNodes) {
				this.files.add(fn.getNode());
			}

			System.out.println("running CEOLQueryEngine with files: " + fileNodes);
			if (enableTraversalScoping)
				System.out.println("Full Traversal Scoping ENABLED");

			if (propertygetter == null)
				propertygetter = new CGraphPropertyGetter(graph, this);

			name = (String) context.get(EOLQueryEngine.PROPERTY_NAME);
			if (name == null)
				name = "Model";

			// defaults to true
			// String ec = context.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
			// enableCache = ec == null ? true : ec.equalsIgnoreCase("true");
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * NeoIdWrapper objects, limited by the files in scope.
	 */
	@Override
	public Collection<?> allContents() {
		final Set<GraphNodeWrapper> allContents = new HashSet<GraphNodeWrapper>();

		for (IGraphNode rawFileNode : files) {
			final FileNode f = new FileNode(rawFileNode);
			for (ModelElementNode me : f.getModelElements()) {
				GraphNodeWrapper wrapper = new GraphNodeWrapper(me.getNode(), this);
				allContents.add(wrapper);
			}
		}

		return allContents;
	}

	@Override
	public Collection<Object> getAllOf(IGraphNode typeNode, final String typeorkind) {
		OptimisableCollection nodes = new OptimisableCollection(this, new GraphNodeWrapper(typeNode, this));

		// operations on the graph
		// ...

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {

			IGraphNode node = n.getStartNode();

			// System.err.println(Arrays.toString(files.toArray()));
			// System.err.println(files.iterator().next().getGraph());
			// System.err.println(node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE).iterator().next().getEndNode().getGraph());

			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {

				if (files.contains(e.getEndNode())) {
					nodes.add(new GraphNodeWrapper(node, this));
				}
			}
		}
		return nodes;
	}
}
