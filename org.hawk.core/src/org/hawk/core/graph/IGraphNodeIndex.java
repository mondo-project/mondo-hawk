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

import java.util.Map;

import org.hawk.core.model.IHawkIterable;

public interface IGraphNodeIndex {

	String getName();

	IHawkIterable<IGraphNode> query(String key, Object valueExpr);

	IHawkIterable<IGraphNode> get(String key, Object valueExpr);

	void add(IGraphNode n, Map<String, Object> derived);

	void add(IGraphNode n, String s, Object derived);

	void remove(IGraphNode n);

	void flush();

}
