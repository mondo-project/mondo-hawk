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
import java.util.List;

import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.types.EolBag;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.ModelElementNode;

public class CGraphPropertyGetter extends GraphPropertyGetter {

	private final CEOLQueryEngine engine;

	public CGraphPropertyGetter(IGraphDatabase graph2, CEOLQueryEngine m) {
		super(graph2, m);
		System.out.println("CGraphPropertyGetter created.");
		engine = m;
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

		if (property.startsWith("revRefNav_")) {

			// LinkedList<?> otherNodes = new LinkedList<>();

			String property2 = property.substring(10);

			boolean ismany = isMany(property2);

			for (IGraphEdge r : node.getIncomingWithType(property2)) {

				IGraphNode n = r.getStartNode();

				if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {
					ret = addIfInScope(n);
					break;
				} else {
					System.err.println("warning: " + r.getType()
							+ " : not containment");
					if (!ismany) {
						if (ret == null)
							ret = addIfInScope(n);
						else
							throw new EolRuntimeException(
									"A relationship with arity 1 ( " + property
											+ " ) has more than 1 links");
					} else {
						if (ret == null)
							ret = new EolBag<GraphNodeWrapper>();
						GraphNodeWrapper o = addIfInScope(n);
						if (o != null)
							((EolBag<GraphNodeWrapper>) ret)
									.add((GraphNodeWrapper) o);
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

					ret = addIfInScope(r.getStartNode());

					break;

				}

			}

			if (ret == null)
				throw new EolRuntimeException("eContainer failed,\n" + object
						+ "\nis not contained in current scope");

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

			ret = getCollectionForProperty(property);

			final Collection<Object> retCollection = (Collection<Object>) ret;

			if (node.getProperty(property) != null) {

				final List<Object> values = Arrays.asList((Object[]) node
						.getProperty(property));
				retCollection.addAll(values);

			}

			for (IGraphEdge r : node.getOutgoingWithType(property)) {
				GraphNodeWrapper o = addIfInScope(r.getEndNode());
				if (o != null)
					retCollection.add(o);
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
				IGraphNode n = r.getEndNode();
				GraphNodeWrapper o = addIfInScope(n);
				if (otherNodes != null) {
					if (o != null)
						otherNodes.add(o);
				} else if (otherNode == null)
					otherNode = o;
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

	private GraphNodeWrapper addIfInScope(IGraphNode node) {

		if (!engine.enableTraversalScoping)
			return new GraphNodeWrapper(node.getId().toString(), m);

		//System.out.println("addIfInScope used...");
		
		GraphNodeWrapper ret = null;

		if (engine.files.contains(node
				.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)
				.iterator().next().getEndNode())) {
			ret = new GraphNodeWrapper(node.getId().toString(), m);
		}

		return ret;
	}

}
