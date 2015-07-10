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
import org.hawk.mondix.relations.HawkContanmentMondixRelation;
import org.hawk.neo4j_v2.Neo4JDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkContainmentQueryInstance extends HawkQueryInstance implements
IMondixView {

	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();

	private Map<String, Object> filter = new HashMap<String, Object>();

	HashSet<List<Object>> contents = null;

	Boolean direct = null;
	String containerId = null;
	String contentId = null;
	private static IAbstractConsole console = new DefaultConsole();

	public static void main(String[] _a) throws Exception {

		IGraphDatabase d = new Neo4JDatabase();
		d.run(new File(
				"D:/workspace/_hawk_runtime_example/runtime_data"), console);
		HawkContainmentQueryInstance h = new HawkContainmentQueryInstance(
				new HawkContanmentMondixRelation(new HawkMondixInstance(d)), d);

		// try (IGraphTransaction t = h.graph.beginTransaction()) {
		//
		console.println("start");

		// h.containerId = 49267L;
		h.contentId = "49267";
		// h.direct=false;

		h.getAllTuples();

		console.println("done");

		// ((FileOutputConsole) console).flush();
	}

	public HawkContainmentQueryInstance(IMondixRelation rel,
			IGraphDatabase graph) {

		super(rel, graph);
		this.graph = graph;
		baseRelation = rel;
		selectedColumnNames = baseRelation.getColumns();

	}

	public HawkContainmentQueryInstance(IGraphDatabase graph,
			List<String> cols, Map<String, Object> f, IMondixRelation rel) {

		super(graph, cols, f, rel);
		this.graph = graph;
		baseRelation = rel;
		if (cols != null)
			selectedColumnNames = cols;
		else {
			selectedColumnNames = baseRelation.getColumns();
		}

		if (f != null)
			filter = f;

		direct = (boolean) filter.get("direct");
		containerId = (String) filter.get("containerId");
		contentId = (String) filter.get("contentId");

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

		// Long containerId, Long contentId, Boolean direct
		contents = new HashSet<>();

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

			t.success();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(contents);
			// ((FileOutputConsole) console).flush();
		}

	}

	private void directContainments() {

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
					addToContents(containerId, e.getEndNode().getId() + "",
							true);
				}
			}

		} else if (contentId != null) {

			IGraphNode n = graph.getNodeById(contentId);
			for (IGraphEdge e : n.getIncoming()) {
				if (e.getProperty("isContainment") != null) {
					addToContents(e.getStartNode().getId() + "", contentId,
							true);
				}
			}

		} else {

			for (IGraphNode n : graph.allNodes(null)) {
				for (IGraphEdge e : n.getOutgoing()) {
					if (e.getProperty("isContainment") != null) {
						addToContents(e.getStartNode().getId() + "", e
								.getEndNode().getId() + "", true);
					}
				}

			}

		}

	}

	private void addToContents(Object id1, Object id2, boolean d) {

		// Long containerId, Long contentId, Boolean direct
		List<Object> list = new LinkedList<Object>();

		if (selectedColumnNames.contains("containerId"))
			list.add(id1);
		if (selectedColumnNames.contains("contentId"))
			list.add(id2);
		if (selectedColumnNames.contains("direct"))
			list.add(d);

		console.println(list.toString());

		contents.add(list);

	}

	private HashSet<IGraphNode> nextContainDepth(IGraphNode n) {

		HashSet<IGraphNode> ret = new HashSet<>();

		for (IGraphEdge e : n.getOutgoing()) {
			if (e.getProperty("isContainment") != null)
				ret.add(e.getEndNode());
		}
		return ret;
	}

	private HashSet<IGraphNode> nextContainedDepth(IGraphNode n) {

		HashSet<IGraphNode> ret = new HashSet<>();

		for (IGraphEdge e : n.getIncoming()) {
			if (e.getProperty("isContainment") != null)
				ret.add(e.getStartNode());
		}
		return ret;
	}

	private void indirectContainments() {

		// FIXME indirect including direct?

		if (containerId != null && contentId != null) {

			HashSet<IGraphNode> firstlevelcontainednodes = nextContainDepth(graph
					.getNodeById(containerId));

			for (IGraphNode fn : firstlevelcontainednodes) {

				HashSet<IGraphNode> secondlevelcontainednodes = nextContainDepth(fn);

				HashSet<IGraphNode> nlevelcontainnodes = secondlevelcontainednodes;
				boolean nextDepth = nlevelcontainnodes.iterator().hasNext();
				HashSet<IGraphNode> temp = new HashSet<>();
				while (nextDepth) {
					for (IGraphNode sn : nlevelcontainnodes) {

						if (sn.getId().equals(contentId))
							addToContents(containerId, contentId, false);

						temp.addAll(nextContainDepth(sn));

					}
					nlevelcontainnodes = temp;
					temp.clear();
					nextDepth = nlevelcontainnodes.iterator().hasNext();
				}
			}
		} else if (containerId != null) {

			HashSet<IGraphNode> firstlevelcontainednodes = nextContainDepth(graph
					.getNodeById(containerId));

			for (IGraphNode fn : firstlevelcontainednodes) {

				HashSet<IGraphNode> secondlevelcontainednodes = nextContainDepth(fn);

				HashSet<IGraphNode> nlevelcontainnodes = secondlevelcontainednodes;
				boolean nextDepth = nlevelcontainnodes.iterator().hasNext();
				HashSet<IGraphNode> temp = new HashSet<>();
				while (nextDepth) {
					for (IGraphNode sn : nlevelcontainnodes) {

						addToContents(containerId, sn.getId() + "", false);

						temp.addAll(nextContainDepth(sn));

					}
					nlevelcontainnodes = temp;
					temp.clear();
					nextDepth = nlevelcontainnodes.iterator().hasNext();
				}
			}

		} else if (contentId != null) {

			HashSet<IGraphNode> firstlevelcontainednodes = nextContainedDepth(graph
					.getNodeById(contentId));

			for (IGraphNode fn : firstlevelcontainednodes) {

				HashSet<IGraphNode> secondlevelcontainednodes = nextContainedDepth(fn);

				HashSet<IGraphNode> nlevelcontainnodes = secondlevelcontainednodes;
				boolean nextDepth = nlevelcontainnodes.iterator().hasNext();
				HashSet<IGraphNode> temp = new HashSet<>();
				while (nextDepth) {
					for (IGraphNode sn : nlevelcontainnodes) {

						addToContents(sn.getId() + "", contentId, false);

						temp.addAll(nextContainedDepth(sn));

					}
					nlevelcontainnodes = temp;
					temp.clear();
					nextDepth = nlevelcontainnodes.iterator().hasNext();
				}
			}

		} else {

			for (IGraphNode n : graph.allNodes(null)) {

				HashSet<IGraphNode> firstlevelcontainednodes = nextContainDepth(n);

				for (IGraphNode fn : firstlevelcontainednodes) {

					HashSet<IGraphNode> secondlevelcontainednodes = nextContainDepth(fn);

					HashSet<IGraphNode> nlevelcontainnodes = secondlevelcontainednodes;
					boolean nextDepth = nlevelcontainnodes.iterator().hasNext();
					HashSet<IGraphNode> temp = new HashSet<>();
					while (nextDepth) {
						for (IGraphNode sn : nlevelcontainnodes) {

							addToContents(n.getId() + "", sn.getId() + "",
									false);

							temp.addAll(nextContainDepth(sn));

						}
						nlevelcontainnodes = temp;
						temp.clear();
						nextDepth = nlevelcontainnodes.iterator().hasNext();
					}
				}

			}
		}

	}

}
