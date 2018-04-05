/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
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
