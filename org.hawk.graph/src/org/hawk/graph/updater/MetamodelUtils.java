/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.updater;

import java.util.Iterator;

import org.hawk.core.graph.*;
import org.hawk.core.model.*;

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

			Iterator<IGraphEdge> it = node.getOutgoingWithType("typeOf")
					.iterator();

			while (it.hasNext()) {
				IGraphNode nn = it.next().getEndNode();

				// System.err.println(nn.getProperty("id").toString());
				// System.err.println(">"+type);

				if (nn.getProperty("id").toString().equals(type)) {
					found = true;
				}
			}
			tx.success();
			tx.close();
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

				ret = node.getOutgoingWithType("typeOf").iterator().next()
						.getEndNode().getProperty("id").toString();

			} catch (Exception e) {
				System.err.println("Exception in typeOfName(Node node)");

			}
			tx.success();
			tx.close();
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
					if (cl.getProperty("id")
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
						if (cl.getProperty("id").equals(metaClassName)) {
							ret = cl;
						}
					}

				}
			}
			tx.success();
			tx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}
