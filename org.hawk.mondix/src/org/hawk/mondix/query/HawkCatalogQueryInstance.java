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
package org.hawk.mondix.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkCatalogQueryInstance extends HawkQueryInstance implements
		IMondixView {

	protected IGraphDatabase graph;
	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();
	private Map<String, Object> filter = new HashMap<String, Object>();

	public HawkCatalogQueryInstance(IMondixRelation rel, IGraphDatabase graph) {

		super(rel, graph);
		baseRelation = rel;
		this.graph = graph;
		selectedColumnNames = baseRelation.getColumns();

	}

	public HawkCatalogQueryInstance(IGraphDatabase graph, List<String> cols,
			Map<String, Object> f, IMondixRelation rel) {

		super(graph, cols, f, rel);
		baseRelation = rel;
		this.graph = graph;
		if (cols != null)
			selectedColumnNames = cols;
		else
			selectedColumnNames = baseRelation.getColumns();
		
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
		
		graph = null;
		baseRelation = null;
		selectedColumnNames = null;
		filter = null;

	}

	public int getCountOfTuples() {
		return 7;
	}

	public Iterable<? extends List<?>> getAllTuples() {
		List<List<String>> ret = new LinkedList<List<String>>();

		if (selectedColumnNames.contains("name")) {

			ret.add(list(""));
			ret.add(list("Slot"));
			ret.add(list("Object"));
			ret.add(list("Containment"));
			ret.add(list("Type"));
			ret.add(list("Metamodel"));
			ret.add(list("File"));

		}
		return ret;
	}

	private List<String> list(String s) {
		List<String> ret = new LinkedList<String>();
		ret.add(s);
		return ret;
	}

}
