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
