/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.timeaware.queries.operations.scopes;

import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Base class for a set of node wrappers that are designed to limit the
 * historical scope of the original node, and preserve this scope over the basic
 * version traversal primitives (next/prev/earliest/latest/...).
 */
public abstract class AbstractTimeAwareNodeWrapper implements IScopingTimeAwareGraphNode {

	/**
	 * The original time aware graph node that is being wrapped. This may
	 * be a wrapper itself, allowing us to compose operations.
	 */
	protected ITimeAwareGraphNode original;

	public AbstractTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
		this.original = original;
	}

	@Override
	public Object getId() {
		return original.getId();
	}

	@Override
	public Set<String> getPropertyKeys() {
		return original.getPropertyKeys();
	}

	@Override
	public Object getProperty(String name) {
		return original.getProperty(name);
	}

	@Override
	public void setProperty(String name, Object value) {
		original.setProperty(name, value);
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		return original.getEdges();
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return original.getEdgesWithType(type);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return original.getOutgoingWithType(type);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return original.getIncomingWithType(type);
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return original.getIncoming();
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return original.getOutgoing();
	}

	@Override
	public void delete() {
		original.delete();
	}

	@Override
	public IGraphDatabase getGraph() {
		return original.getGraph();
	}

	@Override
	public void removeProperty(String name) {
		original.removeProperty(name);
	}

	@Override
	public boolean isAlive() {
		return original.isAlive();
	}

	@Override
	public long getTime() {
		return original.getTime();
	}

	@Override
	public void end() {
		original.end();
	}

	@Override
	public ITimeAwareGraphNode unscope() {
		if (original instanceof IScopingTimeAwareGraphNode) {
			return ((IScopingTimeAwareGraphNode)original).unscope();
		} else {
			return original;
		}
	}
	
}