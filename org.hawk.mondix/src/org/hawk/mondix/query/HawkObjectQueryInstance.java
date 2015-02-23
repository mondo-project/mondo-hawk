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
import org.hawk.mondix.relations.HawkFileMondixRelation;
import org.hawk.mondix.relations.HawkObjectMondixRelation;
import org.hawk.neo4j_v2.Neo4JDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkObjectQueryInstance extends HawkQueryInstance implements
IMondixView {

	protected IGraphDatabase graph;
	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();
	private Map<String, Object> filter = new HashMap<String, Object>();

	private HashSet<List<Object>> contents = null;

	String id = null;
	String typeId = null;
	Boolean direct = null;
	String fileId = null;
	private static IAbstractConsole console = new DefaultConsole();

	public static void main(String[] _) throws Exception {

		IGraphDatabase d = new Neo4JDatabase();
		d.run("test_db_single", new File(
				"D:/workspace/_hawk_runtime_example/runtime_data"), console);
		HawkObjectQueryInstance h = new HawkObjectQueryInstance(
				new HawkObjectMondixRelation(new HawkMondixInstance(d)), d);

		// try (IGraphTransaction t = h.graph.beginTransaction()) {
		//
		console.println("start");

		// h.id = "161";
		h.typeId = "110";
		h.direct = true;
		h.fileId = "119";

		h.getCountOfTuples();
		// h.getAllTuples();

		console.println("done");

		// ((FileOutputConsole) console).flush();
	}

	public HawkObjectQueryInstance(IMondixRelation rel, IGraphDatabase graph) {

		super(rel, graph);
		baseRelation = rel;
		this.graph = graph;
		selectedColumnNames = baseRelation.getColumns();

	}

	public HawkObjectQueryInstance(IGraphDatabase graph, List<String> cols,
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
		typeId = (String) filter.get("typeId");
		direct = (Boolean) filter.get("direct");
		fileId = (String) filter.get("fileId");

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
			// IGraphNodeIndex fileindex = graph.getFileIndex();

			if (id == null && typeId == null && direct != null
					&& fileId != null) {
				IGraphNode file = graph.getNodeById(fileId);
				for (IGraphEdge e : file.getIncomingWithType("file")) {
					if (direct)
						addToContents(e.getStartNode().getId() + "", e
								.getStartNode().getOutgoingWithType("typeOf")
								.iterator().next().getEndNode().getId()
								+ "", direct, fileId);
					else {
						addToContents(e.getStartNode().getId() + "", e
								.getStartNode().getOutgoingWithType("typeOf")
								.iterator().next().getEndNode().getId()
								+ "", direct, fileId);
						for (IGraphEdge ee : e.getStartNode()
								.getOutgoingWithType("kindOf")) {
							addToContents(e.getStartNode().getId() + "", ee
									.getEndNode().getId() + "", direct, fileId);
						}
					}
				}
			} else if (id != null && typeId == null && direct != null
					&& fileId == null) {
				IGraphNode object = graph.getNodeById(id);
				if (direct)
					addToContents(id, object.getOutgoingWithType("typeOf")
							.iterator().next().getEndNode().getId()
							+ "", direct, object.getOutgoingWithType("file")
							.iterator().next().getEndNode().getId()
							+ "");
				else {
					addToContents(id, object.getOutgoingWithType("typeOf")
							.iterator().next().getEndNode().getId()
							+ "", direct, object.getOutgoingWithType("file")
							.iterator().next().getEndNode().getId()
							+ "");
					for (IGraphEdge ee : object.getOutgoingWithType("kindOf")) {
						addToContents(id, ee.getEndNode().getId() + "", direct,
								object.getOutgoingWithType("file").iterator()
										.next().getEndNode().getId()
										+ "");
					}
				}
			} else if (id == null && typeId != null && direct != null
					&& fileId == null) {

				IGraphNode type = graph.getNodeById(typeId);

				if (direct) {
					for (IGraphEdge e : type.getIncomingWithType("typeOf"))
						addToContents(e.getStartNode().getId() + "", typeId,
								direct,
								e.getStartNode().getOutgoingWithType("file")
										.iterator().next().getEndNode().getId()
										+ "");
				} else {
					for (IGraphEdge e : type.getIncomingWithType("typeOf"))
						addToContents(e.getStartNode().getId() + "", typeId,
								direct,
								e.getStartNode().getOutgoingWithType("file")
										.iterator().next().getEndNode().getId()
										+ "");
					for (IGraphEdge e : type.getIncomingWithType("kindOf"))
						addToContents(e.getStartNode().getId() + "", typeId,
								direct,
								e.getStartNode().getOutgoingWithType("file")
										.iterator().next().getEndNode().getId()
										+ "");
				}

			} else if (id == null && typeId != null && direct != null
					&& fileId != null) {

				IGraphNode type = graph.getNodeById(typeId);
				// System.err.println(type.getProperty("id"));

				if (direct) {
					// System.err.println(id + typeId + direct + fileId);
					for (IGraphEdge e : type.getIncomingWithType("typeOf")) {
						if (e.getStartNode().getOutgoingWithType("file")
								.iterator().next().getEndNode().getId()
								.toString().equals(fileId))
							addToContents(e.getStartNode().getId() + "",
									typeId, direct, fileId);
					}
				} else {
					for (IGraphEdge e : type.getIncomingWithType("typeOf"))
						if (e.getStartNode().getOutgoingWithType("file")
								.iterator().next().getEndNode().getId()
								.toString().equals(fileId))
							addToContents(e.getStartNode().getId() + "",
									typeId, direct, fileId);
					for (IGraphEdge e : type.getIncomingWithType("kindOf"))
						if (e.getStartNode().getOutgoingWithType("file")
								.iterator().next().getEndNode().getId()
								.toString().equals(fileId))
							addToContents(e.getStartNode().getId() + "",
									typeId, direct, fileId);
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

	private void addToContents(Object id1, Object id2, Object id3, Object id4) {

		// Long id, String uri
		List<Object> list = new LinkedList<Object>();

		if (selectedColumnNames.contains("id"))
			list.add(id1);
		if (selectedColumnNames.contains("typeId"))
			list.add(id2);
		if (selectedColumnNames.contains("direct"))
			list.add(id3);
		if (selectedColumnNames.contains("fileId"))
			list.add(id4);

		console.println(list.toString());

		contents.add(list);

	}

}
