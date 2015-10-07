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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;


/**
 * Read-only abstraction of a model element within the graph populated by this
 * updater.
 */
public class ModelElementNode {

	/** Label for the transient edge from the model element node to its file. */
	public static final String EDGE_LABEL_FILE = "file";

	/** Label for the transient edge from the model element node to its type and all its supertypes. */
	public static final String EDGE_LABEL_OFKIND = "ofKind";

	/** Label for the transient edge from the node to its immediate type. */
	public static final String EDGE_LABEL_OFTYPE = "ofType";

	/** Labels for all the transient edges from a model element node. */
	public static final List<String> TRANSIENT_EDGE_LABELS = Arrays.asList(
		EDGE_LABEL_FILE, EDGE_LABEL_OFKIND, EDGE_LABEL_OFTYPE
	);

	public static final Set<String> TRANSIENT_ATTRIBUTES = new HashSet<>(Arrays.asList(IModelIndexer.IDENTIFIER_PROPERTY, IModelIndexer.SIGNATURE_PROPERTY));

	private final IGraphNode node;

	// never access this field directly: always call getTypeNode(),
	// as we use lazy initialization.
	private TypeNode typeNode;
	private FileNode fileNode;

	public ModelElementNode(IGraphNode node) {
		this.node = node;
	}

	/**
	 * Returns the type node for this model element node.
	 */
	public TypeNode getTypeNode() {
		if (typeNode == null) {
			final IGraphNode rawTypeNode = getFirstEndNode(EDGE_LABEL_OFTYPE);
			typeNode = new TypeNode(rawTypeNode);
		}
		return typeNode;
	}

	/**
	 * Returns the file node for this model element node.
	 */
	public FileNode getFileNode() {
		if (fileNode == null) {
			final IGraphNode rawFileNode = getFirstEndNode(EDGE_LABEL_FILE);
			fileNode = new FileNode(rawFileNode);
		}
		return fileNode;
	}

	/**
	 * Fills in the <code>attributeValues</code> and
	 * <code>referenceValues</code> maps with the contents of the slots of the
	 * <code>modelElementNode</code>. Derived attributes and
	 * reverse references are *not* included either.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public void getSlotValues(Map<String, Object> attributeValues, Map<String, Object> referenceValues) {
			for (Slot s : getTypeNode().getSlots()) {
				final Object value = getSlotValue(s);
				if (value == null) continue;
				if (s.isAttribute()) {
					attributeValues.put(s.getName(), value);
				} else if (s.isReference()) {
					referenceValues.put(s.getName(), value);
				}
			}
	}

	/**
	 * Returns the value of the <code>slot</code> for this model element node.
	 * The slot should be among those returned by {@link TypeNode#getSlots()}
	 * for this model element node.
	 */
	public Object getSlotValue(Slot slot) {
		final Object rawValue = node.getProperty(slot.getName());
		if (slot.isAttribute() && rawValue != null && slot.isMany()) {
			final Collection<Object> collection = slot.getCollection();
			final Class<?> componentType = rawValue.getClass().getComponentType();
			if (!componentType.isPrimitive()) {
				// non-primitive arrays can be cast to Object[]
				collection.addAll(Arrays.asList((Object[]) rawValue));
			} else if (componentType == double.class) {
				// primitive arrays need to be explicitly cast, and then we have to box the values
				for (double v : (double[])rawValue) collection.add(v);
			} else if (componentType == float.class) {
				for (float v : (float[])rawValue) collection.add(v);
			} else if (componentType == long.class) {
				for (long v : (long[])rawValue) collection.add(v);
			} else if (componentType == int.class) {
				for (int v : (int[])rawValue) collection.add(v);
			} else if (componentType == short.class) {
				for (int v : (short[])rawValue) collection.add(v);
			} else if (componentType == byte.class) {
				for (byte v : (byte[])rawValue) collection.add(v);
			} else if (componentType == char.class) {
				for (char v : (char[])rawValue) collection.add(v);
			} else if (componentType == byte.class) {
				for (byte v : (byte[])rawValue) collection.add(v);
			} else if (componentType == boolean.class) {
				for (boolean v : (boolean[])rawValue) collection.add(v);
			}
			return collection;
		} else if (slot.isReference()) {
			final Collection<Object> referencedIds;
			if (slot.isMany()) {
				referencedIds = slot.getCollection();
			} else {
				referencedIds = new ArrayList<>();
			}

			for (IGraphEdge r : node
					.getOutgoingWithType(slot.getName())) {
				final Object id = r.getEndNode().getId();
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
						slot.getName(), referencedIds.size()));
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

	public boolean isContainment(String featureName) {
		return outgoingEdgeWithTypeHasProperty(featureName, "isContainment");
	}

	public boolean isContainer(String featureName) {
		return outgoingEdgeWithTypeHasProperty(featureName, "isContainer");
	}

	protected IGraphNode getFirstEndNode(final String edgeLabel) {
		final IGraphNode rawTypeNode = node
				.getOutgoingWithType(edgeLabel).iterator().next()
				.getEndNode();
		return rawTypeNode;
	}

	private boolean outgoingEdgeWithTypeHasProperty(String featureName,
			final String propertyName) {
		Iterator<IGraphEdge> edges = getNode().getOutgoingWithType(featureName).iterator();
		if (edges.hasNext()) {
			return edges.next().getProperty(propertyName) != null;
		}
		return false;
	}

	public String getId() {
		return getNode().getId().toString();
	}
}