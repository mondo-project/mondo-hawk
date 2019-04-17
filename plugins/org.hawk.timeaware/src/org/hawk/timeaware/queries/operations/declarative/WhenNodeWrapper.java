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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node wrapper that only exposes a specific list of versions of that node,
 * starting at the wrapped version.
 */
public class WhenNodeWrapper extends AbstractTimeAwareNodeWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhenNodeWrapper.class);

	private List<Long> matchingVersions;

	/**
	 * Creates a new wrapper, which only exposes some versions from its current
	 * version onwards.
	 * 
	 * @param original         Node to be wrapped.
	 * @param matchingVersions Timepoints to be exposed, from newest to oldest.
	 */
	public WhenNodeWrapper(ITimeAwareGraphNode original, List<Long> matchingVersions) {
		super(original);

		this.matchingVersions = matchingVersions;

		assert !matchingVersions.isEmpty() : "At least one matching version should exist";
		assert matchingVersions.get(matchingVersions.size()	- 1) == original.getTime() : "Matching versions should be from newest to oldest, and the wrapped node should be the oldest matching version";
	}

	@Override
	public List<Long> getAllInstants() throws Exception {
		return matchingVersions;
	}

	@Override
	public long getEarliestInstant() throws Exception {
		return matchingVersions.get(matchingVersions.size() - 1);
	}

	@Override
	public long getPreviousInstant() throws Exception {
		return ITimeAwareGraphNode.NO_SUCH_INSTANT;
	}

	@Override
	public long getLatestInstant() throws Exception {
		return matchingVersions.get(0);
	}

	@Override
	public long getNextInstant() throws Exception {
		return matchingVersions.get(1);
	}

	@Override
	public ITimeAwareGraphNode travelInTime(long time) {
		try {
			if (time < getEarliestInstant()) {
				return null;
			}

			// Find the latest version before that time
			long matchingTime = getEarliestInstant();
			if (matchingVersions.size() > 1) {
				for (ListIterator<Long> itTimepoint = matchingVersions.listIterator(matchingVersions.size()); itTimepoint.hasPrevious(); ) {
					final long candidate = itTimepoint.previous();
					if (candidate < time) {
						matchingTime = candidate;
					} else {
						break;
					}
				}
			}

			return original.travelInTime(matchingTime);
		} catch (Exception ex) {
			LOGGER.error("Could not travel in time", ex);
		}

		return null;
	}

	@Override
	public List<Long> getInstantsBetween(long fromInclusive, long toInclusive) {
		final ListIterator<Long> itInstants = matchingVersions.listIterator(matchingVersions.size());
		final List<Long> results = new ArrayList<>();

		while (itInstants.hasPrevious()) {
			final long instant = itInstants.next();
			if (instant < fromInclusive) {
				// not there yet, but keep looking
			} else if (instant <= toInclusive) {
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
		int iEarliestMatching = matchingVersions.size();

		for (ListIterator<Long> itInstant = matchingVersions.listIterator(matchingVersions.size()); itInstant.hasPrevious(); ) {
			if (itInstant.previous() >= fromInclusive) {
				return matchingVersions.subList(0, iEarliestMatching);
			} else {
				--iEarliestMatching;
			}
		}

		return Collections.emptyList();
	}

	@Override
	public List<Long> getInstantsUpTo(long toInclusive) {
		int iLatestMatching = 0;

		for (Iterator<Long> itInstant = matchingVersions.iterator(); itInstant.hasNext(); ) {
			if (itInstant.next() <= toInclusive) {
				return matchingVersions.subList(iLatestMatching, matchingVersions.size());
			} else {
				++iLatestMatching;
			}
		}

		return Collections.emptyList();
	}

}
