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
package org.hawk.epsilon.emc;

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
	// return ModelElementNode.EDGE_LABEL_OFKIND;
	// }
	// }).iterator();
	//
	// while (it.hasNext()) {
	// Node nn = it.next().getOtherNode(node);
	// ar.add(nn.getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString());
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
	// new RelationshipUtil().getNewRelationshipType(ModelElementNode.EDGE_LABEL_OFTYPE))
	// .iterator().next().getEndNode().getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString().equals(type);
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
	// r2.getOtherNode(n).getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString())
	// .getEClassifier(n.getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString());
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
	// return ModelElementNode.EDGE_LABEL_OFTYPE;
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
	// .getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString())
	// .getEClassifier(clas);
	//
	// //Neo4JEpsilonModel.time += System.currentTimeMillis() - curr;
	//
	// } catch (Exception e) {
	// System.out.println(p.getEPackageRegistryInstance().keySet());
	// // System.out.println(r2);
	// System.out.println(scn);
	// System.out.println(clas);
	// // System.out.println(r2.getOtherNode(scn).getProperty(GraphWrapper.IDENTIFIER_PROPERTY).toString());
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
