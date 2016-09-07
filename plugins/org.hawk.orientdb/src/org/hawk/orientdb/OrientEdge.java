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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientEdge implements IGraphEdge {

	private static final String TYPE_PROPERTY = "_hp_type";

	// We use the same names as OrientDB's GraphDB implementation, so the Graph viewer in Studio works as usual.
	// We only use one E class however, but at least we can traverse through the graph with the usual facilities.
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
//		if (!id.isPersistent()) {
//			graph.getConsole().printerrln("Warning, unsafe: OrientEdge(ORID) used with non-persistent ID " + id);
//		}
		this.id = id;
		this.db = graph;
	}

	/** Should only be used when the ID is not persistent. */
	public OrientEdge(ODocument newDoc, OrientDatabase graph) {
//		if (newDoc.getIdentity().isPersistent()) {
//			graph.getConsole().printerrln("Warning, inefficient: OrientEdge(ODocument) used with persistent ID " + newDoc.getIdentity());
//		}
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
		startNode.removeOutgoing(this);
		endNode.removeIncoming(this);

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

		final ODocument loaded = db.getGraph().load(getId());
		if (loaded == null) {
			db.getConsole().printerrln("Loading edge with id " + getId() + " from OrientDB produced null value");
			Thread.dumpStack();
		}
		loaded.deserializeFields();
		return loaded;
	}

	public static OrientEdge create(OrientDatabase graph, OrientNode start, OrientNode end, String type, String edgeTypeName) {
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
		newDoc.save();

		OrientEdge newEdge;
		if (newDoc.getIdentity().isPersistent()) {
			newEdge = new OrientEdge(newDoc.getIdentity(), graph);
		} else {
			newEdge = new OrientEdge(newDoc, graph);
		}
		start.addOutgoing(newEdge);
		end.addIncoming(newEdge);

		return newEdge;
	}

	public void save() {
		if (changedEdge != null && changedEdge.isDirty()) {
			changedEdge.save();
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
