/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
import org.hawk.mondix.query.HawkQueryInstance;
import org.hawk.mondix.query.HawkTypeQueryInstance;

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
public class HawkTypeMondixRelation implements IMondixRelation {

	private HawkMondixInstance hawk;

	public HawkTypeMondixRelation(HawkMondixInstance hawkMondixInstance) {
		hawk = hawkMondixInstance;
	}

	public IMondixInstance getIndexerInstance() {
		return hawk;
	}

	public String getName() {
		return "Type";
	}

	public List<String> getColumns() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("id");
		ret.add("name");
		ret.add("metamodelId");
		return ret;
	}

	public int getArity() {
		return 3;
	}

	public HawkQueryInstance openView() {
		return openView(null, null);
	}

	public HawkQueryInstance openView(
			List<String> selectedColumnNames, Map<String, Object> filter) {
		return new HawkTypeQueryInstance(hawk.getGraph(), selectedColumnNames,
				filter, this);
	}

}
