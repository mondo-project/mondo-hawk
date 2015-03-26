/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
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
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkIterable;

public class OptimisableCollectionSelectOperation extends SelectOperation {

	protected EOLQueryEngine model;
	protected Collection<Object> modifiedlist;
	IEolContext context;
	boolean returnOnFirstMatch;

	IGraphDatabase graph = null;

	// IndexManager indexManager = null;

	@Override
	public Object execute(Object target, Variable iterator, Expression ast,
			IEolContext context, boolean returnOnFirstMatch)
			throws EolRuntimeException {

		modifiedlist = (OptimisableCollection) target;

		model = (EOLQueryEngine) ((OptimisableCollection) target)
				.getOwningModel();
		this.context = context;
		this.returnOnFirstMatch = returnOnFirstMatch;

		// Object ret =
		try {
			parseAST(iterator, ast);
		} catch (Exception e) {
			throw new EolRuntimeException(
					"OptimisableCollectionSelectOperation: parseAST(iterator, ast) failed:",
					ast);
		}

		return modifiedlist;

	}

	// @Override
	// public Object execute(Object target, Variable iterator, AST ast,
	// IEolContext context, boolean returnOnFirstMatch)
	// throws EolRuntimeException {
	//
	// return super
	// .execute(target, iterator, ast, context, returnOnFirstMatch);
	//
	// }

	/**
	 * Returns if this ast is optimisable
	 * 
	 * @param iterator
	 * @param ast
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected void parseAST(Variable iterator, AST ast) throws Exception {

		// Iterator<?> it = modifiedlist.iterator();
		// int count = 0;
		// while (it.hasNext()) {
		// NeoIdWrapperDebuggable item = ((NeoIdWrapperDebuggable) it.next());
		// if(count<5)System.err.print(item.getId() + ":" + item.getTypeName() +
		// " | ");
		// //else System.err.print(".");
		// count++;
		// }
		// System.err.println();
		// System.err.println(count);

		// System.out.println("parseAST:: printing ast:");
		// System.out.println(ast.toStringTree());

		// FIXME takes last argument of a complex statement first (a and b and c
		// -- it takes c)

		if (ast.getType() == EolParser.OPERATOR && ast.getText().equals("and")) {

			and(ast, iterator);

		}

		else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("or")) {

			or(ast, iterator);

		}

		else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("xor")) {

			// ( a or b ) and ( not(a and b) )

		}

		else if (ast.getType() == EolParser.OPERATOR
				&& ast.getText().equals("implies")) {

			// not(a) or b

		}

		else if (isOptimisable(ast, iterator)) {

			optimise(iterator, ast, true);

		} else {
			// System.err.println("giving to super: "+ast.toStringTree());
			modifiedlist.retainAll((Collection<Object>) super.execute(
					modifiedlist, iterator, (Expression) ast, context,
					returnOnFirstMatch));
			// System.err.println("super returns: "+rett.getClass());
		}

	}

	@SuppressWarnings("unchecked")
	private void or(AST ast, Variable iterator) throws Exception {

		// System.err.println("anding: "+ast.getFirstChild().toStringTree()+"\n"+ast.getChild(1).toStringTree());

		if (isOptimisable(ast.getFirstChild(), iterator)
				&& (isOptimisable(ast.getChild(1), iterator))) {

			Collection<Object> filter = new HashSet<Object>();

			filter.addAll(optimise(iterator, ast.getFirstChild(), false));
			filter.addAll(optimise(iterator, ast.getChild(1), false));

			modifiedlist.retainAll(filter);

		}

		else {

			modifiedlist.retainAll((Collection<Object>) super.execute(
					modifiedlist, iterator, (Expression) ast, context,
					returnOnFirstMatch));

		}

	}

	private void and(AST ast, Variable iterator) throws Exception {

		// System.err.println("anding: "+ast.getFirstChild().toStringTree()+"\n"+ast.getChild(1).toStringTree());
		boolean firstChild = false;
		boolean secondChild = false;

		if (isOptimisable(ast.getFirstChild(), iterator)) {
			firstChild = true;
			optimise(iterator, ast.getFirstChild(), true);
		}

		if (isOptimisable(ast.getChild(1), iterator)) {
			secondChild = true;
			optimise(iterator, ast.getChild(1), true);
		}

		if (firstChild && secondChild) {
		}

		else if (firstChild) {
			parseAST(iterator, ast.getChild(1));
		}

		else if (secondChild) {
			parseAST(iterator, ast.getFirstChild());
		}

		else {

			parseAST(iterator, ast.getFirstChild());
			parseAST(iterator, ast.getChild(1));

			// modifiedlist = (Collection<?>)
			// super.execute(modifiedlist,iterator, ast, context,
			// returnOnFirstMatch);

		}

	}

	@SuppressWarnings("unchecked")
	private Collection<Object> optimise(Variable iterator, AST ast,
			boolean alterlist) throws EolRuntimeException {

		// System.err.println(">"+ast.toStringTree());

		String attributename = ast.getFirstChild().getChild(1).getText();
		// int or boolean?
		Object attributevalue = null;

		AST sibling = ast.getFirstChild().getNextSibling();

		if (sibling.getType() == EolParser.BOOLEAN)
			attributevalue = sibling.getText().equals("true") ? true : false;
		else if (sibling.getType() == EolParser.INT)
			attributevalue = Integer.parseInt(sibling.getText());
		else
			attributevalue = sibling.getText();

		String indexname;

		if ((indexname = isIndexed(attributename)) != null) {

			System.err.println("indexed ast found: "
					+ ast.getFirstChild().getFirstChild().getText()
					+ ast.getFirstChild().getText() + attributename
					+ ast.getText() + attributevalue);

			// use index to query
			try (IGraphTransaction ignored = graph.beginTransaction()) {

				IGraphNodeIndex index = graph.getOrCreateNodeIndex(indexname);

				//
				IHawkIterable<IGraphNode> hits = index.get(attributename,
						attributevalue);

				// modifiedlist.clear();

				HashSet<Object> filter = new HashSet<Object>();

				for (IGraphNode hit : hits) {

					filter.add(new GraphNodeWrapper(hit.getId()
							.toString(), model));

					// ((OptimisableCollection) modifiedlist)
					// .add(new NeoIdWrapperDebuggable(graph, hit
					// .getId(), model));

				}

				if (alterlist)
					modifiedlist.retainAll(filter);
				else
					return filter;

				ignored.success();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// System.err.println(Arrays.toString(ret.toArray()));
			// return ret;

		} else {

			// System.err.println("giving to super: "+ast.toStringTree());

			Collection<Object> filter = (Collection<Object>) super.execute(
					modifiedlist, iterator, (Expression) ast, context,
					returnOnFirstMatch);

			if (alterlist)
				modifiedlist.retainAll(filter);
			else
				return filter;
			// System.err.println("super returns: "+rett.getClass());
			// return rett;

		}

		return null;

	}

	private boolean isOptimisable(AST ast, Variable iterator) {

		return ast.getType() == EolParser.OPERATOR
				&& (ast.getText().equals("=") || ast.getText().equals("=="))
				&& ast.getFirstChild().getType() == EolParser.POINT
				&& ast.getFirstChild().getFirstChild().getText()
						.equals(iterator.getName());

	}

	private String isIndexed(String attributename) {

		try {

			// indexname = ...
			if (graph == null)
				graph = model.getBackend();

			try (IGraphTransaction ignored = graph.beginTransaction()) {

				IGraphNode metaclass = graph
						.getNodeById(((OptimisableCollection) modifiedlist).type
								.getId());

				String indexname = metaclass.getOutgoingWithType("epackage")
						.iterator().next().getEndNode().getProperty("id")
						+ "##"
						+ metaclass.getProperty("id")
						+ "##"
						+ attributename;

				// if (indexManager == null) indexManager = graph.index();

				// System.err.println(indexname);
				// System.err.println(graph.getNodeIndexNames());

				if (graph.nodeIndexExists(indexname))
					return indexname;

				ignored.success();
			}

		} catch (Exception e) {
			System.err
					.println("OptimisableCollectionSelectOperation, isIndexed, suppressed exception: "
							+ e.getCause());
			e.printStackTrace();
		}

		return null;

	}

}