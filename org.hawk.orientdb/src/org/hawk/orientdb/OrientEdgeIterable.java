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
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientEdgeIterable extends OrientIterable<IGraphEdge, OIdentifiable> {

	public OrientEdgeIterable(Iterable<OIdentifiable> ret, OrientDatabase graph) {
		super(ret, graph);
	}

	@Override
	protected OrientEdge convert(OIdentifiable o) {
		if (o instanceof ORID) {
			return new OrientEdge((ORID)o, getGraph());
		} else {
			final ODocument oDoc = (ODocument)o;
			final ORID id = oDoc.getIdentity();
			if (id.isPersistent()) {
				return new OrientEdge(id, getGraph());
			} else {
				return new OrientEdge(oDoc, getGraph());
			}
		}
	}

}
