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
package org.hawk.epsilon.emc;

import java.util.Iterator;

import org.hawk.core.graph.*;
import org.hawk.core.model.*;

public class MetamodelUtils {

	/**
	 * SLOW - ONLY USE FOR ERRORS
	 * 
	 * @param node
	 * @return the class (kindOf) of the modelElement as a String
	 */
	// public String kindOfName(Node node) {
	//
	// String s = "";
	// ArrayList<String> ar = new ArrayList<String>();
	//
	// Iterator<Relationship> it = node.getRelationships(Direction.OUTGOING,
	// new RelationshipType() {
	//
	// @Override
	// public String name() {
	// return "kindOf";
	// }
	// }).iterator();
	//
	// while (it.hasNext()) {
	// Node nn = it.next().getOtherNode(node);
	// ar.add(nn.getProperty("id").toString());
	// }
	//
	// for (String ss : ar)
	// s += ss + "\n\t\t\t";
	//
	// return s.trim();
	//
	// }

	// public boolean isOfType(Node node, String type) {
	//
	// return node.getRelationships(Direction.OUTGOING,
	// new RelationshipUtil().getNewRelationshipType("typeOf"))
	// .iterator().next().getEndNode().getProperty("id").toString().equals(type);
	//
	// }

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

	// public HawkClass getClassFromNode(Node n, ModelParser p) throws Exception
	// {
	//
	// HawkClass o = null;
	//
	// try (Transaction tx = n.getGraphDatabase().beginTx()) {
	// // operations on the graph
	// // ...
	//
	// for (Relationship r2 : n.getRelationships(Direction.OUTGOING,
	// new RelationshipType() {
	// @Override
	// public String name() {
	// return "epackage";
	// }
	// })) {
	//
	// o = p.getEPackageRegistryInstance()
	// .getPackage(
	// r2.getOtherNode(n).getProperty("id").toString())
	// .getEClassifier(n.getProperty("id").toString());
	// }
	// tx.success();
	// tx.close();
	// }
	// return o;
	// }

	// public HawkClass getTypeOfFromNode(Node n, ModelParser p) {
	//
	// String clas = new MetamodelUtils().typeOfName(n);
	//
	// if (clas == null) {
	// return null;
	// }
	//
	// // Iterator<Relationship> it = n.getRelationships().iterator();
	// // while(it.hasNext())
	// // System.out.println(it.next().getType());
	//
	// // System.out.println(clas);
	//
	// HawkClass o = null;
	//
	// //long curr = System.currentTimeMillis();
	//
	// try (Transaction tx = n.getGraphDatabase().beginTx()) {
	// // operations on the graph
	// // ...
	//
	// Node scn = n
	// .getRelationships(Direction.OUTGOING,
	// new RelationshipType() {
	// @Override
	// public String name() {
	// return "typeOf";
	// }
	// }).iterator().next().getEndNode();
	//
	// // System.err.println(clas);
	//
	// try {
	//
	// o = p.getEPackageRegistryInstance()
	// .getPackage(
	// scn.getRelationships(Direction.OUTGOING,
	// new RelationshipType() {
	// @Override
	// public String name() {
	// return "epackage";
	// }
	// }).iterator().next().getEndNode()
	// .getProperty("id").toString())
	// .getEClassifier(clas);
	//
	// //Neo4JEpsilonModel.time += System.currentTimeMillis() - curr;
	//
	// } catch (Exception e) {
	// System.out.println(p.getEPackageRegistryInstance().keySet());
	// // System.out.println(r2);
	// System.out.println(scn);
	// System.out.println(clas);
	// // System.out.println(r2.getOtherNode(scn).getProperty("id").toString());
	// e.printStackTrace();
	// }
	//
	// // System.out.println(o);
	//
	// // for (EObject pack : ) {
	// //
	// // if (((EPackage) pack).getNsURI().equals(
	// // clas.substring(0, clas.indexOf("/")))) {
	// // o = (EClass) ((EPackage) pack).getEClassifier(clas
	// // .substring(clas.indexOf("/") + 1));
	// // }
	// // }
	//
	// tx.success();
	// }
	//
	// return o;
	// }

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
				IHawkIterable<IGraphNode> eps = epackagedictionary.query("id",
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
