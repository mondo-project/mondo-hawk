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

import java.util.Collections;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;

public class OrientLightEdge implements IGraphEdge {

	private final OrientNode start;
	private final OrientNode end;
	private final String type;

	public OrientLightEdge(OrientNode start, OrientNode end, String type) {
		this.start = start;
		this.end = end;
		this.type = type;
	}

	@Override
	public Object getId() {
		// We have no real identity within the DB
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Set<String> getPropertyKeys() {
		return Collections.emptySet();
	}

	@Override
	public Object getProperty(String name) {
		// We have no properties
		return null;
	}

	@Override
	public void setProperty(String name, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public OrientNode getStartNode() {
		return start;
	}

	@Override
	public OrientNode getEndNode() {
		return end;
	}

	@Override
	public void delete() {
		start.removeOutgoing(end.getDocument(), type);
		end.removeIncoming(start.getDocument(), type);
	}

	@Override
	public void removeProperty(String name) {
		// nothing to do
	}

}
