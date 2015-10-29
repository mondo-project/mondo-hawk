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

	private static final String TYPE_PROPERTY = "@hawk@type";
	private static final String FROM_PROPERTY = "@hawk@from";
	private static final String TO_PROPERTY = "@hawk@to";

	private ODocument edge;
	private OrientDatabase db;

	public OrientEdge(ODocument newDoc, OrientDatabase orientDatabase) {
		this.edge = newDoc;
		this.db = orientDatabase;
		edge.deserializeFields();
	}

	public ORID getId() {
		return edge.getIdentity();
	}

	public String getType() {
		return edge.field(TYPE_PROPERTY).toString();
	}

	public Set<String> getPropertyKeys() {
		final Set<String> fieldNames = new HashSet<>(Arrays.asList(edge.fieldNames()));
		fieldNames.remove(TYPE_PROPERTY);
		fieldNames.remove(FROM_PROPERTY);
		fieldNames.remove(TO_PROPERTY);
		return fieldNames;
	}

	public Object getProperty(String name) {
		return edge.field(name);
	}

	public void setProperty(String name, Object value) {
		edge.field(name, value);
		db.markEdgeAsDirty(this);
	}

	public OrientNode getStartNode() {
		return getNode(FROM_PROPERTY);
	}

	public OrientNode getEndNode() {
		return getNode(TO_PROPERTY);
	}

	private OrientNode getNode(final String property) {
		final Object value = edge.field(property);
		if (value instanceof ODocument) {
			ODocument doc = (ODocument) value;
			return db.getNodeById(doc.getIdentity());
		} else {
			ORecordId id = (ORecordId) value;
			return db.getNodeById(id.getIdentity());
		}
	}

	public void delete() {
		db.deleteEdge(this);
	}

	public void removeProperty(String name) {
		edge.removeField(name);
		db.markEdgeAsDirty(this);
	}

	@Override
	public int hashCode() {
		final int prime = 5381;
		int result = 1;
		result = prime * result + ((edge == null) ? 0 : edge.getIdentity().hashCode());
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
		if (edge == null) {
			if (other.edge != null)
				return false;
		} else if (!edge.getIdentity().equals(other.edge.getIdentity()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrientEdge [" + edge + "]";
	}

	public ODocument getDocument() {
		return edge;
	}

	public static OrientEdge create(OrientDatabase graph, OrientNode start, OrientNode end, String type, String edgeTypeName) {
		ODocument newDoc = new ODocument(edgeTypeName);
		newDoc.field(TYPE_PROPERTY, type);
		newDoc.field(FROM_PROPERTY, start.getDocument().getIdentity());
		newDoc.field(TO_PROPERTY, end.getDocument().getIdentity());

		final OrientEdge newEdge = new OrientEdge(newDoc, graph);
		start.addOutgoing(newEdge);
		end.addIncoming(newEdge);

		return newEdge;
	}

}
