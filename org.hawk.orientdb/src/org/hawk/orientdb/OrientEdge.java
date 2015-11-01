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
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientEdge implements IGraphEdge {

	private static final String TYPE_PROPERTY = "_hp_type";
	private static final String FROM_PROPERTY = "_hp_from";
	private static final String TO_PROPERTY = "_hp_to";

	/** Database that contains this edge. */
	private final OrientDatabase db;

	/** Identifier of the edge within the database. */
	private final ORID id;

	/** Keeps the changed document in memory until the next save. */
	private ODocument changedEdge;

	public OrientEdge(ORID id, OrientDatabase graph) {
		this.id = id;
		this.db = graph;
	}

	public ORID getId() {
		return id;
	}

	public String getType() {
		final ODocument tmpEdge = getDocument();
		tmpEdge.deserializeFields(TYPE_PROPERTY);
		return tmpEdge.field(TYPE_PROPERTY).toString();
	}

	public Set<String> getPropertyKeys() {
		final ODocument tmpEdge = getDocument();
		final Set<String> fieldNames = new HashSet<>(Arrays.asList(tmpEdge.fieldNames()));
		fieldNames.remove(TYPE_PROPERTY);
		fieldNames.remove(FROM_PROPERTY);
		fieldNames.remove(TO_PROPERTY);
		return fieldNames;
	}

	public Object getProperty(String name) {
		final ODocument tmpEdge = getDocument();
		tmpEdge.deserializeFields(name);
		return tmpEdge.field(name);
	}

	public void setProperty(String name, Object value) {
		changedEdge = getDocument();
		changedEdge.field(name, value);
		db.markEdgeAsDirty(this);
	}

	public OrientNode getStartNode() {
		return getNode(FROM_PROPERTY);
	}

	public OrientNode getEndNode() {
		return getNode(TO_PROPERTY);
	}

	private OrientNode getNode(final String property) {
		final ODocument tmpEdge = getDocument();
		tmpEdge.deserializeFields(property);
		final Object value = tmpEdge.field(property);
		if (value instanceof ODocument) {
			ODocument doc = (ODocument) value;
			return db.getNodeById(doc.getIdentity());
		} else {
			ORecordId id = (ORecordId) value;
			return db.getNodeById(id);
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
		db.getGraph().delete(id);

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
		OrientEdge other = (OrientEdge) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientEdge [" + id + "]";
	}

	public ODocument getDocument() {
		if (changedEdge != null) {
			return changedEdge;
		}

		final ODocument dirtyEdge = db.getEdgeById(id).changedEdge;
		if (dirtyEdge != null) {
			return dirtyEdge;
		}

		final ODocument loaded = db.getGraph().load(id);
		if (loaded == null) {
			db.getConsole().printerrln("Loading edge with id " + id + " from OrientDB produced null value");
			Thread.dumpStack();
		}
		return loaded;
	}

	public static OrientEdge create(OrientDatabase graph, OrientNode start, OrientNode end, String type, String edgeTypeName) {
		ODocument newDoc = new ODocument(edgeTypeName);
		newDoc.field(TYPE_PROPERTY, type);
		newDoc.field(FROM_PROPERTY, start.getDocument().getIdentity());
		newDoc.field(TO_PROPERTY, end.getDocument().getIdentity());
		newDoc.save();

		final OrientEdge newEdge = new OrientEdge(newDoc.getIdentity(), graph);
		start.addOutgoing(newEdge);
		end.addIncoming(newEdge);

		return newEdge;
	}

	public void save() {
		if (changedEdge != null && changedEdge.isDirty()) {
			changedEdge.save();
		}
		changedEdge = null;
	}

}
