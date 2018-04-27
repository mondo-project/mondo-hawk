/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York.
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
 * updater. Note: this class does not deal with transactions at all - the caller
 * should handle it.
 */
public class ModelElementNode {

	public static final String OBJECT_VERTEX_LABEL = "eobject";

	public static final String EDGE_PROPERTY_CONTAINER = "isContainer";

	public static final String EDGE_PROPERTY_CONTAINMENT = "isContainment";

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

	/** Prefix for any derived edges (e.g. computed through EOL). */
	public static final String DERIVED_EDGE_PREFIX = "de";

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
	 * Returns the type nodes for the supertypes of this model element node,
	 * excluding its own type (which can be obtained through {@link #getTypeNode()}.
	 */
	public List<TypeNode> getKindNodes() {
		final List<TypeNode> nodes = new ArrayList<>();
		for (IGraphEdge outEdge : node.getOutgoingWithType(EDGE_LABEL_OFKIND)) {
			nodes.add(new TypeNode(outEdge.getEndNode()));
		}
		return nodes;
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
	 * Returns the file nodes for this model element node. Model element nodes
	 * with more than one file node can happen with models based on global UUIDs
	 * (e.g. Modelio models).
	 */
	public List<FileNode> getFileNodes() {
		final List<FileNode> nodes = new ArrayList<>();
		for (IGraphEdge outEdge : node.getOutgoingWithType(EDGE_LABEL_FILE)) {
			nodes.add(new FileNode(outEdge.getEndNode()));
		}
		return nodes;
	}

	/**
	 * Fills in the <code>attributeValues</code> and
	 * <code>referenceValues</code> maps with the contents of the slots of the
	 * <code>modelElementNode</code>. Derived attributes and reverse references
	 * are *not* included either. If a <code>null</code> value is passed, that
	 * type of slot will not be read.
	 *
	 * @throws Exception
	 *             Could not begin the transaction on the graph.
	 */
	public void getSlotValues(Map<String, Object> attributeValues, Map<String, Object> referenceValues, Map<String, Object> mixedValues, Map<String, Object> derivedValues) {
		final List<Slot> slots = getTypeNode().getSlots();
		for (Slot s : slots) {
			if (s.isAttribute() && attributeValues != null) {
				final Object value = getSlotValue(s);
				if (value == null) continue;
				attributeValues.put(s.getName(), value);
			} else if (s.isReference() && referenceValues != null) {
				final Object value = getSlotValue(s);
				if (value == null) continue;
				referenceValues.put(s.getName(), value);
			} else if (s.isMixed() && mixedValues != null) {
				final Object value = getSlotValue(s);
				if (value == null) continue;
				mixedValues.put(s.getName(), value);
			} else if (s.isDerived()) {
				final Object value = getSlotValue(s);
				if (value == null) continue;
				derivedValues.put(s.getName(), value);
			}
		}
	}

	/**
	 * Returns the value of the non-derived <code>slot</code> for this model element node.
	 * The slot should be among those returned by {@link TypeNode#getSlots()}
	 * for this model element node.
	 */
	public Object getSlotValue(Slot slot) {
		Object rawValue = null;
		if (slot.isDerived()) {

			// It's a regular derived property
			for (IGraphEdge r : node.getOutgoingWithType(slot.getName())) {
				if (rawValue == null) {
					final IGraphNode dpNode = r.getEndNode();
					rawValue = dpNode.getProperty(slot.getName());

					if (rawValue == null) {
						// Derived edges are not stored as a property, but rather
						// as actual edges from the derived property node to the target
						Iterator<IGraphEdge> dEdges = dpNode.getOutgoingWithType(DERIVED_EDGE_PREFIX + slot.getName()).iterator();
						if (dEdges.hasNext()) {
							List<IGraphNode> targets = new ArrayList<>();
							while (dEdges.hasNext()) {
								targets.add(dEdges.next().getEndNode());
							}
							return targets;
						}
					}
				} else {
					throw new IllegalStateException("WARNING: a derived property node (arity 1) -- ( " + slot.getName()
							+ " ) has more than 1 links in store!");
				}
			}
		} else {
			rawValue = node.getProperty(slot.getName());
		}

		return decodeRawValue(slot, rawValue);
	}

	protected Object decodeRawValue(Slot slot, final Object rawValue) {
		final Collection<Object> collection = slot.getCollection();

		if (slot.isMany() && rawValue != null && (slot.isAttribute() || slot.isMixed() || slot.isDerived())) {
			final Class<?> componentType = rawValue.getClass().getComponentType();
			if (componentType == null) {
				// Derived reference that has not been computed yet, or there was an error evaluating it - no value for now
				return null;
			} else if (!componentType.isPrimitive()) {
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
		}

		if (slot.isReference() || slot.isMixed()) {
			for (IGraphEdge r : node.getOutgoingWithType(slot.getName())) {
				final Object id = r.getEndNode().getId();
				collection.add(id);
			}
		}

		if (slot.isMany()) {
			return collection;
		} else if (slot.isAttribute()) {
			return rawValue;
		} else if (collection.size() == 1) {
			return collection.iterator().next();
		} else if (collection.isEmpty()) {
			return null;
		} else {
			throw new IllegalArgumentException(String.format(
					"A relationship with arity 1 (%s) had %d links",
					slot.getName(), collection.size()));
		}
	}

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

	/**
	 * Returns <code>true</code> if this model element is the root of the
	 * {@link FileNode} it is part of. Note that a root model element might be
	 * still contained within another model element in a different file node.
	 *
	 * TODO: check into why some non-root model elements in Modelio do not have
	 * a container.
	 */
	public boolean isRoot() {
		return getFileNode().isRoot(this);
	}

	public ModelElementNode getContainer() {
		for (IGraphEdge edge : node.getOutgoing()) {
			if (edge.getProperty(EDGE_PROPERTY_CONTAINER) != null) {
				return new ModelElementNode(edge.getEndNode());
			}
		}
		for (IGraphEdge edge : node.getIncoming()) {
			if (edge.getProperty(EDGE_PROPERTY_CONTAINMENT) != null) {
				return new ModelElementNode(edge.getStartNode());
			}
		}
		return null;
	}

	public boolean isContainment(String featureName) {
		return outgoingEdgeWithTypeHasProperty(featureName, EDGE_PROPERTY_CONTAINMENT);
	}

	public boolean isContainer(String featureName) {
		return outgoingEdgeWithTypeHasProperty(featureName, EDGE_PROPERTY_CONTAINER);
	}

	protected IGraphNode getFirstEndNode(final String edgeLabel) {
		final IGraphNode rawTypeNode = node
				.getOutgoingWithType(edgeLabel).iterator().next()
				.getEndNode();
		return rawTypeNode;
	}

	private boolean outgoingEdgeWithTypeHasProperty(String featureName,	final String propertyName) {
		Iterable<IGraphEdge> edges = getNode().getOutgoingWithType(featureName);
		for (IGraphEdge edge : edges) {
			if (featureName.equals(edge.getType())) {
				return edge.getProperty(propertyName) != null;
			}
		}
		return false;
	}

	/**
	 * Returns the graph node identifier for this model element. For Neo4j
	 * databases, this will be usually an integer.
	 */
	public String getNodeId() {
		return getNode().getId().toString();
	}

	/**
	 * Returns the model element identifier for this model element. For EMF
	 * models, this will usually be the URI fragment within the resource.
	 */
	public String getElementId() {
		return getNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
	}

	/**
	 * Returns <code>true</code> if this model element is contained by any other,
	 * regardless of whether the container is on a different file.
	 */
	public boolean isContained() {
		return getContainer() != null;
	}

	public boolean hasChildren() {
		for (IGraphEdge edge : node.getOutgoing()) {
			if (edge.getProperty(EDGE_PROPERTY_CONTAINMENT) != null) {
				return true;
			}
		}
		for (IGraphEdge edge : node.getIncoming()) {
			if (edge.getProperty(EDGE_PROPERTY_CONTAINER) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if this model element is contained directly or
	 * indirectly by the provided file within the provided repository.
	 */
	public boolean isContainedWithin(final String containerRepository, final String containerPath) {
		for (ModelElementNode men = this; men != null; men = men.getContainer()) {
			final FileNode fn = men.getFileNode();
			final String repositoryURL = fn.getRepositoryURL();
			final String filePath = fn.getFilePath();

			if (repositoryURL.equals(containerRepository) && filePath.equals(containerPath)) {
				return true;
			}
		}

		return false;
	}

	public boolean isOfKind(String metaClass) {
		return isOf(metaClass, EDGE_LABEL_OFKIND) || isOf(metaClass, EDGE_LABEL_OFTYPE);
	}

	public boolean isOfType(String metaClass) {
		return isOf(metaClass, EDGE_LABEL_OFTYPE);
	}

	public boolean isOfKind(IGraphNode typeNode) {
		return isOf(typeNode, EDGE_LABEL_OFKIND) || isOf(typeNode, EDGE_LABEL_OFTYPE);
	}

	public boolean isOfType(IGraphNode typeNode) {
		return isOf(typeNode, EDGE_LABEL_OFTYPE);
	}

	protected boolean isOf(IGraphNode typeNode, String edgeLabelOftype) {
		for (IGraphEdge edge : node.getOutgoingWithType(edgeLabelOftype)) {
			if (edge.getEndNode().getId().equals(typeNode.getId())) {
				return true;
			}
		}
		return false;
	}

	protected boolean isOf(String metaClass, final String edgeLabel) {
		for (IGraphEdge edge : node.getOutgoingWithType(edgeLabel)) {
			TypeNode tn = new TypeNode(edge.getEndNode());
			if (metaClass.equals(tn.getTypeName())) {
				return true;
			}
		}
		return false;
	}
}