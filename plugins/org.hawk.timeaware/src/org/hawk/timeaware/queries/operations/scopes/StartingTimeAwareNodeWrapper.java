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

import java.util.List;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Node wrapper which only exposes the versions starting at a certain timepoint.
 * If only given a node, it will be from the timepoint of that node.
 */
public class StartingTimeAwareNodeWrapper extends AbstractSingleWrapTimeAwareNodeWrapper {

	private final long fromInclusive;

	// TODO add unwrapping step and unwrapping tests in the quantifiers (e.g. combine when+always and nested until)

	public StartingTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
		this(original, original.getTime());
	}

	public StartingTimeAwareNodeWrapper(ITimeAwareGraphNode original, long fromInclusive) {
		super(original);
		this.fromInclusive = fromInclusive;
	}

	@Override
	public List<Long> getAllInstants() throws Exception {
		return original.getInstantsBetween(fromInclusive, original.getLatestInstant());
	}

	@Override
	public long getEarliestInstant() throws Exception {
		return fromInclusive;
	}

	@Override
	public long getPreviousInstant() throws Exception {
		final long prev = original.getPreviousInstant();
		if (prev == ITimeAwareGraphNode.NO_SUCH_INSTANT || prev < fromInclusive) {
			return ITimeAwareGraphNode.NO_SUCH_INSTANT;
		} else {
			return prev;
		}
	}

	@Override
	public long getLatestInstant() throws Exception {
		return original.getLatestInstant();
	}

	@Override
	public long getNextInstant() throws Exception {
		return original.getNextInstant();
	}

	@Override
	public ITimeAwareGraphNode travelInTime(long time) {
		final long actualTime = Math.max(time, fromInclusive);
		return original.travelInTime(actualTime);
	}

	@Override
	public List<Long> getInstantsBetween(long fromInclusive, long toInclusive) {
		final long actualFromTime = Math.max(fromInclusive, this.fromInclusive);
		final long actualToTime = Math.max(actualFromTime, toInclusive);
		return original.getInstantsBetween(actualFromTime, actualToTime);
	}

	@Override
	public List<Long> getInstantsFrom(long fromInclusive) {
		final long actualFromTime = Math.max(fromInclusive, this.fromInclusive);
		return original.getInstantsFrom(actualFromTime);
	}

	@Override
	public List<Long> getInstantsUpTo(long toInclusive) {
		return original.getInstantsBetween(this.fromInclusive, toInclusive);
	}

	@Override
	protected ITimeAwareGraphNode wrap(ITimeAwareGraphNode n) {
		return new StartingTimeAwareNodeWrapper(n, this.fromInclusive);
	}
	
}