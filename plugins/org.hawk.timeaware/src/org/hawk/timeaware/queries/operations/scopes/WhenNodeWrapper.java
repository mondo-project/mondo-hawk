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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node wrapper that only exposes a specific list of versions of that node,
 * starting at the wrapped version.
 */
public class WhenNodeWrapper extends AbstractTimeAwareNodeWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhenNodeWrapper.class);

	private final List<Long> matchingVersions;
	private final int matchingVersionPosition;

	/**
	 * Creates a new wrapper, which only exposes some versions from its current
	 * version onwards. This version assumes the wrapped node is the oldest among
	 * the matching versions.
	 * 
	 * @param original         Node to be wrapped.
	 * @param matchingVersions Timepoints to be exposed, from newest to oldest.
	 */
	public WhenNodeWrapper(ITimeAwareGraphNode original, List<Long> matchingVersions) {
		this(original, matchingVersions, matchingVersions.size() - 1);
	}

	/**
	 * Creates a new wrapper, which only exposes some versions of itself. The wrapped
	 * node is in a certain <code>position</code> of the matching versions (the first one
	 * is the newest one).
	 */
	protected WhenNodeWrapper(ITimeAwareGraphNode original, List<Long> matchingVersions, int position) {
		super(original);
		this.matchingVersions = matchingVersions;
		this.matchingVersionPosition = position;

		assert !matchingVersions.isEmpty() : "At least one matching version should exist";
		assert matchingVersions.get(position) == original.getTime() : "Wrapped node should have the expected time from its position in the matched timepoints list";
	}

	@Override
	public List<Long> getAllInstants() {
		return matchingVersions;
	}

	@Override
	public long getEarliestInstant() {
		return matchingVersions.get(matchingVersions.size() - 1);
	}

	@Override
	public long getPreviousInstant() {
		// Versions go from newest to oldest, so +1 is older, -1 is newer
		if (matchingVersionPosition + 1 < matchingVersions.size()) {
			return matchingVersionPosition + 1;
		} else {
			return ITimeAwareGraphNode.NO_SUCH_INSTANT;
		}
	}

	@Override
	public long getLatestInstant() {
		return matchingVersions.get(0);
	}

	@Override
	public long getNextInstant() {
		if (matchingVersionPosition > 0) {
			return matchingVersions.get(matchingVersionPosition - 1);
		} else {
			return ITimeAwareGraphNode.NO_SUCH_INSTANT;
		}
	}

	@Override
	public ITimeAwareGraphNode travelInTime(long time) {
		try {
			if (time < getEarliestInstant()) {
				return null;
			}

			// Find the latest version before or equal to that time
			int position = matchingVersions.size() - 1;
			if (matchingVersions.size() > 1) {
				for (int i = matchingVersions.size() - 2; i >= 0; i--) {
					final long candidate = matchingVersions.get(i);
					if (candidate <= time) {
						position = i;
					} else {
						break;
					}
				}
			}

			final long timepoint = matchingVersions.get(position);
			return new WhenNodeWrapper(original.travelInTime(timepoint), matchingVersions, position);
		} catch (Exception ex) {
			LOGGER.error("Could not travel in time", ex);
		}

		return null;
	}

	@Override
	public List<Long> getInstantsBetween(long fromInclusive, long toInclusive) {
		final List<Long> results = new ArrayList<>();
		final Iterator<Long> itInstants = matchingVersions.iterator();

		while (itInstants.hasNext()) {
			final long instant = itInstants.next();
			if (instant > toInclusive) {
				// too recent, skip
			} else if (instant >= fromInclusive) {
				results.add(instant);
			} else {
				// after the end of the range, stop
				break;
			}
		}
		
		return results;
	}

	@Override
	public List<Long> getInstantsFrom(long fromInclusive) {
		return getInstantsBetween(fromInclusive, getLatestInstant());
	}

	@Override
	public List<Long> getInstantsUpTo(long toInclusive) {
		return getInstantsBetween(getEarliestInstant(), toInclusive);
	}

	@Override
	public List<ITimeAwareGraphNode> getAllVersions() throws Exception {
		final List<ITimeAwareGraphNode> taNodes = new ArrayList<>(matchingVersions.size());
		for (int i = 0; i < matchingVersions.size(); i++) {
			final ITimeAwareGraphNode version = original.travelInTime(matchingVersions.get(i));
			final WhenNodeWrapper wrapped = new WhenNodeWrapper(version, matchingVersions, i);
			taNodes.add(wrapped);
		}
		return taNodes;
	}

	@Override
	public ITimeAwareGraphNode getEarliest() throws Exception {
		final ITimeAwareGraphNode version = original.travelInTime(getEarliestInstant());
		return new WhenNodeWrapper(version, matchingVersions, matchingVersions.size() - 1);
	}

	@Override
	public ITimeAwareGraphNode getPrevious() {
		if (matchingVersionPosition + 1 < matchingVersions.size()) {
			final ITimeAwareGraphNode version = original.travelInTime(matchingVersions.get(matchingVersionPosition + 1));
			return new WhenNodeWrapper(version, matchingVersions, matchingVersionPosition + 1);
		} else {
			return null;
		}
	}

	@Override
	public ITimeAwareGraphNode getLatest() {
		final ITimeAwareGraphNode version = original.travelInTime(getLatestInstant());
		return new WhenNodeWrapper(version, matchingVersions, 0);
	}

	@Override
	public ITimeAwareGraphNode getNext() throws Exception {
		if (matchingVersionPosition > 0) {
			final ITimeAwareGraphNode version = original.travelInTime(matchingVersions.get(matchingVersionPosition - 1));
			return new WhenNodeWrapper(version, matchingVersions, matchingVersionPosition - 1);
		} else {
			return null;
		}
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsBetween(long fromInclusive, long toInclusive) throws Exception {
		final List<ITimeAwareGraphNode> results = new ArrayList<>();

		for (int i = 0; i < matchingVersions.size(); ++i) {
			final long instant = matchingVersions.get(i);
			if (instant > toInclusive) {
				// too recent, skip
			} else if (instant >= fromInclusive) {
				final ITimeAwareGraphNode version = original.travelInTime(instant);
				results.add(new WhenNodeWrapper(version, matchingVersions, i));
			} else {
				// after the end of the range, stop
				break;
			}
		}
		
		return results;
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsFrom(long fromInclusive) throws Exception {
		return getVersionsBetween(fromInclusive, getLatestInstant());
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsUpTo(long toInclusive) throws Exception {
		return getVersionsBetween(getEarliestInstant(), toInclusive);
	}
	
}
