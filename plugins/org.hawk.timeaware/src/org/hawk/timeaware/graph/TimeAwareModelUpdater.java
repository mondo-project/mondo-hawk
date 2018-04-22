/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.timeaware.graph;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.graph.updater.DeletionUtils;
import org.hawk.graph.updater.GraphModelInserter;
import org.hawk.graph.updater.GraphModelUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refines the {@link GraphModelUpdater} to keep the full history of the project
 * into the graph.
 */
public class TimeAwareModelUpdater extends GraphModelUpdater {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareModelUpdater.class);

	/**
	 * Variant of {@link DeletionUtils} which ends node timelines instead of
	 * deleting their entire history.
	 */
	public class SoftDeletionUtils extends DeletionUtils {
		public SoftDeletionUtils(IGraphDatabase graph) {
			super(graph);
		}

		@Override
		protected boolean delete(IGraphNode modelElement) {
			if (!modelElement.getEdges().iterator().hasNext()) {
				try {
					removeFromIndexes(modelElement);
					((ITimeAwareGraphNode)modelElement).end();
					return true;
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}

			return false;
		}
	}

	@Override
	protected GraphModelInserter createInserter() {
		return new GraphModelInserter(indexer, this::createDeletionUtils, typeCache) {
			@Override
			protected double calculateModelDeltaRatio(IGraphNode fileNode, boolean verbose) throws Exception {
				/*
				 * We want to always do a transactional update - batch update starts by removing
				 * every existing node, so we would lose track of the various versions of each
				 * model element.
				 */
				return 0;
			}
		};
	}

	@Override
	protected DeletionUtils createDeletionUtils() {
		return new SoftDeletionUtils(indexer.getGraph());
	}
}
