/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

/**
 * Read-only abstraction of a model element type in the graph populated by the
 * updater.
 */
public class TypeNode {
	private final IGraphNode node;
	private final String name; 

	// never use this field directly: use getSlots() instead, as we use lazy initialization.
	private Map<String, Slot> slots;

	public TypeNode(IGraphNode node) {
		this.node = node;
		this.name = (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
	}

	public IGraphNode getNode() {
		return node;
	}

	public MetamodelNode getMetamodel() {
		if (node.getGraph().currentMode() == Mode.NO_TX_MODE) {
			throw new IllegalStateException("Cannot retrieve metamodel node from type in batch mode");
		}

		final Iterator<IGraphEdge> itEPackageEdges = node.getOutgoingWithType("epackage").iterator();
		final IGraphEdge ePackageEdge = itEPackageEdges.next();
		return new MetamodelNode(ePackageEdge.getEndNode());
	}

	public String getMetamodelURI() {
		return getMetamodel().getUri();
	}

	public String getTypeName() {
		return name;
	}

	public Slot getSlot(String name) {
		if (node.getProperty(name) != null) {
			return new Slot(this, name);
		}
		return null;
	}

	public Map<String, Slot> getSlots() {
		if (slots == null) {
			slots = new HashMap<>();
			for (String propertyName : node.getPropertyKeys()) {
				// skip over the 'id' property, which is a friendly identifier and not a 'real' slot
				if (IModelIndexer.IDENTIFIER_PROPERTY.equals(propertyName)) continue;

				final Slot slot = new Slot(this, propertyName);
				if (slot.isAttribute() || slot.isReference() || slot.isMixed() || slot.isDerived()) {
					slots.put(propertyName, slot);
				}
			}
		}
		return slots;
	}
	
	public Iterable<ModelElementNode> getAll() {
		final Iterable<IGraphEdge> iterableKind = node.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND);
		final Iterable<IGraphEdge> iterableType = node.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE);
		return new Iterable<ModelElementNode>() {

			@Override
			public Iterator<ModelElementNode> iterator() {
				final Iterator<IGraphEdge> itKind = iterableKind.iterator();
				final Iterator<IGraphEdge> itType = iterableType.iterator();
				return new Iterator<ModelElementNode>() {

					@Override
					public boolean hasNext() {
						return itKind.hasNext() || itType.hasNext();
					}

					@Override
					public ModelElementNode next() {
						if (itKind.hasNext()) {
							return new ModelElementNode(itKind.next().getStartNode());
						} else {
							return new ModelElementNode(itType.next().getStartNode());
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Deprecated: please use {@link #getAll()} from now on.
	 */
	@Deprecated
	public Iterable<ModelElementNode> getAllInstances() {
		return getAll();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeNode other = (TypeNode) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
}