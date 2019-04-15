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
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.FrameStack;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.operations.declarative.FirstOrderOperation;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * First-order operation that makes the target time-aware node travel to the
 * first timepoint since the current timepoint where a particular predicate was
 * true, if it exists. The returned time-aware node will only report the
 * versions since that moment: any past history will be ignored.
 * </p>
 * 
 * <p>
 * This means that if you want to search through the entire history of a node,
 * you will have to use <code>node.earliest.since(...)</code>. This is done to
 * make it easier to restrict the scope of the search in long histories.
 * </p>
 * 
 * <p>
 * If no such timepoint exists, the operation will return an undefined value,
 * which can be checked against with <code>.isDefined()</code>.
 * </p>
 */
public class SinceOperation extends FirstOrderOperation {

	private class SinceTimeAwareNodeWrapper implements ITimeAwareGraphNode {

		// TODO add unwrapping step and unwrapping tests in the quantifiers (e.g. combine when+always and nested until)

		/**
		 * The original time aware graph node that is being wrapped. This may
		 * be a wrapper itself, allowing us to compose operations.
		 */
		private ITimeAwareGraphNode original;

		public SinceTimeAwareNodeWrapper(ITimeAwareGraphNode original) {
			this.original = original;
		}

		@Override
		public Object getId() {
			return original.getId();
		}

		@Override
		public Set<String> getPropertyKeys() {
			return original.getPropertyKeys();
		}

		@Override
		public Object getProperty(String name) {
			return original;
		}

		@Override
		public void setProperty(String name, Object value) {
			original.setProperty(name, value);
		}

		@Override
		public Iterable<IGraphEdge> getEdges() {
			return original.getEdges();
		}

		@Override
		public Iterable<IGraphEdge> getEdgesWithType(String type) {
			return original.getEdgesWithType(type);
		}

		@Override
		public Iterable<IGraphEdge> getOutgoingWithType(String type) {
			return original.getOutgoingWithType(type);
		}

		@Override
		public Iterable<IGraphEdge> getIncomingWithType(String type) {
			return original.getIncomingWithType(type);
		}

		@Override
		public Iterable<IGraphEdge> getIncoming() {
			return original.getIncoming();
		}

		@Override
		public Iterable<IGraphEdge> getOutgoing() {
			return original.getOutgoing();
		}

		@Override
		public void delete() {
			original.delete();
		}

		@Override
		public IGraphDatabase getGraph() {
			return original.getGraph();
		}

		@Override
		public void removeProperty(String name) {
			original.removeProperty(name);
		}

		@Override
		public boolean isAlive() {
			return original.isAlive();
		}

		@Override
		public long getTime() {
			return original.getTime();
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
		public void end() {
			original.end();
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

	private static final Logger LOGGER = LoggerFactory.getLogger(SinceOperation.class);

	private final Supplier<EOLQueryEngine> containerModelSupplier;

	public SinceOperation(Supplier<EOLQueryEngine> containerModelSupplier) {
		this.containerModelSupplier = containerModelSupplier;
	}

	@Override
	public Object execute(Object target, Variable iterator, Expression expression, IEolContext context) throws EolRuntimeException {
		if (target == null) {
			LOGGER.warn("always called on null value, returning false");
			return false;
		}
		if (!(target instanceof GraphNodeWrapper)) {
			LOGGER.warn("always called on non-node {}, returning false", target.getClass().getName());
			return false;
		}
		final GraphNodeWrapper gnw = (GraphNodeWrapper)target;

		if (!(gnw.getNode() instanceof ITimeAwareGraphNode)) {
			LOGGER.warn("always called on non-timeaware node {}, returning false", target.getClass().getName());
			return false;
		}
		final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) gnw.getNode();
 
		try {
			final long latestInstant = taNode.getLatestInstant();
			System.out.println("Latest instant is " + latestInstant);
			final List<ITimeAwareGraphNode> versions = taNode.getVersionsBetween(taNode.getTime(), latestInstant);
			final FrameStack scope = context.getFrameStack();

			for (ListIterator<ITimeAwareGraphNode> itVersion = versions.listIterator(versions.size()); itVersion.hasPrevious(); ) {
				final ITimeAwareGraphNode version = itVersion.previous();
				final GraphNodeWrapper listItem = new GraphNodeWrapper(version, containerModelSupplier.get());

				if (iterator.getType()==null || iterator.getType().isKind(listItem)){
					scope.enterLocal(FrameType.UNPROTECTED, expression);

					scope.put(Variable.createReadOnlyVariable(iterator.getName(), listItem));
					Object bodyResult = context.getExecutorFactory().execute(expression, context);
					scope.leaveLocal(expression);

					if (bodyResult instanceof Boolean && (boolean)bodyResult) {
						return new GraphNodeWrapper(new SinceTimeAwareNodeWrapper(version), containerModelSupplier.get());
					}
				}
			}
		} catch (Exception ex) {
			throw new EolInternalException(ex, expression);
		}

		return null;
	}

	@Override
	public boolean isOverridable() {
		return false;
	}

}
