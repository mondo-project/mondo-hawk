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

import java.util.Iterator;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkClass;
import org.hawk.graph.ModelElementNode;

public class MetamodelUtils {

	public static String eClassNSURI(IHawkClass e) {
		// return e.getEPackage().getNsURI() + "/" +
		return e.getName();
	}

	public static boolean isOfType(IGraphNode node, String type) {

		boolean found = false;

		try (IGraphTransaction tx = node.getGraph().beginTransaction()) {
			// operations on the graph
			// ...

			Iterator<IGraphEdge> it = node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)
					.iterator();

			while (it.hasNext()) {
				IGraphNode nn = it.next().getEndNode();

				// System.err.println(nn.getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString());
				// System.err.println(">"+type);

				if (nn.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString().equals(type)) {
					found = true;
				}
			}
			tx.success();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return found;

	}

	public String eClassFullNSURI(IHawkClass e) {
		return e.getPackageNSURI() + "/" + e.getName();
	}

	/**
	 * 
	 * @param node
	 * @return the class (typeOf) of the modelElement as a String
	 * @throws Exception
	 */
	public String typeOfName(IGraphNode node) {

		String ret = null;

		try (IGraphTransaction tx = node.getGraph().beginTransaction()) {
			// operations on the graph
			// ...

			try {

				ret = node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().next()
						.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();

			} catch (Exception e) {
				System.err.println("Exception in typeOfName(Node node)");

			}
			tx.success();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public IGraphNode getClassNode(IGraphDatabase graph, String metaClassName) {

		IGraphNode cl = null;
		IGraphNode ret = null;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNodeIndex epackagedictionary = graph.getMetamodelIndex();

			IGraphNode ep = null;

			if (metaClassName.contains("::")) {
				ep = epackagedictionary.get("id", metaClassName).getSingle();
				for (IGraphEdge r : ep.getIncomingWithType("epackage")) {

					cl = r.getStartNode();
					if (cl.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
							.equals(metaClassName.substring(metaClassName
									.indexOf("::") + 2))) {
						ret = cl;
					}
				}
			} else {
				IGraphIterable<IGraphNode> eps = epackagedictionary.query("id",
						"*");
				for (IGraphNode epp : eps) {
					for (IGraphEdge r : epp.getIncomingWithType("epackage")) {

						cl = r.getStartNode();
						if (cl.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(metaClassName)) {
							ret = cl;
						}
					}

				}
			}
			tx.success();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}
