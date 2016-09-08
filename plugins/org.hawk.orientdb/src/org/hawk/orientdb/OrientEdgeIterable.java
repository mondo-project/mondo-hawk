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

import org.hawk.core.graph.IGraphEdge;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientEdgeIterable extends OrientIterable<IGraphEdge, OIdentifiable> {

	private final OrientNode start;
	private final String edgeLabel;

	public OrientEdgeIterable(OrientNode start, String edgeLabel, Iterable<OIdentifiable> ret, OrientDatabase graph) {
		super(ret, graph);
		this.start = start;
		this.edgeLabel = edgeLabel;
	}

	@Override
	protected IGraphEdge convert(OIdentifiable o) {
		final OrientNode n = graph.getNodeById(o);
		final ODocument doc = n.getDocument();
		if (doc.getSchemaClass().getName().startsWith(OrientDatabase.VERTEX_TYPE_PREFIX)) {
			return new OrientLightEdge(start, n, edgeLabel);
		} else {
			return new OrientEdge(doc, getGraph());
		}
	}

}
