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
package org.hawk.orientdb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientNode implements IGraphNode {
	private static final String PREFIX_PROPERTY = "_hp_";
	private static final String PREFIX_INCOMING = "_hi_";
	private static final String PREFIX_OUTGOING = "_ho_";

	private static final Map<String, String> INVALID_CHAR_REPLACEMENTS;
	static {
		INVALID_CHAR_REPLACEMENTS = new HashMap<String, String>();
		INVALID_CHAR_REPLACEMENTS.put(":", "!hcol!");
		INVALID_CHAR_REPLACEMENTS.put(",", "!hcom!");
		INVALID_CHAR_REPLACEMENTS.put(";", "!hsco!");
		INVALID_CHAR_REPLACEMENTS.put(" ", "!hspa!");
		INVALID_CHAR_REPLACEMENTS.put("%", "!hpct!");
		INVALID_CHAR_REPLACEMENTS.put("=", "!hequ!");
		INVALID_CHAR_REPLACEMENTS.put(".", "!hdot!");
	}

	private enum Direction { IN, OUT, BOTH };

	private OrientDatabase graph;
	private ODocument vertex;

	private static String unescapeFromField(final String escapedPropertyName) {
		String propertyName = escapedPropertyName;
		for (Entry<String, String> entry : INVALID_CHAR_REPLACEMENTS.entrySet()) {
			propertyName = propertyName.replace(entry.getValue(), entry.getKey());
		}
		return propertyName;
	}

	private static String escapeToField(final String unescapedFieldName) {
		String fieldName = unescapedFieldName;
		for (Entry<String, String> entry : INVALID_CHAR_REPLACEMENTS.entrySet()) {
			fieldName = fieldName.replace(entry.getKey(), entry.getValue());
		}
		return fieldName;
	}

	public OrientNode(ODocument o, OrientDatabase graph) {
		this.vertex = o;
		this.graph = graph;
		vertex.deserializeFields();
	}

	@Override
	public ORID getId() {
		return vertex.getIdentity();
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Set<String> keys = new HashSet<>();
		for (String s : vertex.fieldNames()) {
			if (s.startsWith(PREFIX_PROPERTY)) {
				keys.add(unescapeFromField(s.substring(PREFIX_PROPERTY.length())));
			}
		}
		return keys;
	}

	@Override
	public Object getProperty(String name) {
		final Object value = vertex.field(escapeToField(PREFIX_PROPERTY + name));
		if (value instanceof OTrackedList<?>) {
			final OTrackedList<?> cValue = (OTrackedList<?>)value;
			Class<?> genericClass = cValue.getGenericClass();
			if (genericClass == null) {
				if (!cValue.isEmpty()) {
					genericClass = cValue.get(0).getClass();
				} else {
					genericClass = Object.class;
				}
			}
			final Object[] newArray = (Object[])Array.newInstance(genericClass, cValue.size());
			return cValue.toArray(newArray);
		}
		return value;
	}

	public void setProperties(Map<String, Object> props) {
		final Map<String, Object> mappedProps = new HashMap<>();
		for (Entry<String, Object> entry : props.entrySet()) {
			String fieldName = entry.getKey();
			mappedProps.put(escapeToField(PREFIX_PROPERTY + fieldName), entry.getValue());
		}
		vertex.fromMap(mappedProps);
		graph.markNodeAsDirty(this);
	}

	@Override
	public void setProperty(String name, Object value) {
		vertex.field(escapeToField(PREFIX_PROPERTY + name), value);
		graph.markNodeAsDirty(this);
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		final List<ODocument> edges = getEdgeDocuments(Direction.BOTH);
		return new OrientEdgeIterable(edges, graph);
	}

	private List<ODocument> getEdgeDocuments(final Direction dir) {
		final List<ODocument> edges = new ArrayList<>();
		for (String propName : vertex.fieldNames()) {
			if (propName.startsWith(PREFIX_INCOMING) && dir != Direction.OUT || propName.startsWith(PREFIX_OUTGOING) && dir != Direction.IN) {
				Iterable<ODocument> odocs = vertex.field(propName);
				if (odocs != null) {
					for (ODocument odoc : odocs) {
						edges.add(odoc);
					}
				}
			}
		}
		return edges;
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		final List<ODocument> edges = getEdgeDocuments(type, Direction.BOTH);
		return new OrientEdgeIterable(edges, graph);
	}

	private List<ODocument> getEdgeDocuments(String type, Direction direction) {
		final List<ODocument> edges = new ArrayList<>();
		if (direction == Direction.IN || direction == Direction.BOTH) {
			final Iterable<ODocument> inODocs = vertex.field(escapeToField(PREFIX_INCOMING + type));
			if (inODocs != null) {
				for (ODocument odoc : inODocs) {
					edges.add(odoc);
				}
			}
		}

		if (direction == Direction.OUT || direction == Direction.BOTH) {
			final Iterable<ODocument> outODocs = vertex.field(escapeToField(PREFIX_OUTGOING + type));
			if (outODocs != null) {
				for (ODocument odoc : outODocs) {
					edges.add(odoc);
				}
			}
		}
		return edges;
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		final List<ODocument> edges = getEdgeDocuments(type, Direction.OUT);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		final List<ODocument> edges = getEdgeDocuments(type, Direction.IN);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		final List<ODocument> edges = getEdgeDocuments(Direction.IN);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		final List<ODocument> edges = getEdgeDocuments(Direction.OUT);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public void delete() {
		graph.deleteNode(this);
		graph.unmarkNodeAsDirty(this);
	}

	@Override
	public IGraphDatabase getGraph() {
		return graph;
	}

	@Override
	public void removeProperty(String name) {
		vertex.removeField(name);
		graph.markNodeAsDirty(this);
	}

	@Override
	public int hashCode() {
		final int prime = 5381;
		int result = 1;
		result = prime * result + ((vertex == null) ? 0 : vertex.getIdentity().hashCode());
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
		OrientNode other = (OrientNode) obj;
		if (vertex == null) {
			if (other.vertex != null)
				return false;
		} else if (!vertex.getIdentity().equals(other.vertex.getIdentity()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientNode [" + vertex + "]";
	}

	public ODocument getDocument() {
		return vertex;
	}

	public void addOutgoing(OrientEdge newEdge) {
		addToList(newEdge, escapeToField(PREFIX_OUTGOING + newEdge.getType()));
	}

	private void addToList(OrientEdge newEdge, final String fldName) {
		Collection<ODocument> out = vertex.field(fldName);
		if (out == null) {
			out = new ArrayList<ODocument>();
		}
		out.add(newEdge.getDocument());
		vertex.field(fldName, out);
	}

	public void addIncoming(OrientEdge newEdge) {
		addToList(newEdge, escapeToField(PREFIX_INCOMING + newEdge.getType()));
	}

	public void removeOutgoing(OrientEdge orientEdge) {
		removeFromList(orientEdge, escapeToField(PREFIX_OUTGOING + orientEdge.getType()));
	}

	private void removeFromList(OrientEdge orientEdge, final String fldName) {
		Collection<ODocument> out = vertex.field(fldName);
		if (out != null) {
			out.remove(orientEdge.getDocument());
			vertex.field(fldName, out);
		}
	}

	public void removeIncoming(OrientEdge orientEdge) {
		removeFromList(orientEdge, escapeToField(PREFIX_INCOMING + orientEdge.getType()));
	}
}
