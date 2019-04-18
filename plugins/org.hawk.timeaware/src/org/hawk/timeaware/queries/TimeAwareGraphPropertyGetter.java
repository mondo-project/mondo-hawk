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
package org.hawk.timeaware.queries;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.pgetters.GraphPropertyGetter;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.timeaware.queries.operations.declarative.EndingTimeAwareNodeWrapper;
import org.hawk.timeaware.queries.operations.declarative.StartingTimeAwareNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TimeAwareGraphPropertyGetter extends GraphPropertyGetter {

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareGraphPropertyGetter.class);

	protected TimeAwareGraphPropertyGetter(IGraphDatabase graph, EOLQueryEngine m) {
		super(graph, m);
	}

	@Override
	protected Object invokePredefined(String property, IGraphNode node) throws EolRuntimeException {
		final Object ret = super.invokePredefined(property, node);
		if (ret != null) {
			return ret;
		}

		final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) node;
		try {
			switch (property) {
			case "versions":
				return getVersions(taNode);
			case "latest":
				return new GraphNodeWrapper(taNode.getLatest(), model);
			case "earliest":
				return new GraphNodeWrapper(taNode.getEarliest(), model);
			case "previous":
			case "prev":
				final ITimeAwareGraphNode taPrevious = taNode.getPrevious();
				if (taPrevious == null) {
					return null;
				} else {
					return new GraphNodeWrapper(taPrevious, model);
				}
			case "next":
				final ITimeAwareGraphNode taNext = taNode.getNext();
				if (taNext == null) {
					return null;
				} else {
					return new GraphNodeWrapper(taNext, model);
				}
			case "time":
				return taNode.getTime();
			case "sinceThen": {
					final StartingTimeAwareNodeWrapper scoped = new StartingTimeAwareNodeWrapper(taNode);
					return new GraphNodeWrapper(scoped, model);
				}
			case "untilThen": {
				final EndingTimeAwareNodeWrapper scoped = new EndingTimeAwareNodeWrapper(taNode);
				return new GraphNodeWrapper(scoped, model);
			}
			case "afterThen": {
					ITimeAwareGraphNode nextVersion = taNode.getNext();
					if (nextVersion == null) {
						return null;
					} else {
						final StartingTimeAwareNodeWrapper scoped = new StartingTimeAwareNodeWrapper(nextVersion);
						return new GraphNodeWrapper(scoped, model);
					}
				}
			case "beforeThen": {
					ITimeAwareGraphNode prevVersion = taNode.getPrevious();
					if (prevVersion == null) {
						return null;
					} else {
						final EndingTimeAwareNodeWrapper scoped = new EndingTimeAwareNodeWrapper(prevVersion);
						return new GraphNodeWrapper(scoped, model);
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new EolRuntimeException(ex.getMessage());
		}

		return null;
	}

	private List<GraphNodeWrapper> getVersions(ITimeAwareGraphNode taNode) throws Exception {
		final List<GraphNodeWrapper> result = new ArrayList<>();
		for (ITimeAwareGraphNode version : taNode.getAllVersions()) {
			result.add(new GraphNodeWrapper(version, model));
		}
		return result;
	}
}