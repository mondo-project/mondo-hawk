package org.hawk.epsilon.emc;

import java.util.Set;

import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.eol.models.IModel;
import org.hawk.core.graph.IGraphNode;

public class COptimisableCollection extends OptimisableCollection {

	protected static COptimisableCollectionSelectOperation indexedAttributeListSelectOperation;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COptimisableCollection(IModel m, GraphNodeWrapper t,
			Set<IGraphNode> files) {
		super(m, t);
		indexedAttributeListSelectOperation = new COptimisableCollectionSelectOperation(
				model.getBackend(), files);
	}

	@Override
	public AbstractOperation getAbstractOperation(String name) {
		if ("select".equals(name)) {
			return indexedAttributeListSelectOperation;
		} else
			return null;
	}

}
