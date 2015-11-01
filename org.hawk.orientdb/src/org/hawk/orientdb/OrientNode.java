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
import org.hawk.orientdb.util.OrientNameCleaner;

import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientNode implements IGraphNode {
	private static final String PREFIX_PROPERTY = "_hp_";
	private static final String PREFIX_INCOMING = "_hi_";
	private static final String PREFIX_OUTGOING = "_ho_";

	private enum Direction { IN, OUT, BOTH };

	/** Database that contains this node. */
	private final OrientDatabase graph;

	/** Identifier of the node within the database. */
	private final ORID id;

	/** Keeps the changed document in memory until the next save. */
	private ODocument changedVertex;

	/** Should only be used with non-persistent IDs. */
	public OrientNode(ODocument doc, OrientDatabase graph) {
		this.id = doc.getIdentity();
		this.graph = graph;
		this.changedVertex = doc;
	}

	public OrientNode(ORID id, OrientDatabase graph) {
		this.id = id;
		this.graph = graph;
	}

	@Override
	public ORID getId() {
		return id;
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Set<String> keys = new HashSet<>();
		ODocument tmpVertex = getDocument();
		for (String s : tmpVertex.fieldNames()) {
			if (s.startsWith(PREFIX_PROPERTY)) {
				keys.add(OrientNameCleaner.unescapeFromField(s.substring(PREFIX_PROPERTY.length())));
			}
		}
		return keys;
	}

	@Override
	public Object getProperty(String name) {
		final String fieldName = OrientNameCleaner.escapeToField(PREFIX_PROPERTY + name);
		ODocument tmpVertex = getDocument();
		tmpVertex.deserializeFields(fieldName);
		final Object value = tmpVertex.field(fieldName);
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

	public static void setProperties(ODocument doc, Map<String, Object> props) {
		final Map<String, Object> mappedProps = new HashMap<>();
		for (Entry<String, Object> entry : props.entrySet()) {
			String fieldName = entry.getKey();
			mappedProps.put(OrientNameCleaner.escapeToField(PREFIX_PROPERTY + fieldName), entry.getValue());
		}
		doc.fromMap(mappedProps);
	}

	@Override
	public void setProperty(String name, Object value) {
		if (value == null) {
			removeProperty(name);
		} else {
			changedVertex = getDocument();
			changedVertex.field(OrientNameCleaner.escapeToField(PREFIX_PROPERTY + name), value);
			graph.markNodeAsDirty(this);
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		final List<ORID> edges = getEdgeDocuments(Direction.BOTH);
		return new OrientEdgeIterable(edges, graph);
	}

	private List<ORID> getEdgeDocuments(final Direction dir) {
		final List<ORID> edges = new ArrayList<>();
		final ODocument tmpVertex = getDocument();
		for (String propName : tmpVertex.fieldNames()) {
			if (propName.startsWith(PREFIX_INCOMING) && dir != Direction.OUT || propName.startsWith(PREFIX_OUTGOING) && dir != Direction.IN) {
				tmpVertex.deserializeFields(propName);
				Iterable<Object> odocs = tmpVertex.field(propName);
				addAllORIDs(edges, odocs);
			}
		}
		return edges;
	}

	private void addAllORIDs(final List<ORID> edges, Iterable<Object> odocs) {
		if (odocs != null) {
			for (Object odoc : odocs) {
				if (odoc instanceof ORID) {
					edges.add((ORID)odoc);
				} else {
					edges.add(((ODocument)odoc).getIdentity());
				}
			}
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		final List<ORID> edges = getEdgeDocuments(type, Direction.BOTH);
		return new OrientEdgeIterable(edges, graph);
	}

	private List<ORID> getEdgeDocuments(String type, Direction direction) {
		final List<ORID> edges = new ArrayList<>();
		final ODocument tmpVertex = getDocument();
		if (direction == Direction.IN || direction == Direction.BOTH) {
			final String fldName = OrientNameCleaner.escapeToField(PREFIX_INCOMING + type);
			tmpVertex.deserializeFields(fldName);
			final Iterable<Object> inODocs = tmpVertex.field(fldName);
			addAllORIDs(edges, inODocs);
		}

		if (direction == Direction.OUT || direction == Direction.BOTH) {
			final String fldName = OrientNameCleaner.escapeToField(PREFIX_OUTGOING + type);
			tmpVertex.deserializeFields(fldName);
			final Iterable<Object> outODocs = tmpVertex.field(fldName);
			addAllORIDs(edges, outODocs);
		}
		return edges;
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		final List<ORID> edges = getEdgeDocuments(type, Direction.OUT);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		final List<ORID> edges = getEdgeDocuments(type, Direction.IN);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		final List<ORID> edges = getEdgeDocuments(Direction.IN);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		final List<ORID> edges = getEdgeDocuments(Direction.OUT);
		return new OrientEdgeIterable(edges, graph);
	}

	@Override
	public void delete() {
		for (IGraphEdge e : getEdges()) {
			e.delete();
		}
		graph.getGraph().delete(id);
		changedVertex = null;
		graph.unmarkNodeAsDirty(this);
	}

	@Override
	public IGraphDatabase getGraph() {
		return graph;
	}

	@Override
	public void removeProperty(String name) {
		changedVertex = getDocument();
		changedVertex.removeField(OrientNameCleaner.escapeToField(PREFIX_PROPERTY + name));
		graph.markNodeAsDirty(this);
	}

	@Override
	public int hashCode() {
		final int prime = 5381;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.toString().hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientNode [" + id + "]";
	}

	public ODocument getDocument() {
		if (changedVertex != null) {
			return changedVertex;
		}

		ODocument vertex = graph.getNodeById(id).changedVertex;
		if (vertex != null) {
			return vertex;
		}

		final ODocument loaded = graph.getGraph().load(id);
		if (loaded == null) {
			graph.getConsole().printerrln("Loading node with id " + id + " from OrientDB produced null value");
			Thread.dumpStack();
		}
		return loaded;
	}

	public void addOutgoing(OrientEdge newEdge) {
		addToList(newEdge, OrientNameCleaner.escapeToField(PREFIX_OUTGOING + newEdge.getType()));
	}

	private void addToList(OrientEdge newEdge, final String fldName) {
		changedVertex = getDocument();
		changedVertex.deserializeFields(fldName);
		Collection<ORID> out = changedVertex.field(fldName);
		if (out == null) {
			out = new ArrayList<ORID>();
		}
		out.add(newEdge.getDocument().getIdentity());
		changedVertex.field(fldName, out);
	}

	public void addIncoming(OrientEdge newEdge) {
		addToList(newEdge, OrientNameCleaner.escapeToField(PREFIX_INCOMING + newEdge.getType()));
	}

	public void removeOutgoing(OrientEdge orientEdge) {
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_OUTGOING + orientEdge.getType()));
	}

	private void removeFromList(OrientEdge orientEdge, final String fldName) {
		changedVertex = getDocument();
		changedVertex.deserializeFields(fldName);
		Collection<ORID> out = changedVertex.field(fldName);
		if (out != null) {
			out.remove(orientEdge.getDocument().getIdentity());
			changedVertex.field(fldName, out);
		}
	}

	public void removeIncoming(OrientEdge orientEdge) {
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_INCOMING + orientEdge.getType()));
	}

	public void save() {
		if (changedVertex != null && changedVertex.isDirty()) {
			changedVertex.save();
		}
		if (id.isPersistent()) {
			changedVertex = null;
		}
	}
}
