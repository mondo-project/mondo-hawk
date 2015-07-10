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
import java.util.Iterator;
import java.util.Set;

import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;

public class COptimisableCollectionSelectOperation extends
		OptimisableCollectionSelectOperation {

	Set<IGraphNode> files;
	IGraphDatabase graph;

	public COptimisableCollectionSelectOperation(IGraphDatabase graph,
			Set<IGraphNode> files) {
		this.files = files;
		this.graph = graph;
	}

	@Override
	public Object execute(Object target, Variable iterator, Expression ast,
			IEolContext context, boolean returnOnFirstMatch)
			throws EolRuntimeException {

		Collection<Object> filter = null;

		model = (EOLQueryEngine) ((OptimisableCollection) target)
				.getOwningModel();
		this.context = context;
		this.returnOnFirstMatch = returnOnFirstMatch;

		// Object ret =
		try {
			filter = decomposeAST(target, ast);
		} catch (Exception e) {
			throw new EolRuntimeException(
					"COptimisableCollectionSelectOperation: parseAST(iterator, ast) failed:",
					ast);
		}

		try (IGraphTransaction t = graph.beginTransaction()) {
			// limit to files
			Iterator<Object> it = filter.iterator();
			while (it.hasNext()) {
				GraphNodeWrapper o = (GraphNodeWrapper) it.next();
				if (!files.contains(graph.getNodeById(o.getId())
						.getOutgoingWithType("file").iterator().next()
						.getEndNode()))
					filter.remove(o);
			}
			t.success();
		} catch (Exception e) {
			throw new EolRuntimeException(
					"execute failed in COptimisableCollectionSelectOperation");
		}

		return filter;

	}
	// if (files.contains(node.getOutgoingWithType("file").iterator().next()))

}
