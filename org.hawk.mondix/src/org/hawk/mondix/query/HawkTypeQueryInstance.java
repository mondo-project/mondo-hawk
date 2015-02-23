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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.hawk.mondix.HawkMondixInstance;
import org.hawk.mondix.relations.HawkTypeMondixRelation;
import org.hawk.neo4j_v2.Neo4JDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkTypeQueryInstance extends HawkQueryInstance implements
IMondixView {

	protected IGraphDatabase graph;
	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();
	private Map<String, Object> filter = new HashMap<String, Object>();

	private HashSet<List<Object>> contents = null;

	String id = null;
	String name = null;
	String metamodelId = null;
	private static IAbstractConsole console = new DefaultConsole();

	public static void main(String[] _) throws Exception {

		IGraphDatabase d = new Neo4JDatabase();
		d.run("test_db_single", new File(
				"D:/workspace/_hawk_runtime_example/runtime_data"), console);
		HawkTypeQueryInstance h = new HawkTypeQueryInstance(
				new HawkTypeMondixRelation(new HawkMondixInstance(d)), d);

		// try (IGraphTransaction t = h.graph.beginTransaction()) {
		//
		console.println("start");

		h.metamodelId = "0";
		h.name = "MethodDeclaration";

		h.getCountOfTuples();
		// h.getAllTuples();

		console.println("done");

		// ((FileOutputConsole) console).flush();
	}

	public HawkTypeQueryInstance(IMondixRelation rel, IGraphDatabase graph) {

		super(rel, graph);
		baseRelation = rel;
		this.graph = graph;
		selectedColumnNames = baseRelation.getColumns();

	}

	public HawkTypeQueryInstance(IGraphDatabase graph, List<String> cols,
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

		id = (String) filter.get("id");
		name = (String) filter.get("name");
		metamodelId = (String) filter.get("metamodelId");

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
		contents = null;

	}

	public int getCountOfTuples() {

		int ret = 0;

		if (contents == null)
			setContents();

		ret = contents.size();

		return ret;

	}

	public Iterable<? extends List<?>> getAllTuples() {

		if (contents == null)
			setContents();

		return contents;

	}

	private void setContents() {

		contents = new HashSet<>();

		try (IGraphTransaction t = graph.beginTransaction()) {
			//

			if (id == null && name == null && metamodelId != null) {

				IGraphNode metamodel = graph.getNodeById(metamodelId);
				for (IGraphEdge n : metamodel.getIncomingWithType("epackage"))
					addToContents(n.getStartNode().getId() + "", n
							.getStartNode().getProperty("id"), metamodelId);

			} else if (id == null && name != null && metamodelId != null) {

				IGraphNode metamodel = graph.getNodeById(metamodelId);
				for (IGraphEdge n : metamodel.getIncomingWithType("epackage")) {
					IGraphNode type = n.getStartNode();
					if (type.getProperty("id").equals(name)) {
						addToContents(type.getId() + "", name, metamodelId);
					}
				}

			} else {// do nothing, not supported
			}
			t.success();
		} catch (Exception e) {
			System.err
					.println("exception in IGraphTransaction beginTransaction():");
			e.printStackTrace();
		}
	}

	private void addToContents(Object id1, Object id2, String id3) {

		// Long id, String uri
		List<Object> list = new LinkedList<Object>();

		if (selectedColumnNames.contains("id"))
			list.add(id1);
		if (selectedColumnNames.contains("name"))
			list.add(id2);
		if (selectedColumnNames.contains("metamodelId"))
			list.add(id3);

		console.println(list.toString());

		contents.add(list);

	}

}
