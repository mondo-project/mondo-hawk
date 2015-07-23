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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;


/**
 * Read-only abstraction of a model element within the graph populated by this
 * updater.
 */
public class ModelElementNode {
	private final IGraphNode node;

	// never access this field directly: always call getTypeNode(),
	// as we use lazy initialization.
	private TypeNode typeNode;

	public ModelElementNode(IGraphNode node) {
		this.node = node;
	}

	/**
	 * Returns the type node for this model element node.
	 */
	public TypeNode getTypeNode() {
		if (typeNode == null) {
			final IGraphNode rawTypeNode = node
					.getOutgoingWithType("typeOf").iterator().next()
					.getEndNode();
			typeNode = new TypeNode(rawTypeNode);
		}
		return typeNode;
	}

	/**
	 * Fills in the <code>attributeValues</code> and
	 * <code>referenceValues</code> maps with the contents of the slots of the
	 * <code>modelElementNode</code>. Derived attributes and reverse references
	 * are *not* included.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public void getSlotValues(Map<String, Object> attributeValues, Map<String, Object> referenceValues) {
			for (Slot s : getTypeNode().getSlots()) {
				final Object value = getSlotValue(s);
				if (s.isAttribute()) {
					attributeValues.put(s.getPropertyName(), value);
				} else if (s.isReference()) {
					referenceValues.put(s.getPropertyName(), value);
				}
			}
	}

	/**
	 * Returns the value of the <code>slot</code> for this model element node.
	 * The slot should be among those returned by {@link TypeNode#getSlots()}
	 * for this model element node.
	 */
	public Object getSlotValue(Slot slot) {
		final Object rawValue = node.getProperty(slot.getPropertyName());
		if (slot.isAttribute() && rawValue != null && slot.isMany()) {
			final Collection<Object> collection = slot.getCollection();
			collection.addAll(Arrays.asList((Object[]) rawValue));
			return collection;
		} else if (slot.isReference()) {
			final Collection<Object> referencedIds;
			if (slot.isMany()) {
				referencedIds = slot.getCollection();
			} else {
				referencedIds = new ArrayList<>();
			}

			for (IGraphEdge r : node
					.getOutgoingWithType(slot.getPropertyName())) {
				final String id = r.getEndNode().getId().toString();
				referencedIds.add(id);
			}

			if (slot.isMany()) {
				return referencedIds;
			} else if (referencedIds.size() == 1) {
				return referencedIds.iterator().next();
			} else if (referencedIds.isEmpty()) {
				return null;
			} else {
				throw new IllegalArgumentException(String.format(
						"A relationship with arity 1 (%s) had %d links",
						slot.getPropertyName(), referencedIds.size()));
			}
		} else {
			return rawValue;
		}
	} // getValue

	public IGraphNode getNode() {
		return node;
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
		ModelElementNode other = (ModelElementNode) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
}