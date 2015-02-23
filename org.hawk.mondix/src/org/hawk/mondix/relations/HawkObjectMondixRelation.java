/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Bergmann Gabor		- mondix API
 ******************************************************************************/
package org.hawk.mondix.relations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.mondix.HawkMondixInstance;
import org.hawk.mondix.query.HawkObjectQueryInstance;
import org.hawk.mondix.query.HawkQueryInstance;

import eu.mondo.mondix.core.IMondixInstance;
import eu.mondo.mondix.core.IMondixRelation;

/**
 * Represents a base relation for which queries can be opened.
 * 
 * 
 * @param Tuple
 *            the tuple type of this relation
 * 
 */
public class HawkObjectMondixRelation implements IMondixRelation {

	private HawkMondixInstance hawk;

	public HawkObjectMondixRelation(HawkMondixInstance hawkMondixInstance) {
		hawk = hawkMondixInstance;
	}

	/**
	 * Returns the indexer instance that owns and maintains this base relation.
	 */
	public IMondixInstance getIndexerInstance() {
		return hawk;
	}

	/**
	 * Returns the name of this relation that uniquely identifies it within the
	 * indexer instance.
	 */
	public String getName() {
		return "Object";
	}

	/**
	 * Returns an ordered list of column names.
	 */
	public List<String> getColumns() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("id");
		ret.add("typeId");
		ret.add("direct");
		ret.add("fileId");
		return ret;
	}

	/**
	 * Returns the number of parameters/columns of this relation.
	 * <p>
	 * Equivalent to {@link #getColumns()}.size()
	 */
	public int getArity() {
		return 4;
	}

	public HawkQueryInstance openView() {
		return openView(null, null);
	}

	public HawkQueryInstance openView(
			List<String> selectedColumnNames, Map<String, Object> filter) {
		return new HawkObjectQueryInstance(hawk.getGraph(),
				selectedColumnNames, filter, this);
	}

}
