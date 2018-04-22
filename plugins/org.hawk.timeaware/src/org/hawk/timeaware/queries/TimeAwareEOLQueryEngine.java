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

import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;

/**
 * Variant of {@link EOLQueryEngine} that exposes the time-aware nature of the
 * index through EOL.
 *
 * TODO: integrate a nicer "human time to seconds since epoch UTC" library.
 *
 * TODO: allow for revision-based times as well.
 */
public class TimeAwareEOLQueryEngine extends EOLQueryEngine {

	public class TimeAwareNodeOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof GraphNodeWrapper && ((GraphNodeWrapper)target).getNode() instanceof ITimeAwareGraphNode;
		}

		public boolean isAlive(String moment) {
			final ITimeAwareGraphNode taNode = getTimeAwareNode();

			long time;
			if ("now".equals(moment)) {
				time = System.currentTimeMillis() / 1_000;
			} else {
				throw new UnsupportedOperationException("Cannot understand '" + moment + "' yet");
			}

			return taNode.travelInTime(time) != null;
		}

		protected ITimeAwareGraphNode getTimeAwareNode() {
			return (ITimeAwareGraphNode) ((GraphNodeWrapper)target).getNode();
		}
	}

	public class TypeHistoryOperationContributor extends OperationContributor {
		@Override
		public boolean contributesTo(Object target) {
			return target instanceof EolModelElementType;
		}

		public List<GraphNodeWrapper> created(String range) throws Exception {
			final EolModelElementType eolType = (EolModelElementType)target;
			final List<IGraphNode> typeNodes = getTypeNodes(eolType.getTypeName());

			List<GraphNodeWrapper> ret = new ArrayList<>();
			for (IGraphNode node : typeNodes) {
				ITimeAwareGraphNode taNode = (ITimeAwareGraphNode)node;

				if ("anytime".equals(range)) {
					for (ITimeAwareGraphNode typeNodeVersion : taNode.getAllVersions()) {
						TypeNode typeNode = new TypeNode(typeNodeVersion);
						for (ModelElementNode instanceNode : typeNode.getAllInstances()) {
							final IGraphNode meNode = instanceNode.getNode();
							final GraphNodeWrapper gnw = new GraphNodeWrapper(meNode, (EOLQueryEngine)eolType.getModel());
							ret.add(gnw);
						}
					}
				} else {
					throw new UnsupportedOperationException("Cannot understand '" + range + "' yet");
				}
			}

			return ret;
		}
	}

	@Override
	protected IEolModule createModule() {
		final IEolModule module = super.createModule();

		module.getContext().getOperationContributorRegistry()
			.add(new TypeHistoryOperationContributor());
		module.getContext().getOperationContributorRegistry()
			.add(new TimeAwareNodeOperationContributor());

		return module;
	}
}
