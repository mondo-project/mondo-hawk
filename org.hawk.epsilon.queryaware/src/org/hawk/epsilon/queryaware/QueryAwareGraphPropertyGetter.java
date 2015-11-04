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
package org.hawk.epsilon.queryaware;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.types.EolBag;
import org.eclipse.epsilon.eol.types.EolOrderedSet;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.eclipse.epsilon.eol.types.EolSet;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.AccessListener;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.GraphNodeWrapper;
import org.hawk.epsilon.emc.GraphPropertyGetter;
import org.hawk.epsilon.emc.MetamodelUtils;
import org.hawk.graph.ModelElementNode;

public class QueryAwareGraphPropertyGetter extends GraphPropertyGetter {

	private boolean broadcastAccess = false;

	// private HawkClass featureStartingNodeClass = null;
	private IGraphNode featureStartingNodeClassNode = null;

	private static AccessListener accessListener = new AccessListener();

	public QueryAwareGraphPropertyGetter(IGraphDatabase graph2, EOLQueryEngine m) {

		super(graph2, m);

		// System.err.println("hi: "+broadcastAccess);

	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object object, final String property)
			throws EolRuntimeException {

		Object ret = null;

		// System.err.println("QueryAwareGraphPropertyGetter INVOKE: " + object
		// + " ::::: " + property);

		// if (object.equals(null))
		// throw new EolRuntimeException(
		// "null object passed to neopropertygetter!");
		// else

		if (!(object instanceof GraphNodeWrapper))
			throw new EolRuntimeException(
					"a non GraphNodeWrapper object passed to QueryAwareGraphPropertyGetter!");

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNode node = graph.getNodeById(((GraphNodeWrapper) object)
					.getId());

			// System.out.println(node+":::"+property);

			// System.err.println(object+" : "+property);

			if (property.startsWith("revRefNav_")) {

				// LinkedList<?> otherNodes = new LinkedList<>();

				String property2 = property.substring(10);

				for (IGraphEdge r : node.getIncomingWithType(property2)) {

					if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {
						ret = new GraphNodeWrapper(r.getStartNode().getId()
								.toString(), m);
					} else {
						System.err.println(r.getType() + " : not containment");
						if (ret == null)
							ret = new EolBag<GraphNodeWrapper>();
						((EolBag<GraphNodeWrapper>) ret)
								.add(new GraphNodeWrapper(r.getStartNode()
										.getId().toString(), m));
					}
				}

				if (ret == null) {
					System.err.println(object);
					System.err.println(property);
					// throw new
					// EolRuntimeException("REVERSE NAVIGATION FAILED (return == null)");
					System.err
							.println("REVERSE NAVIGATION FAILED (return == null), returning null");
					// check metamodel if it can exist but is unset or
					// return
					// null?
				}

			}

			else if (property.equals("eContainer")) {

				// HawkClass o = new
				// MetamodelUtils().getTypeOfFromNode(node,m.parser);

				// o.isContained();
				// System.err.println(o.eContainingFeatureName());

				Iterable<IGraphEdge> inc = node.getIncoming();

				for (IGraphEdge r : inc) {

					if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {

						ret = new GraphNodeWrapper(r.getStartNode().getId()
								.toString(), m);

						break;

					}

				}

				if (ret == null)
					throw new EolRuntimeException("eContainer failed,\n"
							+ object + "\nis not contained");

			}

			else if (canHaveDerivedAttr(node, property)) {

				for (IGraphEdge r : node.getOutgoingWithType(property)) {
					if (ret == null)
						ret = r.getEndNode().getProperty(property);
					else
						throw new EolRuntimeException(
								"WARNING: a derived property (arity 1) -- ( "
										+ property
										+ " ) has more than 1 links in store!");
				}
				if (ret == null) {
					throw new EolRuntimeException(
							"derived attribute lookup failed for: " + object
									+ " # " + property);
				} else if (ret instanceof String
						&& ((String) ret).startsWith("_NYD##")) {
					// XXX IDEA: dynamically derive on the spot on access
					System.err.println("attribute: " + property
							+ " is NYD for node: " + node.getId());
				}

			}

			else if (canHaveMixed(node, property)) {

				// XXX support mixed features
				throw new UnsupportedOperationException(
						"mixed features in queryaware: NYI");

			}

			else if (canHaveAttr(node, property)) {

				//
				if (object instanceof QueryAwareGraphNodeWrapper) {
					ret = ((QueryAwareGraphNodeWrapper) object)
							.getAttributeValue(property);
					// System.err
					// .println("QueryAwareGraphNodeWrapper found, attribute: "
					// + property + " :: " + ret);
				}

				if (ret != null || node.getProperty(property) != null) {
					// FIXMEdone handle collections / ordered etc
					if (!(isMany(property)))
						ret = ret != null ? ret : node.getProperty(property);
					else {

						if (isOrdered(property) && isUnique(property))
							ret = new EolOrderedSet<GraphNodeWrapper>();
						else if (isOrdered(property))
							ret = new EolSequence<GraphNodeWrapper>();
						else if (isUnique(property))
							ret = new EolSet<GraphNodeWrapper>();
						else
							ret = new EolBag<GraphNodeWrapper>();

						Object[] array = ret != null ? (Object[]) ret
								: ((Object[]) node.getProperty(property));

						for (int i = 0; i < array.length; i++)
							((Collection<Object>) ret).add(array[i]);

					}
				} else
				// return new NeoIdWrapper(0L, m);
				{
					// ret = "UNSET";
					ret = null;
					// throw new EolRuntimeException("unset property");
				}

			}

			else if (canHaveRef(node, property)) {

				//
				if (object instanceof QueryAwareGraphNodeWrapper) {
					ret = ((QueryAwareGraphNodeWrapper) object)
							.getReference(property);
					// System.err
					// .println("QueryAwareGraphNodeWrapper found, reference: "
					// + property + " :: " + ret);
				}

				GraphNodeWrapper otherNode = null;
				Collection<GraphNodeWrapper> otherNodes = null;

				// FIXMEdone arity etc in metamodel
				// otherNodes = new EolBag<NeoIdWrapperDebuggable>();
				if (isMany(property)) {

					if (isOrdered(property) && isUnique(property))
						otherNodes = new EolOrderedSet<GraphNodeWrapper>();
					else if (isOrdered(property))
						otherNodes = new EolSequence<GraphNodeWrapper>();
					else if (isUnique(property))
						otherNodes = new EolSet<GraphNodeWrapper>();
					else
						otherNodes = new EolBag<GraphNodeWrapper>();

				}

				if (ret != null) {

					if (otherNodes == null) {

						otherNode = new GraphNodeWrapper(
								((Iterable<String>) ret).iterator().next(), m);

					} else {

						for (String s : (Iterable<String>) ret) {

							otherNodes.add(new GraphNodeWrapper(s, m));

						}
					}

				} else
					for (IGraphEdge r : node.getOutgoingWithType(property)) {
						if (otherNodes != null)
							otherNodes.add(new GraphNodeWrapper(r.getEndNode()
									.getId().toString(), m));
						else if (otherNode == null)
							otherNode = new GraphNodeWrapper(r.getEndNode()
									.getId().toString(), m);
						else
							throw new EolRuntimeException(
									"A relationship with arity 1 ( " + property
											+ " ) has more than 1 links");
					}

				// System.err.println(otherNodes);

				ret = otherNodes != null ? otherNodes : otherNode;

			} else {
				String error = "ERROR: ";

				try {

					error = error + "\n>>>"
							+ debug(graph, (GraphNodeWrapper) object);

					// error = error + "\nCLASS ATTRS: " + ""

					// ((EClass) r.getEObject(new MetamodelUtils()
					// .typeOfName(node).substring(
					// new MetamodelUtils().typeOfName(node)
					// .indexOf("/"))))
					// .getEAllAttributes()
					// ;

					// error = error + "\nCLASS REFS: " +

					// ((EClass) r.getEObject(new MetamodelUtils()
					// .typeOfName(node).substring(
					// new MetamodelUtils().typeOfName(node)
					// .indexOf("/"))))
					// .getEAllReferences()
					// "";

				} catch (Exception e) {
					e.printStackTrace();
				}

				// System.out.println(">>> " + ret);
				// System.out.println(object);
				// System.out.println(property + "\n-----");

				throw new EolRuntimeException(
						error
								+ "\ndoes not have property (attribute or reference):\n"
								+ property);
			}

			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// if (ret instanceof Collection<?>)
		// System.err
		// .println(Arrays.toString(((Collection<?>) ret).toArray()));
		// else
		// System.err.println(ret);

		if (broadcastAccess)
			broadcastAccess(object, property);

		return ret;

	}

	private void broadcastAccess(Object object, String property) {
		accessListener.accessed(((GraphNodeWrapper) object).getId() + "",
				property);
	}

	public void setBroadcastAccess(boolean b) {
		broadcastAccess = b;
	}

	public AccessListener getAccessListener() {
		return accessListener;
	}

	public boolean getBroadcastStatus() {
		return broadcastAccess;
	}

	private boolean canHaveDerivedAttr(IGraphNode node, String property) {

		featureStartingNodeClassNode = node
				.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)
				.iterator().next().getEndNode();

		if (featureStartingNodeClassNode.getProperty(property) != null) {
			String value = ((String[]) featureStartingNodeClassNode
					.getProperty(property))[0];
			if (value.equals("d"))
				return true;
			else if (value.equals("r") || value.equals("a"))
				return false;
		}

		System.err.println("derived property: "
				+ property
				+ " not found in metamodel for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;

	}

	private boolean canHaveMixed(IGraphNode node, String property) {

		featureStartingNodeClassNode = node
				.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)
				.iterator().next().getEndNode();

		if (featureStartingNodeClassNode.getProperty(property) != null) {
			String value = ((String[]) featureStartingNodeClassNode
					.getProperty(property))[0];
			if (value.equals("m"))
				return true;
			else if (value.equals("r") || value.equals("d")
					|| value.equals("a"))
				return false;
		}

		System.err.println("property: "
				+ property
				+ " not found in metamodel for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;

	}

	private boolean canHaveAttr(IGraphNode node, String property) {

		featureStartingNodeClassNode = node
				.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)
				.iterator().next().getEndNode();

		if (featureStartingNodeClassNode.getProperty(property) != null) {
			String value = ((String[]) featureStartingNodeClassNode
					.getProperty(property))[0];
			if (value.equals("a"))
				return true;
			else if (value.equals("r") || value.equals("d"))
				return false;
		}

		System.err.println("property: "
				+ property
				+ " not found in metamodel for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;

		// featureStartingNodeClass = new
		// MetamodelUtils().getTypeOfFromNode(node, m.parser);

		// return featureStartingNodeClass != null &&
		// featureStartingNodeClass.getEStructuralFeature(property) != null &&
		// featureStartingNodeClass.getEStructuralFeature(property) instanceof
		// HawkAttribute;

	}

	private boolean canHaveRef(IGraphNode node, String property) {

		featureStartingNodeClassNode = node
				.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)
				.iterator().next().getEndNode();

		if (featureStartingNodeClassNode.getProperty(property) != null) {
			String value = ((String[]) featureStartingNodeClassNode
					.getProperty(property))[0];
			if (value.equals("r"))
				return true;
			else if (value.equals("a") || value.equals("d"))
				return false;
		}

		System.err.println("reference: "
				+ property
				+ " not found in metamodel for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
		// System.out.println(o != null && o.getEStructuralFeature(property) !=
		// null
		// && o.getEStructuralFeature(property) instanceof EReference);
		return false;

		// featureStartingNodeClass = new
		// MetamodelUtils().getTypeOfFromNode(node, m.parser);

		// return featureStartingNodeClass != null &&
		// featureStartingNodeClass.getEStructuralFeature(property) != null &&
		// featureStartingNodeClass.getEStructuralFeature(property) instanceof
		// HawkReference;

	}

	private boolean isMany(String ref) {
		if (featureStartingNodeClassNode.getProperty(ref) != null) {
			// System.err.println(referenceStartingNodeClassNode.getProperty(ref).toString());
			return ((String[]) featureStartingNodeClassNode.getProperty(ref))[1]
					.equals("t");
		}
		System.err.println("reference: "
				+ ref
				+ " not found in metamodel (isMany) for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;
		// return featureStartingNodeClass.getEStructuralFeature(ref).isMany();
	}

	private boolean isOrdered(String ref) {
		if (featureStartingNodeClassNode.getProperty(ref) != null)
			return ((String[]) featureStartingNodeClassNode.getProperty(ref))[2]
					.equals("t");
		System.err.println("reference: "
				+ ref
				+ " not found in metamodel (isOrdered) for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;
		// return ((HawkStructuralFeature) featureStartingNodeClass
		// .getEStructuralFeature(ref)).isOrdered();
	}

	private boolean isUnique(String ref) {
		if (featureStartingNodeClassNode.getProperty(ref) != null)
			return ((String[]) featureStartingNodeClassNode.getProperty(ref))[3]
					.equals("t");
		System.err.println("reference: "
				+ ref
				+ " not found in metamodel (isUnique) for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		return false;
		// return
		// featureStartingNodeClass.getEStructuralFeature(ref).isUnique();
	}

	public String debug(IGraphDatabase graph, GraphNodeWrapper object) {

		IGraphNode node = graph.getNodeById(object.getId());

		String ret = node.toString();

		for (String p : node.getPropertyKeys()) {

			Object n = node.getProperty(p);
			String temp = "error: " + n.getClass();
			if (n instanceof int[])
				temp = Arrays.toString((int[]) n);
			if (n instanceof long[])
				temp = Arrays.toString((long[]) n);
			if (n instanceof String[])
				temp = Arrays.toString((String[]) n);
			if (n instanceof boolean[])
				temp = Arrays.toString((boolean[]) n);

			ret = ret
					+ ("["
							+ p
							+ ";"
							+ (((p.equals("class") || p.equals("superclass"))) ? (temp
									.length() < 1000 ? temp
									: "[<TOO LONG TO LOG (>1000chars)>]") : (n)) + "] ");
		}

		Collection<String> refs = new HashSet<String>();

		for (IGraphEdge r : node.getOutgoing()) {

			refs.add(r.getType().toString());

		}

		return ret + "\nOF TYPE: " + new MetamodelUtils().typeOfName(node)

		+ "\nWITH OUTGOING REFERENCES: " + refs;
	}

}
