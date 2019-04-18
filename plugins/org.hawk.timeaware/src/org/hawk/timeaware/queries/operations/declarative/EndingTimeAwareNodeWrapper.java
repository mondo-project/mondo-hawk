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

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

/**
 * Node wrapper which only exposes the versions of the wrapped node up to and including the
 * specified timepoint.
 */
public class EndingTimeAwareNodeWrapper extends AbstractSingleWrapTimeAwareNodeWrapper {

	private final long toInclusive;

	public EndingTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
		this(original, original.getTime());
	}

	public EndingTimeAwareNodeWrapper(ITimeAwareGraphNode original, long toInclusive) {
		super(original);
		this.toInclusive = toInclusive;
	}

	@Override
	public List<Long> getAllInstants() throws Exception {
		return original.getInstantsUpTo(toInclusive);
	}

	@Override
	public long getEarliestInstant() throws Exception {
		return original.getEarliestInstant();
	}

	@Override
	public long getPreviousInstant() throws Exception {
		return original.getPreviousInstant();
	}

	@Override
	public long getLatestInstant() throws Exception {
		return Math.min(toInclusive, original.getLatestInstant());
	}

	@Override
	public long getNextInstant() throws Exception {
		return Math.min(toInclusive, original.getNextInstant());
	}

	@Override
	public ITimeAwareGraphNode travelInTime(long time) {
		final long actualTime = Math.min(time, this.toInclusive);
		return original.travelInTime(actualTime);
	}

	@Override
	public List<Long> getInstantsBetween(long fromInclusive, long toInclusive) {
		final long actualFromTime = Math.min(fromInclusive, this.toInclusive);
		final long actualToTime = Math.min(toInclusive, this.toInclusive);
		return original.getInstantsBetween(actualFromTime, actualToTime);
	}

	@Override
	public List<Long> getInstantsFrom(long fromInclusive) {
		final long actualFromTime = Math.min(fromInclusive, this.toInclusive);
		return original.getInstantsFrom(actualFromTime);
	}

	@Override
	public List<Long> getInstantsUpTo(long toInclusive) {
		final long actualToTime = Math.min(toInclusive, this.toInclusive);
		return original.getInstantsUpTo(actualToTime);
	}

	@Override
	protected ITimeAwareGraphNode wrap(ITimeAwareGraphNode n) {
		return new EndingTimeAwareNodeWrapper(n, toInclusive);
	}

}