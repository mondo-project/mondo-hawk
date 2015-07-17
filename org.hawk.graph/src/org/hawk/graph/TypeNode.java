/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

/**
 * Read-only abstraction of a model element type in the graph populated by the
 * updater.
 */
public class TypeNode {
	private final IGraphNode node;
	private final String name; 

	// never use this field directly: use getSlots() instead, as we use lazy
	// initialization.
	private List<Slot> slots;

	public TypeNode(IGraphNode node) {
		this.node = node;
		// TODO is node.getId().toString() equal to the line below for type nodes?
		this.name = (String)node.getProperty("id");
	}

	public IGraphNode getNode() {
		return node;
	}

	public String getMetamodelName() {
		for (IGraphEdge node : node.getOutgoingWithType("epackage")) {
			return (String)node.getEndNode().getProperty("id");
		}
		return "(unknown)";
	}

	public String getTypeName() {
		return name;
	}

	public List<Slot> getSlots() {
		if (slots == null) {
			slots = new ArrayList<>();
			for (String propertyName : node.getPropertyKeys()) {
				// skip over the 'id' property, which is a friendly identifier and not a 'real' slot
				if ("id".equals(propertyName)) continue;

				final Slot slot = new Slot(node, propertyName);
				if (slot.isAttribute() || slot.isReference()) {
					slots.add(slot);
				}
			}
		}
		return slots;
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