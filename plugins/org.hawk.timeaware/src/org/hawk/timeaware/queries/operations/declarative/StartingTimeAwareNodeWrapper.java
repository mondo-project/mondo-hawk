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
 * Node wrapper which only exposes the versions starting at the current
 * timepoint of the wrapped node.
 */
public class StartingTimeAwareNodeWrapper extends AbstractTimeAwareNodeWrapper {

	// TODO add unwrapping step and unwrapping tests in the quantifiers (e.g. combine when+always and nested until)

	public StartingTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
		super(original);
	}

	@Override
	public List<Long> getAllInstants() throws Exception {
		return original.getInstantsBetween(original.getTime(), original.getLatestInstant());
	}

	@Override
	public long getEarliestInstant() throws Exception {
		return original.getTime();
	}

	@Override
	public long getPreviousInstant() throws Exception {
		return ITimeAwareGraphNode.NO_SUCH_INSTANT;
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
		final long actualTime = Math.max(time, original.getTime());
		return original.travelInTime(actualTime);
	}

	@Override
	public List<Long> getInstantsBetween(long fromInclusive, long toInclusive) {
		final long actualFromTime = Math.max(fromInclusive, original.getTime());
		return original.getInstantsBetween(actualFromTime, toInclusive);
	}

	@Override
	public List<Long> getInstantsFrom(long fromInclusive) {
		final long actualFromTime = Math.max(fromInclusive, original.getTime());
		return original.getInstantsFrom(actualFromTime);
	}

	@Override
	public List<Long> getInstantsUpTo(long toInclusive) {
		return original.getInstantsUpTo(toInclusive);
	}

}