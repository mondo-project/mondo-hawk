/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
