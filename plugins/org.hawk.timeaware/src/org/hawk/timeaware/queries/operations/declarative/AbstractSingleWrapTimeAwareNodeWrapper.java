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
package org.hawk.timeaware.queries.operations.declarative;

import java.util.List;
import java.util.stream.Collectors;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Base class for time-aware node wrappers that can reuse the same wrapping
 * behaviour for all methods that return versions.
 */
public abstract class AbstractSingleWrapTimeAwareNodeWrapper extends AbstractTimeAwareNodeWrapper {

	public AbstractSingleWrapTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
		super(original);
	}

	/**
	 * Wraps a node around the same scope as the current wrapper.
	 */
	protected abstract ITimeAwareGraphNode wrap(ITimeAwareGraphNode n);

	@Override
	public List<ITimeAwareGraphNode> getAllVersions() throws Exception {
		final List<ITimeAwareGraphNode> raw = super.getAllVersions();
		return raw.stream().map(this::wrap).collect(Collectors.toList());
	}

	@Override
	public ITimeAwareGraphNode getEarliest() throws Exception {
		return wrap(super.getEarliest());
	}

	@Override
	public ITimeAwareGraphNode getPrevious() throws Exception {
		return wrap(super.getPrevious());
	}

	@Override
	public ITimeAwareGraphNode getLatest() throws Exception {
		return wrap(super.getLatest());
	}

	@Override
	public ITimeAwareGraphNode getNext() throws Exception {
		return wrap(super.getNext());
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsBetween(long fromInclusive, long toInclusive) throws Exception {
		final List<ITimeAwareGraphNode> raw = super.getVersionsBetween(fromInclusive, toInclusive);
		return raw.stream().map(this::wrap).collect(Collectors.toList());
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsFrom(long fromInclusive) throws Exception {
		final List<ITimeAwareGraphNode> raw = super.getVersionsFrom(fromInclusive);
		return raw.stream().map(this::wrap).collect(Collectors.toList());
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsUpTo(long toInclusive) throws Exception {
		final List<ITimeAwareGraphNode> raw = super.getVersionsUpTo(toInclusive);
		return raw.stream().map(this::wrap).collect(Collectors.toList());
	}

}
