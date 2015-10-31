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

import com.orientechnologies.orient.core.id.ORID;

public class OrientNodeIterable extends OrientIterable<IGraphNode, ORID> {

	public OrientNodeIterable(Iterable<ORID> oRecordIteratorCluster, OrientDatabase graph) {
		super(oRecordIteratorCluster, graph);
	}

	@Override
	protected OrientNode convert(ORID o) {
		return new OrientNode(o, getGraph());
	}

}
