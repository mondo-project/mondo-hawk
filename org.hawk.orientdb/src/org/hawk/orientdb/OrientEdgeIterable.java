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

import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientEdgeIterable extends OrientIterable<IGraphEdge, ODocument> {

	public OrientEdgeIterable(Iterable<ODocument> ret, OrientDatabase graph) {
		super(ret, graph);
	}

	@Override
	protected OrientEdge convert(ODocument o) {
		return new OrientEdge(o, getGraph());
	}

}
