/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.operations.declarative.SelectOperation;
import org.eclipse.epsilon.eol.parse.EolParser;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;

public class OptimisableCollectionSelectOperation extends SelectOperation {

	// XXX can also index subsets of X.all as the context is kept. but this may
	// be counter-productive as you may end up doing a retain all on a list of a
	// couple of elements into an indexed result of millions

	protected EOLQueryEngine model;
	IEolContext context;
	boolean returnOnFirstMatch;
	Variable iterator;

	IGraphNode metaclass;
	IGraphDatabase graph = null;

	// @Override
	// public Object execute(Object target, Variable iterator, Expression ast,
	// IEolContext context, boolean returnOnFirstMatch)
	// throws EolRuntimeException {
	//
	// return super
	// .execute(target, iterator, ast, context, returnOnFirstMatch);
	//
	// }

	@Override
	public Object execute(Object target, Variable iterator, Expression ast,
			IEolContext context, boolean returnOnFirstMatch)
			throws EolRuntimeException {

		// System.err.println("OptimisableCollectionSelectOperation execute called!");

		try {

			this.context = context;
			// cannot guarantee correctness if returnOnFirstMatch is used
			this.returnOnFirstMatch = false;
			this.iterator = iterator;
			model = (EOLQueryEngine) ((OptimisableCollection) target)
					.getOwningModel();

			graph = model.getBackend();
			try (IGraphTransaction ignored = graph.beginTransaction()) {
				metaclass = graph
						.getNodeById(((OptimisableCollection) target).type
								.getId());
				ignored.success();
			} catch (Exception e) {
				e.printStackTrace();
				throw new EolRuntimeException(
						"OptimisableCollectionSelectOperation: parseAST(iterator, ast) failed:",
						ast);
			}

			return decomposeAST(target, ast);

		} catch (Exception e) {
			e.printStackTrace();
			throw new EolRuntimeException(
					"OptimisableCollectionSelectOperation: parseAST(iterator, ast) failed:",
					ast);
		}

	}

	@SuppressWarnings("unchecked")
	protected Collection<Object> decomposeAST(Object target, AST ast)
			throws Exception {

		if (ast.getType() == EolParser.OPERATOR && ast.getText().equals("and")) {
			return and(target, ast);
		} else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("or")) {
			return or(target, ast);
		} else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("xor")) {
			return xor(target, ast);
			// ( a or b ) and ( not(a and b) )
			// a != b
		} else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("implies")) {
			return implies(target, ast);
			// not(a) or b
			// not( a and not(b) )
		} else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("not")) {
			return not(target, ast);
		} else if (isOptimisable(ast)) {
			return optimisedExecution(target, ast);
		} else {
			// System.err.println("giving to super: "+ast.toStringTree());
			Object ret = super.execute(target, iterator, (Expression) ast,
					context, returnOnFirstMatch);
			// System.err.println("super returns: "+ret.getClass());
			return (Collection<Object>) ret;

		}

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> implies(Object target, AST ast) throws Exception {

		boolean a = isOptimisable(ast.getFirstChild());
		boolean b = isOptimisable(ast.getChild(1));

		HashSet<Object> filter = new HashSet<Object>();
		filter.addAll((Collection<Object>) target);

		if (a && b) {

			// not(a) or b
			Collection<Object> aa = optimisedExecution(target,
					ast.getFirstChild());
			Collection<Object> bb = optimisedExecution(target, ast.getChild(1));

			filter.removeAll(aa);
			filter.addAll(bb);
			return filter;

		}

		if (a) {

			// not( a and not(b) )
			Collection<Object> lhsResult = optimisedExecution(target,
					ast.getFirstChild());

			lhsResult.removeAll((Collection<Object>) decomposeAST(lhsResult,
					ast.getChild(1)));

			filter.removeAll(lhsResult);
			return filter;

		}

		if (b) {

			// not( not(b) and a )
			Collection<Object> lhsResult = optimisedExecution(target,
					ast.getChild(1));

			Collection<Object> notb = new HashSet<Object>();
			notb.addAll((Collection<Object>) target);
			notb.removeAll(lhsResult);

			filter.removeAll(decomposeAST(notb, ast.getFirstChild()));
			return filter;

		}

		else {

			// not(a) or b
			Collection<Object> aa = decomposeAST(target, ast.getFirstChild());
			Collection<Object> bb = decomposeAST(target, ast.getChild(1));

			filter.removeAll(aa);
			filter.addAll(bb);
			return filter;

		}

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> not(Object target, AST ast) throws Exception {

		HashSet<Object> filter = new HashSet<Object>();
		filter.addAll((Collection<Object>) target);

		if (isOptimisable(ast.getFirstChild())) {

			filter.removeAll(optimisedExecution(target, ast.getFirstChild()));

		}

		else {

			filter.removeAll(decomposeAST(target, ast.getFirstChild()));

		}

		return filter;

	}

	private Collection<Object> xor(Object target, AST ast) throws Exception {

		Collection<Object> filter = new HashSet<Object>();
		Collection<Object> a = null;
		Collection<Object> b = null;

		if (isOptimisable(ast.getFirstChild())
				&& (isOptimisable(ast.getChild(1)))) {

			a = optimisedExecution(target, ast.getFirstChild());
			b = optimisedExecution(target, ast.getChild(1));

		}

		else {

			a = decomposeAST(target, ast.getFirstChild());
			b = decomposeAST(target, ast.getChild(1));

		}

		// a or b
		filter.addAll(a);
		filter.addAll(b);

		// intersection of a and b
		a.retainAll(b);

		// a xor b
		filter.removeAll(a);

		//

		return filter;

	}

	private Collection<Object> or(Object target, AST ast) throws Exception {

		// System.err.println("anding: "+ast.getFirstChild().toStringTree()+"\n"+ast.getChild(1).toStringTree());

		Collection<Object> filter = new HashSet<Object>();

		if (isOptimisable(ast.getFirstChild())
				&& (isOptimisable(ast.getChild(1)))) {

			filter.addAll(optimisedExecution(target, ast.getFirstChild()));
			filter.addAll(optimisedExecution(target, ast.getChild(1)));

		}

		else {

			filter.addAll(decomposeAST(target, ast.getFirstChild()));
			filter.addAll(decomposeAST(target, ast.getChild(1)));

		}

		return filter;

	}

	private Collection<Object> and(Object target, AST ast) throws Exception {

		// System.err.println("anding: "+ast.getFirstChild().toStringTree()+"\n"+ast.getChild(1).toStringTree());

		boolean a = isOptimisable(ast.getFirstChild());
		boolean b = isOptimisable(ast.getChild(1));

		Collection<Object> filter = new HashSet<>();

		if (a && b) {
			filter.addAll(optimisedExecution(
					optimisedExecution(target, ast.getFirstChild()),
					ast.getChild(1)));
		} else if (a) {
			filter.addAll(decomposeAST(
					optimisedExecution(target, ast.getFirstChild()),
					ast.getChild(1)));
		} else if (b) {
			filter.addAll(decomposeAST(
					optimisedExecution(target, ast.getChild(1)),
					ast.getFirstChild()));
		} else {
			filter.addAll(decomposeAST(
					decomposeAST(target, ast.getFirstChild()), ast.getChild(1)));
			// modifiedlist = (Collection<?>)
			// super.execute(modifiedlist,iterator, ast, context,
			// returnOnFirstMatch);
		}

		return filter;

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> optimisedExecution(Object target, AST ast)
			throws EolRuntimeException {

		// System.err.println(">"+ast.toStringTree());

		String attributename = ast.getFirstChild().getChild(1).getText();
		// int or boolean?
		Object attributevalue = null;

		AST sibling = ast.getFirstChild().getNextSibling();

		if (sibling.getType() == EolParser.BOOLEAN)
			attributevalue = sibling.getText().equals("true") ? true : false;
		else if (sibling.getType() == EolParser.INT) {
			try {
				attributevalue = Integer.parseInt(sibling.getText());
			} catch (Exception e) {
				attributevalue = Long.parseLong(sibling.getText().substring(0,
						sibling.getText().length() - 1));
			}
		}
		// XXX epsilon uses float but real can be float or double
		else if (sibling.getType() == EolParser.FLOAT)
			attributevalue = Double.parseDouble(sibling.getText());
		else
			attributevalue = sibling.getText();

		String indexname;

		if ((indexname = isIndexed(attributename)) != null) {

			HashSet<Object> result = new HashSet<Object>();
			result.addAll((Collection<Object>) target);

			System.err.println("indexed ast found: "
					+ ast.getFirstChild().getFirstChild().getText()
					+ ast.getFirstChild().getText() + attributename
					+ ast.getText() + attributevalue + " (type:"
					+ attributevalue.getClass() + ")");

			HashSet<Object> filter = new HashSet<Object>();
			// use index to query
			try (IGraphTransaction ignored = graph.beginTransaction()) {

				IGraphNodeIndex index = graph.getOrCreateNodeIndex(indexname);

				//
				IGraphIterable<IGraphNode> hits = null;

				if (ast.getText().equals("=") || ast.getText().equals("=="))
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue,
								(int) attributevalue, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename,
								(long) attributevalue, (long) attributevalue,
								true, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename,
								(double) attributevalue,
								(double) attributevalue, true, true);
					else
						hits = index.get(attributename, attributevalue);
				else if (ast.getText().equals(">")) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue,
								Integer.MAX_VALUE, false, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename,
								(long) attributevalue, Long.MAX_VALUE, false,
								true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename,
								(double) attributevalue, Double.MAX_VALUE,
								false, true);
					else
						throw new EolRuntimeException(
								"> used with a non numeric value ("
										+ attributevalue.getClass() + ")");
				} else if (ast.getText().equals(">=")) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, (int) attributevalue,
								Integer.MAX_VALUE, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename,
								(long) attributevalue, Long.MAX_VALUE, true,
								true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename,
								(double) attributevalue, Double.MAX_VALUE,
								true, true);
					else
						throw new EolRuntimeException(
								"> used with a non numeric value ("
										+ attributevalue.getClass() + ")");
				} else if (ast.getText().equals("<")) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, Integer.MIN_VALUE,
								(int) attributevalue, true, false);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, Long.MIN_VALUE,
								(long) attributevalue, true, false);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, Double.MIN_VALUE,
								(double) attributevalue, true, false);
					else
						throw new EolRuntimeException(
								"> used with a non numeric value ("
										+ attributevalue.getClass() + ")");
				} else if (ast.getText().equals("<=")) {
					if (attributevalue instanceof Integer)
						hits = index.query(attributename, Integer.MIN_VALUE,
								(int) attributevalue, true, true);
					else if (attributevalue instanceof Long)
						hits = index.query(attributename, Long.MIN_VALUE,
								(long) attributevalue, true, true);
					else if (attributevalue instanceof Double)
						hits = index.query(attributename, Double.MIN_VALUE,
								(double) attributevalue, true, true);
					else
						throw new EolRuntimeException(
								"> used with a non numeric value ("
										+ attributevalue.getClass() + ")");
				}

				// modifiedlist.clear();

				for (IGraphNode hit : hits) {

					filter.add(new GraphNodeWrapper(hit.getId().toString(),
							model));

					// ((OptimisableCollection) modifiedlist)
					// .add(new NeoIdWrapperDebuggable(graph, hit
					// .getId(), model));

				}

				ignored.success();

			} catch (Exception e) {
				e.printStackTrace();
				throw new EolRuntimeException(
						"optimise(Object target, AST ast) crashed (see above)");

			}

			result.retainAll(filter);
			return result;
			// System.err.println(Arrays.toString(ret.toArray()));
			// return ret;

		} else {

			// System.err.println("giving to super: "+ast.toStringTree());

			return (Collection<Object>) super.execute(target, iterator,
					(Expression) ast, context, returnOnFirstMatch);

			// System.err.println("super returns: "+rett.getClass());
			// return rett;

		}

	}

	private boolean isOptimisable(AST ast) {

		// TODO extend to numeric operations -- need to adapt neo4j lucene for
		// all numbers

		try {

			return ast.getType() == EolParser.OPERATOR
					&& (ast.getText().equals("=") || ast.getText().equals("==")
							|| ast.getText().equals(">")
							|| ast.getText().equals(">=")
							|| ast.getText().equals("<") || ast.getText()
							.equals("<="))
					&& ast.getFirstChild().getType() == EolParser.POINT
					&& ast.getFirstChild().getFirstChild().getText()
							.equals(iterator.getName());

		} catch (Exception e) {
			return false;
		}

	}

	private String isIndexed(String attributename) {

		String result = null;

		try (IGraphTransaction ignored = graph.beginTransaction()) {

			String indexname = metaclass.getOutgoingWithType("epackage")
					.iterator().next().getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
					+ "##" + metaclass.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "##" + attributename;

			// if (indexManager == null) indexManager = graph.index();

			// System.err.println(indexname);
			// System.err.println(graph.getNodeIndexNames());

			if (graph.nodeIndexExists(indexname))
				result = indexname;

			ignored.success();

		} catch (Exception e) {
			System.err
					.println("OptimisableCollectionSelectOperation, isIndexed, suppressed exception: "
							+ e.getCause());
			e.printStackTrace();
		}

		return result;

	}
}