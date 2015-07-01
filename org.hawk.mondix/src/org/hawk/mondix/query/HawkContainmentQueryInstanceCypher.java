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
import org.hawk.core.util.FileOutputConsole;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Transaction;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkContainmentQueryInstanceCypher extends HawkQueryInstance
		implements IMondixView {

	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();

	private Map<String, Object> filter = new HashMap<String, Object>();

	HashSet<List<Object>> contents = null;

	Neo4JDatabase neograph;

	Boolean direct = null;
	Long containerId = null;
	Long contentId = null;
	private static IAbstractConsole console = new FileOutputConsole();

	public static void main(String[] _a) throws Exception {

		Neo4JDatabase d = new Neo4JDatabase();
		d.run(new File(
				"D:/workspace/_hawk_runtime_example/runtime_data"), console);
		HawkContainmentQueryInstanceCypher h = new HawkContainmentQueryInstanceCypher(
				null, d);

		// try (IGraphTransaction t = h.graph.beginTransaction()) {
		//
		console.println("start");

		// h.containerId = 49267L;
		// h.contentId = 49267L;

		h.getAllTuples();

		console.println("done");

		((FileOutputConsole) console).flush();
	}

	public HawkContainmentQueryInstanceCypher(IMondixRelation rel,
			IGraphDatabase graph) {

		super(rel, graph);
		this.graph = graph;
		neograph = (Neo4JDatabase) graph;
		baseRelation = rel;

	}

	public HawkContainmentQueryInstanceCypher(IGraphDatabase graph,
			List<String> cols, Map<String, Object> f, IMondixRelation rel) {

		super(graph, cols, f, rel);
		this.graph = graph;
		baseRelation = rel;
		if (cols != null)
			selectedColumnNames = cols;
		
		if (f != null)
		filter = f;

		direct = (boolean) filter.get("direct");
		containerId = (Long) filter.get("containerId");
		contentId = (Long) filter.get("contentId");

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
		neograph = null;
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

		// Long containerId, Long contentId, Boolean direct

		try (IGraphTransaction t = graph.beginTransaction()) {

			if (direct == null) {
				directContainments();
				indirectContainments();
			} else {
				if (direct) {
					directContainments();
				} else {
					indirectContainments();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void _directContainments() {

		if (containerId != null && contentId != null) {

			IGraphNode n = graph.getNodeById(containerId);

			for (IGraphEdge e : n.getOutgoing()) {
				if (e.getProperty("isContainment") != null
						&& e.getEndNode().getId().equals(contentId)) {
					addToContents(containerId, contentId, true);
				}
			}
		} else if (containerId != null) {

			IGraphNode n = graph.getNodeById(containerId);
			for (IGraphEdge e : n.getOutgoing()) {
				if (e.getProperty("isContainment") != null) {
					addToContents(containerId, e.getEndNode().getId(), true);
				}
			}

		} else if (contentId != null) {

			IGraphNode n = graph.getNodeById(contentId);
			for (IGraphEdge e : n.getIncoming()) {
				if (e.getProperty("isContainment") != null) {
					addToContents(e.getStartNode().getId(), contentId, true);
				}
			}

		} else {

			for (IGraphNode n : graph.allNodes(null)) {
				for (IGraphEdge e : n.getOutgoing()) {
					if (e.getProperty("isContainment") != null) {
						addToContents(e.getStartNode().getId(), e.getEndNode()
								.getId(), true);
					}
				}

			}

		}

	}
	
	private void directContainments() {

		// FIXME NYI - cypher
		ExecutionEngine engine = new ExecutionEngine(neograph.getGraph());

		ExecutionResult result = null;
		// String rows = "";

		try (Transaction ignored = neograph.getGraph().beginTx()) {
			
			//Grabats
			result = engine
					.execute("START dom=node:METAMODELINDEX('id:"
							+ "*"
							+ "') "
							+ "MATCH dom<-[]-(td{id:'TypeDeclaration'})<-[:typeOf]-(node) "
							+ "MATCH node-[:bodyDeclarations]->(methodnode)-[:modifiers]->(modifiernode{public:true}) "
							+ "MATCH methodnode-[:modifiers]->({static:true}) "
							+ "MATCH node-[:name]->(nodename) "
							+ "MATCH methodnode-[:returnType]->()-[:name]->(returntypename) "
							+ "WHERE nodename.fullyQualifiedName=returntypename.fullyQualifiedName "
							+ "MATCH methodnode-[:name]->(methodnodename) "
							+ "RETURN DISTINCT nodename.fullyQualifiedName,methodnodename.fullyQualifiedName");

			ignored.success();
		}

		System.out.println(result.dumpToString());
		
	}

	private void addToContents(Object id1, Object id2, boolean d) {

		List<Object> list = new LinkedList<Object>();
		list.add(id1);
		list.add(id2);
		list.add(d);
		contents.add(list);

	}

	private void indirectContainments() {

		// FIXME NYI - cypher

	}

}
