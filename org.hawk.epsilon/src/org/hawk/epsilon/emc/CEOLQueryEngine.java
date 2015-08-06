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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;

public class CEOLQueryEngine extends EOLQueryEngine {

	Set<IGraphNode> files = null;

	@Override
	public void load(IGraphDatabase g) throws EolModelLoadingException {
		super.load(g);
		load(config);
	}

	public void load(StringProperties config) {
		Map<String, String> m = new HashMap<String, String>();
		for (Object s : config.keySet())
			m.put(((String) (s)), config.getProperty((String) (s)));
		load(m);
	}

	public void load(Map<String, String> context) {
		final GraphWrapper gw = new GraphWrapper(graph);
		String sFilePatterns = null;
		String sRepoPattern = null;

		if (context != null) {
			sFilePatterns = context.get(PROPERTY_FILECONTEXT);
			sRepoPattern = context.get(PROPERTY_REPOSITORYCONTEXT);
			sRepoPattern = (sRepoPattern!=null&&sRepoPattern.trim().length()!=0)?sRepoPattern.trim():null;
		}

		System.err.println(sFilePatterns);
		System.err.println(sRepoPattern);
		
		//if (sFilePatterns != null) {
			final String[] filePatterns = (sFilePatterns != null&&sFilePatterns.trim().length()!=0)?sFilePatterns.split(","):null;
			try (IGraphTransaction tx = graph.beginTransaction()) {
				this.files = new HashSet<>();
				
				List<String> fplist = (filePatterns!=null)?Arrays.asList(filePatterns):null;
				
				final Set<FileNode> fileNodes = gw.getFileNodes(sRepoPattern, 
						fplist);
				for (FileNode fn : fileNodes) {
					this.files.add(fn.getNode());
				}

				System.out.println("running CEOLQueryEngine with files: " + fileNodes);
			} catch (Exception e) {
				System.err.println("internal error trying to retreive file nodes for contextfullQuery");
				e.printStackTrace();
			}

			if (propertygetter == null)
				propertygetter = new GraphPropertyGetter(graph, this);

			name = context.get(EOLQueryEngine.PROPERTY_NAME);
			if (name == null)
				name = "Model";

			// defaults to true
			// String ec = context.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
			// enableCache = ec == null ? true : ec.equalsIgnoreCase("true");

			// limit to declared packages if applicable
			String pa = context.get(EOLQueryEngine.PROPERTY_METAMODELS);
			if (pa != null) {
				String[] eps = ((String) pa).split(",");

				if (!(eps.length == 1 && eps[0].equals("[]"))) {

					epackages = new HashSet<String>();

					for (String s : eps) {
						// System.err.println(s);
						epackages.add(s.trim().replaceAll("\\[", "")
								.replaceAll("]", "").trim());
					}
				}
			}

			try (IGraphTransaction tx = graph.beginTransaction()) {

				epackagedictionary = graph.getMetamodelIndex();

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
		try(IGraphTransaction tx = graph.beginTransaction()) {
			for (IGraphNode rawFileNode : files) {
				final FileNode f = new FileNode(rawFileNode);
				for (ModelElementNode me : f.getModelElements()) {
					GraphNodeWrapper wrapper = new GraphNodeWrapper(me.getNode().getId().toString(), this);
					allContents.add(wrapper);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return allContents;
	}

	public Collection<Object> getAllOf(String arg0, final String typeorkind)
			throws EolModelElementTypeNotFoundException {

		try {

			if (hasType(arg0)) {

				IGraphNode typeNode = null;

				if (arg0.contains("::")) {

					String ep = arg0.substring(0, arg0.indexOf("::"));

					IGraphNode pack = null;

					try (IGraphTransaction tx = graph.beginTransaction()) {
						// operations on the graph
						// ...

						pack = epackagedictionary.get("id", ep).getSingle();

						tx.success();
						tx.close();
					}

					for (IGraphEdge r : pack.getIncomingWithType("epackage")) {

						IGraphNode othernode = r.getStartNode();
						if (othernode.getProperty("id").equals(
								arg0.substring(arg0.indexOf("::") + 2))) {
							typeNode = othernode;
							break;
						}

					}

				} else {

					if (epackages == null) {

						try (IGraphTransaction tx = graph.beginTransaction()) {
							// operations on the graph
							// ...

							Iterator<IGraphNode> packs = epackagedictionary
									.query("id", "*").iterator();
							LinkedList<IGraphNode> possibletypenodes = new LinkedList<IGraphNode>();

							while (packs.hasNext()) {

								IGraphNode pack = packs.next();
								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {

										possibletypenodes.add(othernode);

									}
								}
							}

							if (possibletypenodes.size() == 1)
								typeNode = possibletypenodes.getFirst();
							else
								throw new EolModelElementTypeNotFoundException(
										this.getName(),
										possibletypenodes.size()
												+ " CLASSES FOUND FOR: " + arg0);

							tx.success();
							tx.close();
						}

					} else {

						for (String p : epackages) {

							try (IGraphTransaction tx = graph
									.beginTransaction()) {
								// operations on the graph
								// ...

								IGraphNode pack = epackagedictionary.get("id",
										p).getSingle();
								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {

										typeNode = othernode;
										break;

									}
								}

								tx.success();
								tx.close();
							}
						}

					}
				}

				COptimisableCollection nodes = new COptimisableCollection(
						this,
						new GraphNodeWrapper(typeNode.getId().toString(), this),
						files);

				if (typeNode != null) {

					try (IGraphTransaction tx = graph.beginTransaction()) {
						// operations on the graph
						// ...

						for (IGraphEdge n : typeNode
								.getIncomingWithType(typeorkind)) {

							IGraphNode node = n.getStartNode();

							// System.err.println(Arrays.toString(files.toArray()));
							// System.err.println(files.iterator().next().getGraph());
							// System.err.println(node.getOutgoingWithType("file").iterator().next().getEndNode().getGraph());

							if (files.contains(node.getOutgoingWithType("file")
									.iterator().next().getEndNode())) {
								nodes.add(new GraphNodeWrapper(node.getId()
										.toString(), this));
							}
						}
						tx.success();
						tx.close();
					}
				}

				return nodes;

			} else
				throw new EolModelElementTypeNotFoundException(this.getName(),
						arg0);

		} catch (Exception e) {
			e.printStackTrace();
			throw new EolModelElementTypeNotFoundException(this.getName(), arg0);

		}
	}
}
