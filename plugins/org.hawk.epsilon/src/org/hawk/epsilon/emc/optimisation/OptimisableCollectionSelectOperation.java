/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - improved error reporting
 ******************************************************************************/
package org.hawk.epsilon.emc.optimisation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.epsilon.eol.dom.AndOperatorExpression;
import org.eclipse.epsilon.eol.dom.EqualsOperatorExpression;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.GreaterEqualOperatorExpression;
import org.eclipse.epsilon.eol.dom.GreaterThanOperatorExpression;
import org.eclipse.epsilon.eol.dom.ImpliesOperatorExpression;
import org.eclipse.epsilon.eol.dom.LessEqualOperatorExpression;
import org.eclipse.epsilon.eol.dom.LessThanOperatorExpression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.NotEqualsOperatorExpression;
import org.eclipse.epsilon.eol.dom.NotOperatorExpression;
import org.eclipse.epsilon.eol.dom.OperatorExpression;
import org.eclipse.epsilon.eol.dom.OrOperatorExpression;
import org.eclipse.epsilon.eol.dom.PropertyCallExpression;
import org.eclipse.epsilon.eol.dom.XorOperatorExpression;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.operations.declarative.SelectOperation;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.Utils;
import org.hawk.epsilon.emc.AbstractHawkModel;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimisableCollectionSelectOperation extends SelectOperation {

	// XXX can also index subsets of X.all as the context is kept. but this may
	// be counter-productive as you may end up doing a retain all on a list of a
	// couple of elements into an indexed result of millions

	private static final Logger LOGGER = LoggerFactory.getLogger(OptimisableCollectionSelectOperation.class);

	protected EOLQueryEngine model;
	private IEolContext context;
	private boolean returnOnFirstMatch;
	private Variable iterator;

	IGraphNode metaclass;
	IGraphDatabase graph = null;

	@Override
	public Object execute(Object target, Variable iterator, Expression ast, IEolContext context,
			boolean returnOnFirstMatch) throws EolRuntimeException {

		try {

			this.context = context;
			// cannot guarantee correctness if returnOnFirstMatch is used
			this.returnOnFirstMatch = false;
			this.iterator = iterator;
			model = (EOLQueryEngine) ((OptimisableCollection) target).getModel();

			graph = model.getBackend();
			try (IGraphTransaction ignored = graph.beginTransaction()) {
				metaclass = ((OptimisableCollection) target).type.getNode();
				ignored.success();
			}

			return decomposeAST(target, ast);
		} catch (Exception e) {
			throw new EolRuntimeException("select(...) failed: " + e.getMessage(), ast);
		}
	}

	@SuppressWarnings("unchecked")
	protected Collection<Object> decomposeAST(Object target, Expression ast) throws Exception {

		if (ast instanceof AndOperatorExpression) {
			return and(target, (AndOperatorExpression) ast);
		} else if (ast instanceof OrOperatorExpression) {
			return or(target, (OrOperatorExpression) ast);
		} else if (ast instanceof XorOperatorExpression) {
			return xor(target, (XorOperatorExpression) ast);
			// ( a or b ) and ( not(a and b) )
			// a != b
		} else if (ast instanceof ImpliesOperatorExpression) {
			return implies(target, (ImpliesOperatorExpression) ast);
			// not(a) or b
			// not( a and not(b) )
		} else if (ast instanceof NotOperatorExpression) {
			return not(target, (NotOperatorExpression) ast);
		} else if (isOptimisable(ast)) {
			return optimisedExecution(target, ast);
		} else {
			// System.err.println("giving to super: "+ast.toStringTree());
			Object ret = super.execute(target, iterator, (Expression) ast, context, returnOnFirstMatch);
			// System.err.println("super returns: "+ret.getClass());
			return (Collection<Object>) ret;

		}

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> implies(Object target, ImpliesOperatorExpression ast) throws Exception {

		final Expression lOperand = ast.getFirstOperand();
		final Expression rOperand = ast.getSecondOperand();
		final boolean lOptimisable = isOptimisable(lOperand);
		final boolean rOptimisable = isOptimisable(rOperand);

		final Set<Object> filter = new HashSet<Object>();
		filter.addAll((Collection<Object>) target);

		if (lOptimisable && rOptimisable) {
			// not(a) or b
			Collection<Object> aa = optimisedExecution(target, lOperand);
			Collection<Object> bb = optimisedExecution(target, rOperand);

			filter.removeAll(aa);
			filter.addAll(bb);
			return filter;
		}
		else if (lOptimisable) {
			// not( a and not(b) )
			final Collection<Object> lhsResult = optimisedExecution(target, lOperand);
			lhsResult.removeAll((Collection<Object>) decomposeAST(lhsResult, rOperand));
			filter.removeAll(lhsResult);
			return filter;
		}
		else if (rOptimisable) {
			// not( not(b) and a )
			final Collection<Object> rhsResult = optimisedExecution(target, rOperand);
			final Collection<Object> notb = new HashSet<Object>((Collection<Object>) target);
			notb.removeAll(rhsResult);

			filter.removeAll(decomposeAST(notb, lOperand));
			return filter;
		}
		else {
			// not(a) or b
			final Collection<Object> aa = decomposeAST(target, lOperand);
			final Collection<Object> bb = decomposeAST(target, rOperand);
			filter.removeAll(aa);
			filter.addAll(bb);
			return filter;
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<Object> not(Object target, NotOperatorExpression ast) throws Exception {
		final Set<Object> filter = new HashSet<Object>((Collection<Object>) target);

		final Expression operand = ast.getFirstOperand();
		if (isOptimisable(operand)) {
			filter.removeAll(optimisedExecution(target, operand));
		}
		else {
			filter.removeAll(decomposeAST(target, operand));
		}

		return filter;
	}

	private Collection<Object> xor(Object target, XorOperatorExpression ast) throws Exception {
		Collection<Object> filter = new HashSet<Object>();
		Collection<Object> a = null;
		Collection<Object> b = null;

		final Expression lOperand = ast.getFirstOperand();
		final Expression rOperand = ast.getSecondOperand();
		if (isOptimisable(lOperand) && isOptimisable(rOperand)) {
			a = optimisedExecution(target, lOperand);
			b = optimisedExecution(target, rOperand);
		} else {
			a = decomposeAST(target, lOperand);
			b = decomposeAST(target, rOperand);
		}

		// a or b
		filter.addAll(a);
		filter.addAll(b);

		// intersection of a and b
		a.retainAll(b);

		// a xor b
		filter.removeAll(a);

		return filter;
	}

	private Collection<Object> or(Object target, OrOperatorExpression ast) throws Exception {
		// System.err.println("anding:
		// "+lOperand.toStringTree()+"\n"+rOperand.toStringTree());

		final Expression lOperand = ast.getFirstOperand();
		final Expression rOperand = ast.getSecondOperand();
		final Collection<Object> filter = new HashSet<Object>();

		if (isOptimisable(lOperand) && (isOptimisable(rOperand))) {
			filter.addAll(optimisedExecution(target, lOperand));
			filter.addAll(optimisedExecution(target, rOperand));
		} else {
			filter.addAll(decomposeAST(target, lOperand));
			filter.addAll(decomposeAST(target, rOperand));
		}

		return filter;
	}

	private Collection<Object> and(Object target, AndOperatorExpression ast) throws Exception {

		// System.err.println("anding:
		// "+lOperand.toStringTree()+"\n"+rOperand.toStringTree());

		final Expression lOperand = ast.getFirstOperand();
		final Expression rOperand = ast.getSecondOperand();
		final boolean lOptimisable = isOptimisable(lOperand);
		final boolean rOptimisable = isOptimisable(rOperand);

		final Collection<Object> filter = new HashSet<>();
		if (lOptimisable && rOptimisable) {
			filter.addAll(optimisedExecution(optimisedExecution(target, lOperand), rOperand));
		} else if (lOptimisable) {
			filter.addAll(decomposeAST(optimisedExecution(target, lOperand), rOperand));
		} else if (rOptimisable) {
			filter.addAll(decomposeAST(optimisedExecution(target, rOperand), lOperand));
		} else {
			filter.addAll(decomposeAST(decomposeAST(target, lOperand), rOperand));
			// modifiedlist = (Collection<?>)
			// super.execute(modifiedlist,iterator, ast, context,
			// returnOnFirstMatch);
		}

		return filter;

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> optimisedExecution(Object target, Expression ast) throws EolRuntimeException {

		// NOTE: this assumes that isOptimisable(ast) returned true
		final OperatorExpression opExp = (OperatorExpression) ast;
		final PropertyCallExpression lOperand = (PropertyCallExpression) opExp.getFirstOperand();
		final String attributename = lOperand.getPropertyNameExpression().getName();
		final String variableName = ((NameExpression) lOperand.getTargetExpression()).getName();

		final Expression valueAST = opExp.getSecondOperand();
		Object attributevalue = null;
		try {
			attributevalue = context.getExecutorFactory().execute(valueAST, context);
		} catch (Exception e) {
			// if the rhs is invalid or tries to use the iterator of the select
			// (which is outside its scope) -- default to epsilon's select
			LOGGER.warn("Warning: the RHS of the expression:\n{}"
					+ "\ncannot be evaluated using database indexing,\nas the iterator variable of the current select operation ({}) "
					+ "is not used in this process.\nDefaulting to Epsilon's select", ast, iterator.getName());
		}

		String indexname;
		if (attributevalue != null && (indexname = isIndexed(attributename)) != null) {
			if (!(attributevalue instanceof Collection<?>)) {
				attributevalue = AbstractHawkModel.toPrimitive(attributevalue);
			} else {
				Collection<?> cRet = (Collection<?>) attributevalue;
				Object[] aRet = new Object[cRet.size()];
				int count = 0;
				for (Iterator<?> it = cRet.iterator(); it.hasNext();) {
					aRet[count] = AbstractHawkModel.toPrimitive(it.next());
					count++;
				}
				// flatten to allow comparison to index value (which cannot be
				// multi-valued)
				attributevalue = Arrays.toString(aRet);
			}

			final Set<Object> result = new HashSet<Object>((Collection<Object>) target);
			System.err.println(String.format("indexed ast found: %s.%s %s %s (type: %s)",
					variableName, attributename, ast.getClass().getName(),
					new Utils().toString(attributevalue),
					attributevalue.getClass().getName()));

			final Set<Object> filter = new HashSet<Object>();

			// use index to query
			try (IGraphTransaction ignored = graph.beginTransaction()) {

				IGraphNodeIndex index = graph.getOrCreateNodeIndex(indexname);

				//
				IGraphIterable<? extends IGraphNode> hits = null;

				if (ast instanceof EqualsOperatorExpression && !(ast instanceof NotEqualsOperatorExpression)) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue, (int) attributevalue, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, (long) attributevalue, (long) attributevalue, true, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, (double) attributevalue, (double) attributevalue, true, true);
					else
						hits = index.get(attributename, attributevalue);
				} else if (ast instanceof GreaterEqualOperatorExpression) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue, Integer.MAX_VALUE, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, (long) attributevalue, Long.MAX_VALUE, true, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, (double) attributevalue, Double.MAX_VALUE, true, true);
					else
						throw new EolRuntimeException(
								">= used with a non numeric value (" + attributevalue.getClass() + ")");
				} else if (ast instanceof GreaterThanOperatorExpression) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue, Integer.MAX_VALUE, false, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, (long) attributevalue, Long.MAX_VALUE, false, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, (double) attributevalue, Double.MAX_VALUE, false, true);
					else
						throw new EolRuntimeException(
								"> used with a non numeric value (" + attributevalue.getClass() + ")");
				} else if (ast instanceof LessEqualOperatorExpression) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, Integer.MIN_VALUE, (int) attributevalue, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, Long.MIN_VALUE, (long) attributevalue, true, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, Double.MIN_VALUE, (double) attributevalue, true, true);
					else
						throw new EolRuntimeException(
								"<= used with a non numeric value (" + attributevalue.getClass() + ")");
				} else if (ast instanceof LessThanOperatorExpression) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, Integer.MIN_VALUE, (int) attributevalue, true, false);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, Long.MIN_VALUE, (long) attributevalue, true, false);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, Double.MIN_VALUE, (double) attributevalue, true, false);
					else
						throw new EolRuntimeException(
								"< used with a non numeric value (" + attributevalue.getClass() + ")");
				}

				// modifiedlist.clear();

				for (IGraphNode hit : hits) {

					filter.add(new GraphNodeWrapper(hit, model));

					// ((OptimisableCollection) modifiedlist)
					// .add(new NeoIdWrapperDebuggable(graph, hit
					// .getId(), model));

				}

				ignored.success();

			} catch (Exception e) {
				throw new EolRuntimeException("select optimisation failed: " + e.getMessage(), ast);
			}

			result.retainAll(filter);
			return result;
			// System.err.println(Arrays.toString(ret.toArray()));
			// return ret;

		} else {

			// System.err.println("giving to super: "+ast.toStringTree());

			return (Collection<Object>) super.execute(target, iterator, (Expression) ast, context, returnOnFirstMatch);

			// System.err.println("super returns: "+rett.getClass());
			// return rett;

		}

	}

	private boolean isOptimisable(Expression ast) {
		try {
			if (!(ast instanceof OperatorExpression)) {
				return false;
			}

			// LEFT - we should have iterator.property
			// L1. Check for a property call expression
			final OperatorExpression opExp = (OperatorExpression) ast;
			final Expression rawLOperand = opExp.getFirstOperand();
			if (!(rawLOperand instanceof PropertyCallExpression)) {
				return false;
			}
			final PropertyCallExpression lOperand = (PropertyCallExpression) rawLOperand;

			// L2. Check that we're using the iterator
			final Expression rawTargetExpression = lOperand.getTargetExpression();
			if (!(lOperand.getTargetExpression() instanceof NameExpression)) {
				return false;
			}
			final NameExpression nameExpression = (NameExpression) rawTargetExpression;
			if (!iterator.getName().equals(nameExpression.getName())) {
				return false;
			}

			// MIDDLE - we should be using a comparison operator (but not "!=").
			// Antonio: checking with Dimitris on subtype relationships in =/!=, </<= and >/>=
			return ast instanceof EqualsOperatorExpression && !(ast instanceof NotEqualsOperatorExpression)
					|| ast instanceof GreaterThanOperatorExpression
					|| ast instanceof LessThanOperatorExpression
					|| ast instanceof NotEqualsOperatorExpression
					|| ast instanceof GreaterEqualOperatorExpression
					|| ast instanceof LessEqualOperatorExpression
					;

		} catch (Exception e) {
			return false;
		}
	}

	private String isIndexed(String attributename) {
		String result = null;

		try (IGraphTransaction ignored = graph.beginTransaction()) {
			String indexname = metaclass.getOutgoingWithType("epackage").iterator().next().getEndNode()
					.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "##"
					+ metaclass.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "##" + attributename;

			// if (indexManager == null) indexManager = graph.index();
			// System.err.println(indexname);
			// System.err.println(graph.getNodeIndexNames());

			if (graph.nodeIndexExists(indexname))
				result = indexname;

			ignored.success();
		} catch (Exception e) {
			LOGGER.warn("Suppressed exception from isIndexed", e);
		}

		return result;
	}
}