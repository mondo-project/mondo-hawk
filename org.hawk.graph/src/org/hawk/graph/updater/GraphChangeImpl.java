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
package org.hawk.graph.updater;

import org.hawk.core.graph.IGraphChange;

public class GraphChangeImpl implements IGraphChange {

	// t:add f:remove
	private boolean changeType;
	// from interface: file | metamodel | type | instance | property | reference
	private String elementType;
	// id if node id::name if feature
	private String identifier;
	// non-null for prop or ref | prop is primitive | ref is id of target of ref
	private Object value;
	// transient changes will not trigger updates to derived attributes (for
	// example batch updates which already set derived attributes)
	private boolean trans;

	/**
	 * @param changeType
	 * @param elementType
	 * @param identifier
	 *            (id if node id::name if feature)
	 * @param value
	 *            (non-null for prop or ref | prop is primitive | ref is id of
	 *            target of ref)
	 * @param trans
	 *            (transient changes will not trigger updates to derived
	 *            attributes (for example batch updates which already set
	 *            derived attributes))
	 */
	public GraphChangeImpl(boolean changeType, String elementType,
			String identifier, Object value, boolean trans) {
		this.changeType = changeType;
		this.elementType = elementType;
		this.identifier = identifier;
		this.value = value;
		this.trans = trans;
	}

	public boolean getChangeType() {
		return changeType;
	}

	public String getElementType() {
		return elementType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "GraphChangeImpl: " + (changeType ? "ADD" : "REMOVE") + " : "
				+ elementType + " : " + identifier + " : " + value;
	}

	@Override
	public boolean isTransient() {
		return trans;
	}

}
