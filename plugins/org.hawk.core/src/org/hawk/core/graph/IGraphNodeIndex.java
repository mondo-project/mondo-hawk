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
package org.hawk.core.graph;

import java.util.Map;

public interface IGraphNodeIndex {

	String getName();

	IGraphIterable<IGraphNode> query(String key, Object valueOrPattern);

	IGraphIterable<IGraphNode> query(String key, Number from, Number to,
			boolean fromInclusive, boolean toInclusive);

	IGraphIterable<IGraphNode> get(String key, Object exactValue);

	/**
	 * Associates a node with the index using a certain set of key-value pairs.
	 * Ignores null maps and null values.
	 */
	void add(IGraphNode n, Map<String, Object> values);

	/**
	 * Associates a node with the index using a certain key-value pair.
	 * Ignores null values.
	 */
	void add(IGraphNode n, String key, Object value);

	void remove(IGraphNode n);

	void remove(IGraphNode n, String key, Object value);

	void flush();

	void delete();

}
