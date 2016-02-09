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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.introspection.AbstractPropertyGetter;
import org.eclipse.epsilon.eol.types.EolBag;
import org.eclipse.epsilon.eol.types.EolOrderedSet;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.eclipse.epsilon.eol.types.EolSet;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;

public class GraphPropertyGetter extends AbstractPropertyGetter {

	protected static final int IDX_FLAG_MANY = 1;
	protected static final int IDX_FLAG_ORDERED = 2;
	protected static final int IDX_FLAG_UNIQUE = 3;

	protected static enum PropertyType {
		ATTRIBUTE, DERIVED, REFERENCE, MIXED, INVALID;
		static PropertyType fromCharacter(String s) {
			switch (s) {
			case "d":
				return DERIVED;
			case "r":
				return REFERENCE;
			case "a":
				return ATTRIBUTE;
			case "m":
				return MIXED;
			default:
				return INVALID;
			}
		}
	}

	protected boolean broadcastAccess = false;

	protected IGraphDatabase graph;
	protected EOLQueryEngine m;
	protected IGraphNode featureStartingNodeClassNode = null;

	protected static AccessListener accessListener = new AccessListener();

	public GraphPropertyGetter(IGraphDatabase graph2, EOLQueryEngine m) {
		graph = graph2;
		this.m = m;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object object, final String property)
			throws EolRuntimeException {

		Object ret = null;

		// System.err.println("GraphPropertyGetter INVOKE: "+object+" ::::: "+property);

		// if (object.equals(null))
		// throw new EolRuntimeException(
		// "null object passed to neopropertygetter!");
		// else

		if (!(object instanceof GraphNodeWrapper))
			throw new EolRuntimeException(
					"a non GraphNodeWrapper object passed to GraphPropertyGetter!");

		// try (IGraphTransaction tx = graph.beginTransaction()) {
		// operations on the graph
		// ...

		IGraphNode node = graph
				.getNodeById(((GraphNodeWrapper) object).getId());

		// System.out.println(node+":::"+property);
		// System.err.println(object+" : "+property);

		// avoid using switch (in the outer-most structure) to allow partial
		// matches and method calls in alternatives
		if (property.equals("hawkFile")) {

			String sep = "";
			StringBuilder buff = new StringBuilder(32);
			for (IGraphEdge e : node
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				buff.append(sep);
				buff.append(e.getEndNode()
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.toString());
				sep = ";";

			}
			ret = buff.toString();

		} else if (property.equals("hawkRepo")) {

			String sep = "";
			StringBuilder buff = new StringBuilder(32);
			for (IGraphEdge e : node
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				buff.append(sep);
				buff.append(e.getEndNode()
						.getProperty(FileNode.PROP_REPOSITORY).toString());
				sep = ";";

			}
			ret = buff.toString();

		} else if (property.equals("hawkFiles")) {

			Set<String> files = new HashSet<>();
			for (IGraphEdge e : node
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE))
				files.add(e.getEndNode()
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.toString());

			ret = files;

		} else if (property.equals("hawkRepos")) {

			Set<String> repos = new HashSet<>();
			for (IGraphEdge e : node
					.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE))
				repos.add(e.getEndNode().getProperty(FileNode.PROP_REPOSITORY)
						.toString());

			ret = repos;

		} else if (property.startsWith("revRefNav_")) {

			// LinkedList<?> otherNodes = new LinkedList<>();

			String property2 = property.substring(10);

			boolean ismany = isMany(property2);

			for (IGraphEdge r : node.getIncomingWithType(property2)) {

				if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {
					ret = new GraphNodeWrapper(r.getStartNode().getId()
							.toString(), m);
					break;
				} else {
					System.err.println("warning: " + r.getType()
							+ " : not containment");
					if (!ismany) {
						if (ret == null)
							ret = new GraphNodeWrapper(r.getStartNode().getId()
									.toString(), m);
						else
							throw new EolRuntimeException(
									"A relationship with arity 1 ( " + property
											+ " ) has more than 1 links");
					} else {
						if (ret == null)
							ret = new EolBag<GraphNodeWrapper>();
						((EolBag<GraphNodeWrapper>) ret)
								.add(new GraphNodeWrapper(r.getStartNode()
										.getId().toString(), m));
					}
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

		} else if (property.equals("eContainer")) {

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
				throw new EolRuntimeException("eContainer failed,\n" + object
						+ "\nis not contained");

		} else if (property.equals("eContents")) {
			final List<GraphNodeWrapper> results = new ArrayList<>();
			Iterable<IGraphEdge> out = node.getOutgoing();
			for (IGraphEdge r : out) {
				if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {
					final Object endNodeID = r.getEndNode().getId();
					results.add(new GraphNodeWrapper(endNodeID.toString(), m));
				}
			}
			ret = results;
		} else if (property.equals("hawkIn") || property.equals("hawkOut")) {
			final boolean isIncoming = property.equals("hawkIn");
			final List<GraphNodeWrapper> results = new ArrayList<>();
			final Iterable<IGraphEdge> edges = isIncoming ? node.getIncoming()
					: node.getOutgoing();
			for (IGraphEdge r : edges) {
				if (ModelElementNode.TRANSIENT_EDGE_LABELS
						.contains(r.getType())) {
					continue;
				}
				final IGraphNode edgeNode = isIncoming ? r.getStartNode() : r
						.getEndNode();
				final Object edgeNodeID = edgeNode.getId();
				final GraphNodeWrapper edgeNodeWrapper = new GraphNodeWrapper(
						edgeNodeID.toString(), m);
				results.add(edgeNodeWrapper);
			}
			ret = results;
		} else if (canHaveDerivedAttr(node, property)) {

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

		} else if (canHaveMixed(node, property)) {

			ret = getCollectionForProperty(property);

			final Collection<Object> retCollection = (Collection<Object>) ret;

			if (node.getProperty(property) != null) {

				final List<Object> values = Arrays.asList((Object[]) node
						.getProperty(property));
				retCollection.addAll(values);

			}

			for (IGraphEdge r : node.getOutgoingWithType(property))
				retCollection.add(new GraphNodeWrapper(r.getEndNode().getId()
						.toString(), m));

		} else if (canHaveAttr(node, property)) {

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

		} else if (canHaveRef(node, property)) {

			GraphNodeWrapper otherNode = null;
			Collection<GraphNodeWrapper> otherNodes = null;

			// FIXMEdone arity etc in metamodel
			// otherNodes = new EolBag<NeoIdWrapperDebuggable>();
			if (isMany(property)) {
				otherNodes = getCollectionForProperty(property);
			}

			for (IGraphEdge r : node.getOutgoingWithType(property)) {
				if (otherNodes != null)
					otherNodes.add(new GraphNodeWrapper(r.getEndNode().getId()
							.toString(), m));
				else if (otherNode == null)
					otherNode = new GraphNodeWrapper(r.getEndNode().getId()
							.toString(), m);
				else
					throw new EolRuntimeException(
							"A relationship with arity 1 ( " + property
									+ " ) has more than 1 links");
			}

			ret = otherNodes != null ? otherNodes : otherNode;

		} else {
			throw new EolIllegalPropertyException(object, property, ast,
					context);
		}

		if (broadcastAccess)
			broadcastAccess(object, property);

		return ret;

	}

	protected Collection<GraphNodeWrapper> getCollectionForProperty(
			final String property) {
		if (isOrdered(property) && isUnique(property))
			return new EolOrderedSet<GraphNodeWrapper>();
		else if (isOrdered(property))
			return new EolSequence<GraphNodeWrapper>();
		else if (isUnique(property))
			return new EolSet<GraphNodeWrapper>();
		else
			return new EolBag<GraphNodeWrapper>();
	}

	protected void broadcastAccess(Object object, String property) {
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

	protected boolean canHaveDerivedAttr(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.DERIVED);
	}

	protected boolean canHaveMixed(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.MIXED);
	}

	protected boolean canHaveAttr(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.ATTRIBUTE);
	}

	protected boolean canHaveRef(IGraphNode node, String property) {
		return canHavePropertyWithType(node, property, PropertyType.REFERENCE);
	}

	protected boolean isMany(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_MANY);
	}

	protected boolean isOrdered(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_ORDERED);
	}

	protected boolean isUnique(String ref) {
		return isTypeFlagActive(ref, IDX_FLAG_UNIQUE);
	}

	protected boolean canHavePropertyWithType(IGraphNode node, String property,
			PropertyType expected) {
		final Iterator<IGraphEdge> itTypeOf = node.getOutgoingWithType(
				ModelElementNode.EDGE_LABEL_OFTYPE).iterator();

		if (itTypeOf.hasNext()) {
			featureStartingNodeClassNode = itTypeOf.next().getEndNode();

			String value = "_null_hawk_value_error";

			if (featureStartingNodeClassNode.getProperty(property) != null)
				value = ((String[]) featureStartingNodeClassNode
						.getProperty(property))[0];

			final PropertyType actual = PropertyType.fromCharacter(value);
			if (actual == expected) {
				return true;
			} else if (actual != PropertyType.INVALID) {
				return false;
			} else {
				System.err
						.println("property: "
								+ property
								+ " not found in metamodel for type: "
								+ featureStartingNodeClassNode
										.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
			}
		} else {
			System.err.println("type not found for node " + node);
		}

		return false;
	}

	protected boolean isTypeFlagActive(String reference, final int index) {
		if (featureStartingNodeClassNode == null) {
			System.err.println("type not found previously for " + reference);
			return false;
		}
		if (featureStartingNodeClassNode.getProperty(reference) != null) {
			// System.err.println(referenceStartingNodeClassNode.getProperty(ref).toString());
			return ((String[]) featureStartingNodeClassNode
					.getProperty(reference))[index].equals("t");
		}
		System.err.println("reference: "
				+ reference
				+ " not found in metamodel (isMany) for type: "
				+ featureStartingNodeClassNode
						.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

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
