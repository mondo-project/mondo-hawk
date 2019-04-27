/*******************************************************************************
 * Copyright (c) 2015-2019 The University of York, Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

public class MetamodelNode {

	private IGraphNode node;

	public MetamodelNode(IGraphNode n) {
		this.node = n;
	}

	public Iterable<TypeNode> getTypes() {
		final Iterable<IGraphEdge> iterableEdges = node.getIncomingWithType("epackage");
		return new Iterable<TypeNode>() {
			@Override
			public Iterator<TypeNode> iterator() {
				final Iterator<IGraphEdge> itEdges = iterableEdges.iterator();
				return new Iterator<TypeNode>() {

					@Override
					public boolean hasNext() {
						return itEdges.hasNext();
					}

					@Override
					public TypeNode next() {
						return new TypeNode(itEdges.next().getStartNode());
					}

					@Override
					public void remove() {
						itEdges.remove();
					}};
			}
		};
	}

	/**
	 * Returns the type node in this metamodel node with the specified name.
	 */
	public TypeNode getTypeNode(String typeName) {
		for (TypeNode t : getTypes()) {
			if (t.getTypeName().equals(typeName)) {
				return t;
			}
		}
		throw new NoSuchElementException("Type " + typeName + " does not exist in metamodel " + getUri());
	}

	public IGraphNode getNode() {
		return node;
	}

	public String getUri() {
		return (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
	}

	public String getType() {
		return (String)node.getProperty(IModelIndexer.METAMODEL_TYPE_PROPERTY);
	}

	public String getResource() {
		return (String)node.getProperty(IModelIndexer.METAMODEL_RESOURCE_PROPERTY);
	}

	public List<MetamodelNode> getDependencies() {
		final List<MetamodelNode> nodes = new ArrayList<>();
		for (IGraphEdge e : node.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
			final IGraphNode depNode = e.getEndNode();
			nodes.add(new MetamodelNode(depNode));
		}
		return nodes;
	}
}
