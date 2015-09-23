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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.introspection.AbstractPropertyGetter;
import org.eclipse.epsilon.eol.types.EolBag;
import org.eclipse.epsilon.eol.types.EolOrderedSet;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.eclipse.epsilon.eol.types.EolSet;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.ModelElementNode;

public class GraphPropertyGetter extends AbstractPropertyGetter {

	private static final int IDX_FLAG_MANY = 1;
	private static final int IDX_FLAG_ORDERED = 2;
	private static final int IDX_FLAG_UNIQUE = 3;

	private static enum PropertyType {
		ATTRIBUTE, DERIVED, REFERENCE, INVALID;
		static PropertyType fromCharacter(String s) {
			switch (s) {
			case "d": return DERIVED;
			case "r": return REFERENCE;
			case "a": return ATTRIBUTE;
			default: return INVALID;
			}
		}
	}

	private boolean broadcastAccess = false;

	protected IGraphDatabase graph;
	protected EOLQueryEngine m;
	private IGraphNode featureStartingNodeClassNode = null;

	private static AccessListener accessListener = new AccessListener();

	public GraphPropertyGetter(IGraphDatabase graph2, EOLQueryEngine m) {
		graph = graph2;
		this.m = m;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object object, final String property)
			throws EolRuntimeException {

		Object ret = null;

		//System.err.println("GraphPropertyGetter INVOKE: "+object+" ::::: "+property);

		// if (object.equals(null))
		// throw new EolRuntimeException(
		// "null object passed to neopropertygetter!");
		// else

		if (!(object instanceof GraphNodeWrapper))
			throw new EolRuntimeException(
					"a non GraphNodeWrapper object passed to GraphPropertyGetter!");

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

					if (r.getProperty("isContainment") != null) {
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

					if (r.getProperty("isContainment") != null) {

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

			else if (canHaveAttr(node, property)) {

				if (node.getProperty(property) != null) {
					// FIXMEdone handle collections / ordered etc
					if (!(isMany(property)))
						ret = node.getProperty(property);
					else {

						ret = getCollectionForProperty(property);

						Object[] array = ((Object[]) node.getProperty(property));

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

				GraphNodeWrapper otherNode = null;
				Collection<GraphNodeWrapper> otherNodes = null;

				// FIXMEdone arity etc in metamodel
				// otherNodes = new EolBag<NeoIdWrapperDebuggable>();
				if (isMany(property)) {
					otherNodes = getCollectionForProperty(property);
				}

				for (IGraphEdge r : node.getOutgoingWithType(property)) {
					if (otherNodes != null)
						otherNodes.add(new GraphNodeWrapper(r.getEndNode()
								.getId().toString(), m));
					else if (otherNode == null)
						otherNode = new GraphNodeWrapper(r.getEndNode().getId()
								.toString(), m);
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
			tx.close();
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

	private Collection<GraphNodeWrapper> getCollectionForProperty(final String property) {
		if (isOrdered(property) && isUnique(property))
			return new EolOrderedSet<GraphNodeWrapper>();
		else if (isOrdered(property))
			return new EolSequence<GraphNodeWrapper>();
		else if (isUnique(property))
			return new EolSet<GraphNodeWrapper>();
		else
			return new EolBag<GraphNodeWrapper>();
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

	public IGraphDatabase getGraph() {
		return graph;
	}

	public boolean getBroadcastStatus() {
		return broadcastAccess;
	}

	private boolean canHaveDerivedAttr(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.DERIVED);
	}

	private boolean canHaveAttr(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.ATTRIBUTE);
	}

	private boolean canHaveRef(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.REFERENCE);
	}

	private boolean isMany(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_MANY);
	}

	private boolean isOrdered(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_ORDERED);
	}

	private boolean isUnique(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_UNIQUE);
	}

	private boolean canHavePropertyWithType(IGraphNode node, String property, PropertyType expected) {
		final Iterator<IGraphEdge> itTypeOf = node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator();

		if (itTypeOf.hasNext()) {
			featureStartingNodeClassNode = itTypeOf.next().getEndNode();
			final String value = ((String[]) featureStartingNodeClassNode.getProperty(property))[0];
			final PropertyType actual = PropertyType.fromCharacter(value);
			if (actual == expected) {
				return true;
			} else if (actual != PropertyType.INVALID) {
				return false;
			} else {
				System.err.println("property: " + property
					+ " not found in metamodel for type: "
					+ featureStartingNodeClassNode.getProperty("id"));
			}
		}
		else {
			System.err.println("type not found for node " + node);
		}

		return false;
	}

	private boolean isTypeFlagActive(String reference, final int index) {
		if (featureStartingNodeClassNode == null) {
			System.err.println("type not found previously for " + reference);
			return false;
		}
		if (featureStartingNodeClassNode.getProperty(reference) != null) {
			// System.err.println(referenceStartingNodeClassNode.getProperty(ref).toString());
			return ((String[]) featureStartingNodeClassNode.getProperty(reference))[index]
					.equals("t");
		}
		System.err.println("reference: " + reference
				+ " not found in metamodel (isMany) for type: "
				+ featureStartingNodeClassNode.getProperty("id"));

		return false;
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
