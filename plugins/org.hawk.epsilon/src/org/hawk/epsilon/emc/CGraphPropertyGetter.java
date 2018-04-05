/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.util.Utils;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.internal.updater.DirtyDerivedAttributesListener;

public class CGraphPropertyGetter extends GraphPropertyGetter {

	private final CEOLQueryEngine engine;

	public CGraphPropertyGetter(IGraphDatabase graph2, CEOLQueryEngine m) {
		super(graph2, m);
		// System.out.println("CGraphPropertyGetter created.");
		engine = m;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object object, final String property) throws EolRuntimeException {
		Object ret = null;
		if (!(object instanceof GraphNodeWrapper))
			throw new EolRuntimeException("a non GraphNodeWrapper object passed to GraphPropertyGetter!");

		final IGraphNode node = ((GraphNodeWrapper) object).getNode();

		// avoid using switch (in the outer-most structure) to allow partial
		// matches and method calls in alternatives
		if (property.equals("hawkFile")) {

			String sep = "";
			StringBuilder buff = new StringBuilder(32);
			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				buff.append(sep);
				buff.append(e.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString());
				sep = ";";

			}
			ret = buff.toString();

		} else if (property.equals("hawkRepo")) {

			String sep = "";
			StringBuilder buff = new StringBuilder(32);
			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				buff.append(sep);
				buff.append(e.getEndNode().getProperty(FileNode.PROP_REPOSITORY).toString());
				sep = ";";

			}
			ret = buff.toString();

		} else if (property.equals("hawkFiles")) {

			Set<String> files = new HashSet<>();
			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE))
				files.add(e.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString());

			ret = files;

		} else if (property.equals("hawkRepos")) {

			Set<String> repos = new HashSet<>();
			for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE))
				repos.add(e.getEndNode().getProperty(FileNode.PROP_REPOSITORY).toString());

			ret = repos;

		} else if (property.startsWith(REVERSE_REFNAV_PREFIX)) {

			List<GraphNodeWrapper> nodes = new EolSequence<GraphNodeWrapper>();
			final String referenceName = property.substring(REVERSE_REFNAV_PREFIX.length());
			for (IGraphEdge r : node.getIncomingWithType(referenceName)) {
				GraphNodeWrapper n = wrapIfInScope(r.getStartNode());
				if (n != null)
					nodes.add(n);
			}
			for (IGraphEdge r : node.getIncomingWithType(ModelElementNode.DERIVED_EDGE_PREFIX + referenceName)) {
				IGraphNode derivedNode = r.getStartNode();
				IGraphNode elementNode = derivedNode.getIncoming().iterator().next().getStartNode();
				GraphNodeWrapper n = wrapIfInScope(elementNode);
				if (n != null) {
					nodes.add(n);
				}
			}

			ret = nodes;

		} else if (property.equals("eContainer")) {

			// HawkClass o = new
			// MetamodelUtils().getTypeOfFromNode(node,m.parser);

			// o.isContained();
			// System.err.println(o.eContainingFeatureName());

			Iterable<IGraphEdge> inc = node.getIncoming();

			for (IGraphEdge r : inc) {

				if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {

					ret = wrapIfInScope(r.getStartNode());

					break;

				}

			}

			if (ret == null)
				throw new EolRuntimeException("eContainer failed,\n" + object + "\nis not contained in current scope");

		}

		else if (property.equals("eContents")) {
			final List<GraphNodeWrapper> results = new ArrayList<>();
			Iterable<IGraphEdge> out = node.getOutgoing();
			for (IGraphEdge r : out) {
				if (r.getProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT) != null) {
					final GraphNodeWrapper endNode = wrapIfInScope(r.getEndNode());
					if (endNode != null) {
						results.add(endNode);
					}
				}
			}
			ret = results;
		}

		else if (property.equals("hawkIn") || property.equals("hawkOut")) {
			final boolean isIncoming = property.equals("hawkIn");
			final List<GraphNodeWrapper> results = new ArrayList<>();
			final Iterable<IGraphEdge> edges = isIncoming ? node.getIncoming() : node.getOutgoing();
			for (IGraphEdge r : edges) {
				if (ModelElementNode.TRANSIENT_EDGE_LABELS.contains(r.getType())) {
					continue;
				}
				final IGraphNode edgeNode = isIncoming ? r.getStartNode() : r.getEndNode();
				final GraphNodeWrapper edgeNodeWrapper = wrapIfInScope(edgeNode);
				results.add(edgeNodeWrapper);
			}
			ret = results;
		}

		else if (canHaveDerivedAttr(node, property)) {

			for (IGraphEdge r : node.getOutgoingWithType(property)) {
				if (ret == null) {
					final IGraphNode derivedNode = r.getEndNode();
					ret = derivedNode.getProperty(property);
					if (ret == null) {
						List<GraphNodeWrapper> derivedTargets = new EolSequence<>();
						for (IGraphEdge edge : derivedNode.getOutgoingWithType(ModelElementNode.DERIVED_EDGE_PREFIX + property)) {
							derivedTargets.add(new GraphNodeWrapper(edge.getEndNode(), m));
							ret = derivedTargets;
						}
					}

					ret = retainScoped(ret);

				} else {
					throw new EolRuntimeException("WARNING: a derived property (arity 1) -- ( " + property
							+ " ) has more than 1 links in store!");
				}
			}
			if (ret == null) {
				throw new EolRuntimeException("derived attribute lookup failed for: " + object + " # " + property);
			} else if (ret instanceof String && ((String) ret).startsWith(DirtyDerivedAttributesListener.NOT_YET_DERIVED_PREFIX)) {
				// XXX IDEA: dynamically derive on the spot on access
				System.err.println("attribute: " + property + " is NYD for node: " + node.getId());
			}

		}

		else if (canHaveMixed(node, property)) {

			ret = getCollectionForProperty(property);

			final Collection<Object> retCollection = (Collection<Object>) ret;

			if (node.getProperty(property) != null) {

				final List<?> values = new Utils().asList(node.getProperty(property));
				retCollection.addAll(values);

			}

			for (IGraphEdge r : node.getOutgoingWithType(property)) {
				GraphNodeWrapper o = wrapIfInScope(r.getEndNode());
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
			Collection<Object> otherNodes = null;

			// FIXMEdone arity etc in metamodel
			// otherNodes = new EolBag<NeoIdWrapperDebuggable>();
			if (isMany(property)) {
				otherNodes = getCollectionForProperty(property);
			}

			for (IGraphEdge r : node.getOutgoingWithType(property)) {
				IGraphNode n = r.getEndNode();
				GraphNodeWrapper o = wrapIfInScope(n);
				if (otherNodes != null) {
					if (o != null)
						otherNodes.add(o);
				} else if (otherNode == null)
					otherNode = o;
				else
					throw new EolRuntimeException(
							"A relationship with arity 1 ( " + property + " ) has more than 1 links");
			}

			ret = otherNodes != null ? otherNodes : otherNode;

		} else {
			throw new EolIllegalPropertyException(object, property, ast, context);
		}

		if (broadcastAccess)
			broadcastAccess(object, property);

		return ret;

	}

	private Object retainScoped(Object ret) {

		Collection<?> cRet = null;
		if (ret instanceof Collection<?>)
			cRet = (Collection<?>) ret;

		if (cRet == null) {
			if (ret instanceof GraphNodeWrapper)
				ret = retainScoped((GraphNodeWrapper) ret);
		} else {
			Iterator<?> it = cRet.iterator();
			while (it.hasNext()) {
				Object r = it.next();
				if (r instanceof GraphNodeWrapper)
					if (retainScoped((GraphNodeWrapper) ret) == null)
						it.remove();
			}
		}
		return cRet == null ? ret : cRet;
	}

	private Object retainScoped(GraphNodeWrapper ret) {
		if (!engine.isTraversalScopingEnabled())
			return ret;

		// capture multiple file containment (ie for singleton nodes)
		for (IGraphEdge e : graph.getNodeById(ret.getId()).getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {

			if (engine.getRawFileNodes().contains(e.getEndNode())) {
				return ret;
			}
		}

		return null;
	}

	private GraphNodeWrapper wrapIfInScope(IGraphNode node) {
		if (!engine.isTraversalScopingEnabled())
			return new GraphNodeWrapper(node, m);

		// capture multiple file containment (ie for singleton nodes)
		for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
			if (engine.getRawFileNodes().contains(e.getEndNode())) {
				return new GraphNodeWrapper(node, m);
			}
		}

		return null;
	}
}
