/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Heavyweight edge, represented as a standalone document. Needed to set properties on an edge.
 */
public class OrientEdge implements IGraphEdge {

	private static final String TYPE_PROPERTY = "_hp_type";

	// We use the same names as OrientDB's GraphDB implementation, so the Graph viewer in Studio works as usual.
	private static final String FROM_PROPERTY = "out";
	private static final String TO_PROPERTY = "in";

	// Old names, kept to preserve backwards compatibility
	private static final String FROM_PROPERTY_OLD = "_hp_from";
	private static final String TO_PROPERTY_OLD = "_hp_to";

	/** Database that contains this edge. */
	private final OrientDatabase db;

	/** Identifier of the edge within the database. */
	private ORID id;

	/** Keeps the changed document in memory until the next save. */
	private ODocument changedEdge;

	/** Should be used when the ID is persistent, to save memory. */
	public OrientEdge(ORID id, OrientDatabase graph) {
		this.id = id;
		this.db = graph;
	}

	/** Should only be used when the ID is not persistent. */
	public OrientEdge(ODocument newDoc, OrientDatabase graph) {
		this.db = graph;
		this.changedEdge = newDoc;
		this.id = changedEdge.getIdentity();
	}

	public ORID getId() {
		if (changedEdge != null) {
			return changedEdge.getIdentity();
		}
		return id;
	}

	public String getType() {
		final ODocument tmpEdge = getDocument();
		return tmpEdge.field(TYPE_PROPERTY).toString();
	}

	public Set<String> getPropertyKeys() {
		final ODocument tmpEdge = getDocument();
		final Set<String> fieldNames = new HashSet<>(Arrays.asList(tmpEdge.fieldNames()));
		fieldNames.remove(TYPE_PROPERTY);
		fieldNames.remove(FROM_PROPERTY);
		fieldNames.remove(TO_PROPERTY);
		fieldNames.remove(FROM_PROPERTY_OLD);
		fieldNames.remove(TO_PROPERTY_OLD);
		return fieldNames;
	}

	public Object getProperty(String name) {
		final ODocument tmpEdge = getDocument();
		return tmpEdge.field(name);
	}

	public void setProperty(String name, Object value) {
		changedEdge = getDocument();
		changedEdge.field(name, value);
		db.markEdgeAsDirty(this);
	}

	public OrientNode getStartNode() {
		final OrientNode fromNode = getNode(FROM_PROPERTY);
		return fromNode != null ? fromNode : getNode(FROM_PROPERTY_OLD);
	}

	public OrientNode getEndNode() {
		final OrientNode toNode = getNode(TO_PROPERTY);
		return toNode != null ? toNode : getNode(TO_PROPERTY_OLD);
	}

	private OrientNode getNode(final String property) {
		final ODocument tmpEdge = getDocument();

		// We don't modify edge documents - we just create or delete them.
		// Disabling tracking speeds up warm queries noticeably.
		tmpEdge.setTrackingChanges(false);
		final Object value = tmpEdge.field(property);
		tmpEdge.setTrackingChanges(true);
		if (value instanceof ODocument) {
			ODocument doc = (ODocument) value;
			return db.getNodeById(doc);
		} else if (value != null) {
			ORecordId id = (ORecordId) value;
			return db.getNodeById(id);
		} else {
			return null;
		}
	}

	public void delete() {
		final OrientNode startNode = getStartNode();
		final OrientNode endNode = getEndNode();

		final ODocument doc = getDocument();
		final String edgeType = (String) doc.field(TYPE_PROPERTY);
		startNode.removeOutgoing(doc, edgeType);
		endNode.removeIncoming(doc, edgeType);

		db.markNodeAsDirty(startNode);
		db.markNodeAsDirty(endNode);
		db.unmarkEdgeAsDirty(this);
		db.getGraph().delete(getId());

		changedEdge = null;
	}

	public void removeProperty(String name) {
		changedEdge = getDocument();
		changedEdge.removeField(name);
		db.markEdgeAsDirty(this);
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
		OrientEdge other = (OrientEdge) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientEdge [" + getId() + "]";
	}

	public ODocument getDocument() {
		if (changedEdge != null) {
			return changedEdge;
		}

		final ODocument dirtyEdge = db.getEdgeById(getId()).changedEdge;
		if (dirtyEdge != null) {
			return dirtyEdge;
		}

		return db.getGraph().<ODocument>load(getId());
	}

	public static IGraphEdge create(OrientDatabase graph, OrientNode start, OrientNode end, String type, String edgeTypeName, Map<String, Object> props) {
		if (props != null && !props.isEmpty()) {
			// Has properties - heavyweight edge
			ODocument newDoc = new ODocument(edgeTypeName);
			newDoc.field(TYPE_PROPERTY, type);

			final ODocument startDoc = start.getDocument();
			final ORID startId = startDoc.getIdentity();
			if (startId.isPersistent()) {
				newDoc.field(FROM_PROPERTY, startId);
			} else {
				newDoc.field(FROM_PROPERTY, startDoc);
			}

			final ODocument endDoc = end.getDocument();
			final ORID endId = endDoc.getIdentity();
			if (endId.isPersistent()) {
				newDoc.field(TO_PROPERTY, endId);
			} else {
				newDoc.field(TO_PROPERTY, endDoc);
			}

			if (props != null) {
				for (Entry<String, Object> entry : props.entrySet()) {
					newDoc.field(entry.getKey(), entry.getValue());
				}
			}
			newDoc.save(edgeTypeName);

			OrientEdge newEdge;
			if (newDoc.getIdentity().isPersistent()) {
				newEdge = new OrientEdge(newDoc.getIdentity(), graph);
			} else {
				newEdge = new OrientEdge(newDoc, graph);
			}
			start.addOutgoing(newDoc, type);
			end.addIncoming(newDoc, type);

			return newEdge;
		} else {
			// No properties - lightweight edge
			start.addOutgoing(end.getDocument(), type);
			end.addIncoming(start.getDocument(), type);
			return new OrientLightEdge(start, end, type);
		}
	}

	public void save() {
		if (changedEdge != null && changedEdge.isDirty()) {
			changedEdge.save();

			// Reload document within Orient - needed for UMLIndexingTest
			changedEdge = null;
			changedEdge = getDocument();
		}
		if (getId().isPersistent()) {
			changedEdge = null;
		}
	}

	protected static void setupDocumentClass(OClass docClass) {
		docClass.createProperty(FROM_PROPERTY, OType.LINK);
		docClass.createProperty(TO_PROPERTY, OType.LINK);
		docClass.createProperty(TYPE_PROPERTY, OType.STRING);
	}

}
