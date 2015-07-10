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
package org.hawk.mondix.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public abstract class HawkQueryInstance implements IMondixView {

	protected IGraphDatabase graph;
	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();
	private Map<String, Object> filter = new HashMap<String, Object>();

	public HawkQueryInstance(IMondixRelation rel, IGraphDatabase graph) {

		baseRelation = rel;
		this.graph = graph;

	}

	public HawkQueryInstance(IGraphDatabase graph, List<String> cols,
			Map<String, Object> f, IMondixRelation rel) {

		baseRelation = rel;
		this.graph = graph;
		if (cols != null)
			selectedColumnNames = cols;
		
		if (f != null)
		filter = f;

	}

	public IMondixRelation getBaseRelation() {

		return baseRelation;

	}

	public List<String> getSelectedColumnNames() {

		return selectedColumnNames;

	}

	public Map<String, Object> getFilter() {

		return filter;

	}

	public void dispose() {

		// TODO anything else to dispose?

		baseRelation = null;
		selectedColumnNames = null;
		filter = null;

	}

	public abstract int getCountOfTuples();

	public abstract Iterable<? extends List<?>> getAllTuples();

}
