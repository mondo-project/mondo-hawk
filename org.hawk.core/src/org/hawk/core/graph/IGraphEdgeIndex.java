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
package org.hawk.core.graph;

import org.hawk.core.model.IHawkIterable;

public interface IGraphEdgeIndex {

	String getName();

	IHawkIterable<IGraphEdge> query(String key, Object valueExpr);

	IHawkIterable<IGraphEdge> get(String key, Object valueExpr);

}
