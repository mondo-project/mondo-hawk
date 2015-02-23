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

import java.util.HashSet;

import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.eol.execute.operations.declarative.IAbstractOperationContributor;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IModelElement;

public class OptimisableCollection extends HashSet<Object> implements
		IAbstractOperationContributor, IModelElement {

	protected EOLQueryEngine model;
	protected GraphNodeWrapper type = null;

	protected static OptimisableCollectionSelectOperation indexedAttributeListSelectOperation = new OptimisableCollectionSelectOperation();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OptimisableCollection(IModel m, GraphNodeWrapper t) {

		model = (EOLQueryEngine) m;

		if (type == null)
			type = t;

	}

	@Override
	public AbstractOperation getAbstractOperation(String name) {
		if ("select".equals(name)) {
			return indexedAttributeListSelectOperation;
		} else
			return null;
	}

	@Override
	public IModel getOwningModel() {
		return model;
	}

}
