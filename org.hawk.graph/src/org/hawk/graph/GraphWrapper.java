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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;

/**
 * Wraps an {@link IGraphDatabase} that has been updated by this plugin.
 * Provides some common queries so callers will not need to know the
 * internal details of how the graph database has been set up.
 *
 * TODO This is an incomplete WIP abstraction on top of the graph that is
 * created by the model updaters in this plugin.
 */
public class GraphWrapper {

	/**
	 * Representation of the known metadata about a slot (an attribute or a reference).
	 * Derived attributes and reverse references are *not* included.
	 */
	public static class Slot {
		public final IGraphNode typeNode;
		public final String propertyName;
		public final boolean isAttribute, isReference;
		public final boolean isMany, isOrdered, isUnique;

		public Slot(IGraphNode typeNode, String propertyName) {
			this.typeNode = typeNode;
			this.propertyName = propertyName;

			final String[] propertyMetadata = (String[])typeNode.getProperty(propertyName);
			this.isAttribute = "a".equals(propertyMetadata[0]);
			this.isReference = "r".equals(propertyMetadata[0]);
			this.isMany = "t".equals(propertyMetadata[1]);
			this.isOrdered = "t".equals(propertyMetadata[2]);
			this.isUnique =  "t".equals(propertyMetadata[3]);
		}

		private Collection<Object> getCollection() {
			assert isMany : "A collection cannot be produced for an attribute with isMany = false";
	
			if (isOrdered && isUnique) {
				return new LinkedHashSet<Object>(); // ordered set
			} else if (isOrdered) {
				return new ArrayList<Object>(); // sequence
			} else if (isUnique) {
				return new HashSet<Object>(); // set
			} else {
				return new ArrayList<Object>(); // bag
			}
		}

		/**
		 * Returns the value of this slot for the specified model element node.
		 */
		public Object getValue(IGraphNode modelElementNode) {
			final Object rawValue = modelElementNode.getProperty(propertyName);
			if (isAttribute && rawValue != null && isMany) {
				return getCollection().addAll(Arrays.asList((Object[]) rawValue));
			} else if (isReference) {
				Collection<Object> referencedIds = getCollection();
				for (IGraphEdge r : modelElementNode.getOutgoingWithType(propertyName)) {
					final String id = r.getEndNode().getId().toString();
					referencedIds.add(id);
				}

				if (isMany) {
					return referencedIds;
				} else if (referencedIds.size() == 1) {
					return referencedIds.iterator().next();
				} else if (referencedIds.isEmpty()) {
					return null;
				} else {
					throw new IllegalArgumentException(String.format(
						"A relationship with arity 1 (%s) had %d links", propertyName, referencedIds.size()));
				}
			} else {
				return rawValue;
			}
		} // getValue

	} // Slot

	private final IGraphDatabase graph;

	public GraphWrapper(IGraphDatabase graph) {
		this.graph = graph;
	}

	/**
	 * Returns a set of file nodes matching the specified patterns. The patterns
	 * are of the form '*.extension' or 'file.ext'.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public Set<IGraphNode> getFileNodes(String... patterns) throws Exception {
		final Set<IGraphNode> interestingFileNodes = new HashSet<>();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			IGraphNodeIndex fileIndex = graph.getFileIndex();

			for (String s : patterns) {
				for (IGraphNode n : fileIndex.query("id", s)) {
					interestingFileNodes.add(n);
				}
			}

			tx.success();
			return interestingFileNodes;
		}
	}

	/**
	 * Returns all the nodes representing model elements for the specified file
	 * nodes.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public Set<IGraphNode> getModelElementsFromFileNodes(Collection<IGraphNode> fileNodes) throws Exception {
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final Set<IGraphNode> elemNodes = new HashSet<IGraphNode>();

			for (IGraphNode filenode : fileNodes) {
				for (IGraphEdge edge : filenode.getIncomingWithType("file")) {
					elemNodes.add(edge.getStartNode());
				}
			}

			tx.success();
			return elemNodes;
		}
	}

	/**
	 * Returns a collection of {@link Slot}s of the model element known with the
	 * known metadata about its properties.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public Collection<Slot> getSlots(IGraphNode modelElementNode) throws Exception {
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final Collection<Slot> slots = getSlotsInternal(modelElementNode);
			tx.success();
			return slots;
		}
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
	public void getSlotValues(
		IGraphNode modelElementNode,
		Map<String, Object> attributeValues,
		Map<String, Object> referenceValues) throws Exception {
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final Collection<Slot> slots = getSlotsInternal(modelElementNode);

			for (Slot s : slots) {
				final Object value = s.getValue(modelElementNode);
				if (s.isAttribute) {
					attributeValues.put(s.propertyName, value);
				} else if (s.isReference) {
					referenceValues.put(s.propertyName, value);
				}
			}

			tx.success();
		}
	}

	/**
	 * Returns the type of a model element node in the graph.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	private IGraphNode getModelElementType(IGraphNode modelElementNode) throws Exception {
		return modelElementNode.getOutgoingWithType("typeOf").iterator().next().getEndNode();		
	}

	/**
	 * Internal version of {@link #getSlots(IGraphNode, Map, Map)} that assumes
	 * a transaction has been opened elsewhere.
	 */
	private Collection<Slot> getSlotsInternal(IGraphNode modelElementNode) throws Exception {
		final IGraphNode typeNode = getModelElementType(modelElementNode);
		final List<Slot> slots = new ArrayList<>();
		for (String propertyName : typeNode.getPropertyKeys()) {
			final Slot slot = new Slot(typeNode, propertyName);
			if (slot.isAttribute || slot.isReference) {
				slots.add(slot);
			}
		}
		return slots;
	}

}
