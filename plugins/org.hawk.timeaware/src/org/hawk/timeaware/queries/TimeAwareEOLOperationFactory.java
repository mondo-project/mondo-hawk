/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
package org.hawk.timeaware.queries;


import org.eclipse.epsilon.eol.execute.operations.EolOperationFactory;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.timeaware.queries.operations.patterns.AlwaysReducer;
import org.hawk.timeaware.queries.operations.patterns.EventuallyAtLeastReducer;
import org.hawk.timeaware.queries.operations.patterns.EventuallyAtMostReducer;
import org.hawk.timeaware.queries.operations.patterns.EventuallyReducer;
import org.hawk.timeaware.queries.operations.patterns.NeverReducer;
import org.hawk.timeaware.queries.operations.patterns.VersionQuantifierOperation;
import org.hawk.timeaware.queries.operations.scopes.BoundedVersionQuantifierOperation;
import org.hawk.timeaware.queries.operations.scopes.EndingTimeAwareNodeWrapper;
import org.hawk.timeaware.queries.operations.scopes.StartingTimeAwareNodeWrapper;
import org.hawk.timeaware.queries.operations.scopes.VersionRangeOperation;
import org.hawk.timeaware.queries.operations.scopes.WhenAnnotatedOperation;
import org.hawk.timeaware.queries.operations.scopes.WhenOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended version of the EOL operation factory, adding a new set of first-order
 * operations over the versions of types and nodes.
 */
public class TimeAwareEOLOperationFactory extends EolOperationFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareEOLOperationFactory.class);

	private final EOLQueryEngine containerModel;

	public TimeAwareEOLOperationFactory(EOLQueryEngine model) {
		this.containerModel = model;
	}

	public EOLQueryEngine getContainerModel() {
		return containerModel;
	}

	@Override
	protected void createCache() {
		super.createCache();
		operationCache.put("always",
			new VersionQuantifierOperation(this::getContainerModel, new AlwaysReducer()));
		operationCache.put("never",
			new VersionQuantifierOperation(this::getContainerModel, new NeverReducer()));
		operationCache.put("eventually",
			new VersionQuantifierOperation(this::getContainerModel, new EventuallyReducer()));
		operationCache.put("eventuallyAtMost",
			new BoundedVersionQuantifierOperation(this::getContainerModel, (count -> new EventuallyAtMostReducer(count))));
		operationCache.put("eventuallyAtLeast",
			new BoundedVersionQuantifierOperation(this::getContainerModel, (count -> new EventuallyAtLeastReducer(count))));

		/*
		 * First-order operation that makes the target time-aware node travel to the
		 * first timepoint since the current timepoint where a particular predicate was
		 * true, if it exists. The returned time-aware node will only report the
		 * versions since that moment: any past history will be ignored.
		 *
		 * This means that if you want to search through the entire history of a node,
		 * you will have to use <code>node.earliest.since(...)</code>. This is done to
		 * make it easier to restrict the scope of the search in long histories.
		 *
		 * If no such timepoint exists, the operation will return an undefined value,
		 * which can be checked against with <code>.isDefined()</code>.
		 */
		operationCache.put("since",
			new VersionRangeOperation(this::getContainerModel,
				(original, match) -> new StartingTimeAwareNodeWrapper(match)));

		/*
		 * Variant of .since which does not include the matching version, implementing
		 * an exclusive starting range.
		 */
		operationCache.put("after",
			new VersionRangeOperation(this::getContainerModel,
				(original, version) -> {
					try {
						ITimeAwareGraphNode nextVersion = version.getNext();
						if (nextVersion != null) {
							return new StartingTimeAwareNodeWrapper(nextVersion);
						}
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
					}

					return null;
				}
		));

		/*
		 * First-order operation that returns a version of the current node which
		 * will only report versions up to and including the first timepoint for
		 * which the predicate is true. This implements a closed ending range.
		 *
		 * If no such timepoint exists, the operation will return an undefined value,
		 * which can be checked against with <code>.isDefined()</code>.
		 */
		operationCache.put("until",
			new VersionRangeOperation(this::getContainerModel,
				(original, version) -> new EndingTimeAwareNodeWrapper(original, version.getTime())
			));

		/*
		 * First-order operation that returns a version of the current node which
		 * will only report versions before (excluding) the first timepoint for
		 * which the predicate is true. This implements an open ending range.
		 *
		 * If no such timepoint exists, the operation will return an undefined value,
		 * which can be checked against with <code>isDefined()</code>.
		 */
		operationCache.put("before",
			new VersionRangeOperation(this::getContainerModel, (original, version) -> {
				try {
					final long prevInstant = version.getPreviousInstant();
					if (prevInstant != ITimeAwareGraphNode.NO_SUCH_INSTANT) {
						return new EndingTimeAwareNodeWrapper(original, prevInstant);
					}
				} catch (Exception e) {
					LOGGER.error("Could not retrieve previous instant for before", e);
				}

				return null;
			}));

		operationCache.put("when", new WhenOperation(this::getContainerModel));

		operationCache.put("whenAnnotated", new WhenAnnotatedOperation(this::getContainerModel));
	}

}
