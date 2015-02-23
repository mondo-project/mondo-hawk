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
import org.hawk.mondix.query.HawkContainmentQueryInstance;
import org.hawk.mondix.query.HawkQueryInstance;

import eu.mondo.mondix.core.IMondixRelation;

public class HawkContanmentMondixRelation implements IMondixRelation {

	HawkMondixInstance hawkMondixInstance = null;

	// TODO hint/prepare frequently filtered columns

	public HawkContanmentMondixRelation(HawkMondixInstance hawkMondixInstance) {
		this.hawkMondixInstance = hawkMondixInstance;
	}

	public HawkMondixInstance getIndexerInstance() {
		return hawkMondixInstance;
	}

	public String getName() {
		return "Containment";
	}

	public List<String> getColumns() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("containerId");
		ret.add("contentId");
		ret.add("direct");
		return ret;
	}

	public int getArity() {
		return 3;
	}

	public HawkQueryInstance openView() {
		return new HawkContainmentQueryInstance(this,
				hawkMondixInstance.getGraph());
	}

	public HawkQueryInstance openView(
			List<String> selectedColumnNames, Map<String, Object> filter) {
		return new HawkContainmentQueryInstance(hawkMondixInstance.getGraph(),
				selectedColumnNames, filter, this);
	}

}
