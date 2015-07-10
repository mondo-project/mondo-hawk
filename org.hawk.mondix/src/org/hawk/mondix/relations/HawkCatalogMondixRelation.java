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
import org.hawk.mondix.query.HawkCatalogQueryInstance;
import org.hawk.mondix.query.HawkQueryInstance;

import eu.mondo.mondix.core.IMondixInstance;
import eu.mondo.mondix.core.IMondixRelation;

/**
 * Returns the catalog relation that lists all relations published by this
 * mondix instance.
 * 
 * <p>
 * The catalog relation is guaranteed to:
 * <ul>
 * <li>contain a column called "name" that indicates the names of each published
 * relation (including the catalog itself)
 * <li>have the empty string as name
 * </ul>
 * 
 * <p>
 * Equivalent to calling {@link #getBaseRelationByName(String)} with an empty
 * string parameter
 * 
 */
public class HawkCatalogMondixRelation implements IMondixRelation {

	HawkMondixInstance hawk;

	public HawkCatalogMondixRelation(HawkMondixInstance hawkMondixInstance) {
		hawk = hawkMondixInstance;
	}

	public IMondixInstance getIndexerInstance() {
		return hawk;
	}

	public String getName() {
		return "";
	}

	public List<String> getColumns() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("name");
		return ret;
	}

	public int getArity() {
		return 1;
	}

	public HawkQueryInstance openView() {
		return openView(null, null);
	}

	public HawkQueryInstance openView(
			List<String> selectedColumnNames, Map<String, Object> filter) {
		return new HawkCatalogQueryInstance(hawk.getGraph(),
				selectedColumnNames, filter, this);
	}

}
