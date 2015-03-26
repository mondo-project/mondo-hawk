package org.hawk.epsilon.emc;

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
					"COptimisableCollectionSelectOperation: parseAST(iterator, ast) failed:",
					ast);
		}

		try (IGraphTransaction t = graph.beginTransaction()) {
			// limit to files
			Iterator<Object> it = modifiedlist.iterator();
			while (it.hasNext()) {
				GraphNodeWrapper o = (GraphNodeWrapper) it.next();
				if (!files.contains(graph.getNodeById(o.getId())
						.getOutgoingWithType("file").iterator().next()
						.getEndNode()))
					modifiedlist.remove(o);
			}
			t.success();
		} catch (Exception e) {
			throw new EolRuntimeException(
					"execute failed in COptimisableCollectionSelectOperation");
		}

		return modifiedlist;

	}
	// if (files.contains(node.getOutgoingWithType("file").iterator().next()))

}
