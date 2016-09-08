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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkReference;
import org.hawk.orientdb.util.OrientNameCleaner;

import com.orientechnologies.common.collection.OCollection;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ORecordLazyMultiValue;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientNode implements IGraphNode {
	private static final String PREFIX_PROPERTY = "_hp_";

	/**
	 * Name of the property that stores a index name -> field -> keys map, for
	 * faster removal of nodes from the index (needed for efficient derived
	 * attribute computation).
	 */
	private static final String ATTR_INDEX_KEYS = "_hIndexKeys";

	// These are the same prefixes used by OrientDB's Graph API
	private static final String PREFIX_INCOMING = "in_";
	private static final String PREFIX_OUTGOING = "out_";

	/*
	 * Old prefixes, kept for backwards compatibility (new edges use new
	 * prefixes, we combine new/old edges when listing, removing works on both
	 * new/old edges)
	 */
	private static final String PREFIX_INCOMING_OLD = "_hi_";
	private static final String PREFIX_OUTGOING_OLD = "_ho_";

	private enum Direction { IN, OUT, BOTH };

	/** Database that contains this node. */
	private final OrientDatabase graph;

	/** Identifier of the node within the database. */
	private ORID id;

	/** Keeps the changed document in memory until the next save. */
	private ODocument changedVertex;

	/** Should only be used with non-persistent IDs. */
	public OrientNode(ODocument doc, OrientDatabase graph) {
		this.graph = graph;
		this.changedVertex = doc;
		this.id = doc.getIdentity();
	}

	public OrientNode(ORID id, OrientDatabase graph) {
		this.id = id;
		this.graph = graph;
	}

	@Override
	public ORID getId() {
		/*
		 * If we have a document, it's best to refer to it for the identity: if
		 * this OrientNode is used after a transaction, the ORID might have
		 * changed from a temporary to a persistent one.
		 */
		if (changedVertex != null) {
			return changedVertex.getIdentity();
		}
		return id;
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Set<String> keys = new HashSet<>();
		ODocument tmpVertex = getDocument();
		for (String s : tmpVertex.fieldNames()) {
			if (s != null && s.startsWith(PREFIX_PROPERTY)) {
				keys.add(OrientNameCleaner.unescapeFromField(s.substring(PREFIX_PROPERTY.length())));
			}
		}
		return keys;
	}

	@Override
	public Object getProperty(String name) {
		final String fieldName = OrientNameCleaner.escapeToField(PREFIX_PROPERTY + name);
		ODocument tmpVertex = getDocument();
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
		return getEdges(Direction.BOTH);
	}

	private List<IGraphEdge> getEdges(final Direction dir) {
		final List<IGraphEdge> edges = new ArrayList<>();
		final ODocument tmpVertex = getDocument();
		for (String propName : tmpVertex.fieldNames()) {
			String edgeLabel = null;
			Direction propDir = null;
			if (propName.startsWith(PREFIX_INCOMING) && dir != Direction.OUT) {
				edgeLabel = OrientNameCleaner.unescapeFromField(propName.substring(PREFIX_INCOMING.length()));
				propDir = Direction.IN;
			} else if (propName.startsWith(PREFIX_OUTGOING) && dir != Direction.IN) {
				edgeLabel = OrientNameCleaner.unescapeFromField(propName.substring(PREFIX_OUTGOING.length()));
				propDir = Direction.OUT;
			} else if (propName.startsWith(PREFIX_INCOMING_OLD) && dir != Direction.OUT) {
				edgeLabel = OrientNameCleaner.unescapeFromField(propName.substring(PREFIX_INCOMING_OLD.length()));
				propDir = Direction.IN;
			} else if (propName.startsWith(PREFIX_OUTGOING_OLD) && dir != Direction.IN) {
				edgeLabel = OrientNameCleaner.unescapeFromField(propName.substring(PREFIX_OUTGOING_OLD.length()));
				propDir = Direction.OUT;
			}

			if (edgeLabel != null) {
				Iterable<Object> odocs = tmpVertex.field(propName);
				addAllOIdentifiable(edges, odocs, propDir, edgeLabel);
			}
		}
		return edges;
	}

	private void addAllOIdentifiable(final List<IGraphEdge> edges, Iterable<Object> odocs, Direction dir, String edgeLabel) {
		if (odocs instanceof ORecordLazyMultiValue) {
			// Use a raw iterator that doesn't try to convert values on the fly
			// *and* mark things around as dirty (why?).
			for (Iterator<OIdentifiable> it = ((ORecordLazyMultiValue)odocs).rawIterator(); it.hasNext(); ) {
				edges.add(convertToEdge(it.next(), dir, edgeLabel));
			}
		} else if (odocs != null) {
			for (Object odoc : odocs) {
				if (odoc instanceof OIdentifiable) {
					edges.add(convertToEdge((OIdentifiable)odoc, dir, edgeLabel));
				}
			}
		}
	}

	private IGraphEdge convertToEdge(OIdentifiable odoc, Direction dir, String edgeLabel) {
		final OrientNode n = graph.getNodeById(odoc);
		final ODocument doc = n.getDocument();
		if (doc.getSchemaClass().getName().startsWith(OrientDatabase.VERTEX_TYPE_PREFIX)) {
			if (dir == Direction.OUT) {
				return new OrientLightEdge(this, n, edgeLabel);
			} else {
				return new OrientLightEdge(n, this, edgeLabel);
			}
		} else {
			return new OrientEdge(doc, graph);
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return getEdges(type, Direction.BOTH);
	}

	private List<IGraphEdge> getEdges(String type, Direction direction) {
		final List<IGraphEdge> edges = new ArrayList<>();
		final ODocument tmpVertex = getDocument();
		if (direction == Direction.IN || direction == Direction.BOTH) {
			final String fldName = OrientNameCleaner.escapeToField(PREFIX_INCOMING + type);
			final Iterable<Object> inODocs = tmpVertex.field(fldName);
			addAllOIdentifiable(edges, inODocs, Direction.IN, type);

			final String fldNameOld = OrientNameCleaner.escapeToField(PREFIX_INCOMING_OLD + type);
			final Iterable<Object> inODocsOld = tmpVertex.field(fldNameOld);
			addAllOIdentifiable(edges, inODocsOld, Direction.IN, type);
		}

		if (direction == Direction.OUT || direction == Direction.BOTH) {
			final String fldName = OrientNameCleaner.escapeToField(PREFIX_OUTGOING + type);
			final Iterable<Object> outODocs = tmpVertex.field(fldName);
			addAllOIdentifiable(edges, outODocs, Direction.OUT, type);

			final String fldNameOld = OrientNameCleaner.escapeToField(PREFIX_OUTGOING_OLD + type);
			final Iterable<Object> outODocsOld = tmpVertex.field(fldNameOld);
			addAllOIdentifiable(edges, outODocsOld, Direction.OUT, type);
		}
		return edges;
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return getEdges(type, Direction.OUT);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return getEdges(type, Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return getEdges(Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return getEdges(Direction.OUT);
	}

	@Override
	public void delete() {
		for (IGraphEdge e : getEdges()) {
			e.delete();
		}
		graph.getGraph().delete(getId());
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
		result = prime * result + ((getId() == null) ? 0 : getId().toString().hashCode());
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
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientNode [" + getId() + "]";
	}

	public ODocument getDocument() {
		if (changedVertex != null) {
			return changedVertex;
		}

		ODocument vertex = graph.getNodeById(getId()).changedVertex;
		if (vertex != null) {
			return vertex;
		}

		return graph.getGraph().<ODocument>load(getId());
	}

	public void addOutgoing(ODocument newEdge, String edgeLabel) {
		final String edgePropName = OrientNameCleaner.escapeToField(PREFIX_OUTGOING + edgeLabel);
		if (!graph.getGraph().getTransaction().isActive()) {
			changedVertex = getDocument();

			final OClass oClass = changedVertex.getSchemaClass();
			if (!oClass.existsProperty(edgePropName)) {
				/**
				 * Outgoing edges that haven't been created in
				 * OrientDatabase#registerNodeClass are from the inner workings
				 * of Hawk itself, and therefore don't really require any
				 * explicit ordering. This is the case for file, ofKind, and
				 * ofType.
				 */
				oClass.createProperty(edgePropName, OType.LINKBAG, (OType)null, true);
			}
		}
		addToList(newEdge, edgePropName);
		graph.markNodeAsDirty(this);
	}

	@SuppressWarnings("unchecked")
	private void addToList(ODocument edgeDoc, final String fldName) {
		changedVertex = getDocument();

		Object out = changedVertex.field(fldName);
		if (out == null) {
			OProperty prop = changedVertex.getSchemaClass().getProperty(fldName);
			if (prop != null && prop.getType() == OType.LINKBAG) {
				out = new ORidBag();
			} else {
				out = new ArrayList<OIdentifiable>();
			}
		}

		changedVertex.setTrackingChanges(false);
		if (out instanceof Collection) {
			Collection<OIdentifiable> col = (Collection<OIdentifiable>)out;
			col.add(edgeDoc);
		} else if (out instanceof OCollection) {
			OCollection<OIdentifiable> bag = (OCollection<OIdentifiable>)out;
			bag.add(edgeDoc);
		}
		changedVertex.field(fldName, out);
		changedVertex.setTrackingChanges(true);
	}

	public void addIncoming(ODocument newEdge, String edgeLabel) {
		final String edgePropName = OrientNameCleaner.escapeToField(PREFIX_INCOMING + edgeLabel);
		if (!graph.getGraph().getTransaction().isActive()) {
			changedVertex = getDocument();

			final OClass oClass = changedVertex.getSchemaClass();
			if (!oClass.existsProperty(edgePropName)) {
				// Incoming edges are always linkbags - there's no specific order to them.
				// The property didn't exist before, so we shouldn't be rechecking all
				// records for that either.
				oClass.createProperty(edgePropName, OType.LINKBAG, (OType)null, true);
			}
		}
		addToList(newEdge, edgePropName);
		graph.markNodeAsDirty(this);
	}

	public void removeOutgoing(ODocument orientEdge, String edgeLabel) {
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_OUTGOING + edgeLabel));
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_OUTGOING_OLD + edgeLabel));
		graph.markNodeAsDirty(this);
	}

	@SuppressWarnings("unchecked")
	private void removeFromList(ODocument orientEdge, final String fldName) {
		changedVertex = getDocument();
		Object out = changedVertex.field(fldName);

		changedVertex.setTrackingChanges(false);
		if (out instanceof Collection) {
			((Collection<OIdentifiable>)out).remove(orientEdge);
		} else if (out instanceof OCollection) {
			((OCollection<OIdentifiable>) out).remove(orientEdge);
		}
		changedVertex.field(fldName, out);
		changedVertex.setTrackingChanges(true);
	}

	public void removeIncoming(ODocument orientEdge, String edgeLabel) {
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_INCOMING + edgeLabel));
		removeFromList(orientEdge, OrientNameCleaner.escapeToField(PREFIX_INCOMING_OLD + edgeLabel));
		graph.markNodeAsDirty(this);
	}

	public void save() {
		if (changedVertex != null && changedVertex.isDirty()) {
			changedVertex.save();
			id = changedVertex.getIdentity();
		}
		if (getId().isPersistent()) {
			changedVertex = null;
		}
	}

	@SuppressWarnings("unchecked")
	public void addIndexKey(String idxName, String field, Object key) {
		changedVertex = getDocument();

		Map<String, Map<String, List<Object>>> fieldsByIndex = (Map<String, Map<String, List<Object>>>) changedVertex.field(ATTR_INDEX_KEYS);
		if (fieldsByIndex == null) {
			fieldsByIndex = new HashMap<>();
		}

		Map<String, List<Object>> keysByField = fieldsByIndex.get(idxName);
		if (keysByField == null) {
			keysByField = new HashMap<>();
			fieldsByIndex.put(idxName, keysByField);
		}

		List<Object> keys = keysByField.get(field);
		if (keys == null) {
			keys = new ArrayList<>();
			keysByField.put(field, keys);
		}

		keys.add(key);
		changedVertex.field(ATTR_INDEX_KEYS, fieldsByIndex);
		graph.markNodeAsDirty(this);
	}

	@SuppressWarnings("unchecked")
	public void removeIndexKey(String idxName, String field, Object key) {
		changedVertex = getDocument();

		Map<String, Map<String, List<Object>>> fieldsByIndex = (Map<String, Map<String, List<Object>>>) changedVertex.field(ATTR_INDEX_KEYS);
		if (fieldsByIndex != null) {
			Map<String, List<Object>> keysByField = fieldsByIndex.get(idxName);
			if (keysByField != null) {
				List<Object> keys = keysByField.get(field);
				if (keys != null && keys.remove(key)) {
					changedVertex.field(ATTR_INDEX_KEYS, fieldsByIndex);
					graph.markNodeAsDirty(this);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Object> removeIndexField(String idxName, String field) {
		changedVertex = getDocument();

		Map<String, Map<String, List<Object>>> fieldsByIndex = (Map<String, Map<String, List<Object>>>) changedVertex.field(ATTR_INDEX_KEYS);
		if (fieldsByIndex != null) {
			Map<String, List<Object>> keysByField = fieldsByIndex.get(idxName);
			if (keysByField != null) {
				List<Object> keys = keysByField.remove(field);
				if (keys != null) {
					changedVertex.field(ATTR_INDEX_KEYS, fieldsByIndex);
					graph.markNodeAsDirty(this);
					return keys;
				}
			}
		}

		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Entry<String, Map<String, List<Object>>>> removeIndexFields(String commonPrefix) {
		changedVertex = getDocument();

		boolean changed = false;
		List<Map.Entry<String, Map<String, List<Object>>>> removedEntries = new ArrayList<>();
		Map<String, Map<String, List<Object>>> fieldsByIndex = (Map<String, Map<String, List<Object>>>) changedVertex.field(ATTR_INDEX_KEYS);
		if (fieldsByIndex != null) {
			for (Iterator<Entry<String, Map<String, List<Object>>>> itEntry = fieldsByIndex.entrySet().iterator(); itEntry.hasNext(); ) {
				Entry<String, Map<String, List<Object>>> entry = itEntry.next();
				String key = entry.getKey();
				if (key.startsWith(commonPrefix)) {
					itEntry.remove();
					removedEntries.add(entry);
					changed = true;
				}
			}
		}

		if (changed) {
			changedVertex.field(ATTR_INDEX_KEYS, fieldsByIndex);
			graph.markNodeAsDirty(this);
		}
		return removedEntries;
	}

	protected static void setupDocumentClass(OClass oClass, IHawkClass hClass) {
		// Oversize leaves some extra space in the record, to reduce the
		// frequency in which we need to defragment. Orient sets the oversize
		// of class V at 2 by default, so we do the same.
		oClass.setOverSize(2);

		if (hClass != null) {
			for (IHawkAttribute attr : hClass.getAllAttributes()) {
				OType type = OType.ANY;

				if (attr.isMany()) {
					if (attr.isOrdered() || !attr.isUnique()) {
						type = OType.EMBEDDEDLIST;
					} else {
						type = OType.EMBEDDEDSET;
					}
				} else {
					switch (attr.getType().getInstanceType()) {
					case "java.lang.Long":
					case "Long":
						type = OType.LONG;
						break;
					case "java.lang.Integer":
					case "Integer":
						type = OType.INTEGER;
						break;
					case "java.lang.Short":
					case "Short":
						type = OType.SHORT;
						break;
					case "java.lang.Byte":
					case "Byte":
						type = OType.BYTE;
						break;
					case "java.lang.Float":
					case "Float":
						type = OType.FLOAT;
						break;
					case "java.lang.Double":
					case "Double":
						type = OType.DOUBLE;
						break;
					case "java.lang.Boolean":
					case "Boolean":
						type = OType.BOOLEAN;
						break;
					case "java.lang.String":
					case "String":
						type = OType.STRING;
						break;
					default:
						System.err.println("Unknown instance type " + attr.getType().getInstanceType()
								+ ", falling back to OType.ANY");
					}
				}
				oClass.createProperty(PREFIX_PROPERTY + attr.getName(), type);
			}

			for (IHawkReference ref : hClass.getAllReferences()) {
				if (ref.isOrdered()) {
					oClass.createProperty(PREFIX_OUTGOING + ref.getName(), OType.LINKLIST);
				} else {
					oClass.createProperty(PREFIX_OUTGOING + ref.getName(), OType.LINKBAG);
				}
			}
		}

		System.out.println("set up properties for " + oClass.getName() + ": " + oClass.declaredProperties());
	}
}
