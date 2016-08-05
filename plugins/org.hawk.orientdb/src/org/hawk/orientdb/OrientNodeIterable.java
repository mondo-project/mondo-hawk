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

import org.hawk.core.graph.IGraphNode;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientNodeIterable extends OrientIterable<IGraphNode, OIdentifiable> {

	public OrientNodeIterable(Iterable<OIdentifiable> oRecordIteratorCluster, OrientDatabase graph) {
		super(oRecordIteratorCluster, graph);
	}

	@Override
	protected OrientNode convert(OIdentifiable o) {
		if (o instanceof ORID) {
			return new OrientNode((ORID)o, getGraph());
		} else {
			return new OrientNode((ODocument)o, getGraph());
		}
	}

}
