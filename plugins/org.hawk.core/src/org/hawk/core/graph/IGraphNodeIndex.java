/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.graph;

import java.util.Map;

public interface IGraphNodeIndex {

	String getName();

	IGraphIterable<? extends IGraphNode> query(String key, Object valueOrPattern);

	IGraphIterable<? extends IGraphNode> query(String key, Number from, Number to,
			boolean fromInclusive, boolean toInclusive);

	IGraphIterable<? extends IGraphNode> get(String key, Object exactValue);

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
