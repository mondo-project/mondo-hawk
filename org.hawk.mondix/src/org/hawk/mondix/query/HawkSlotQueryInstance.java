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
import java.io.Serializable;
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
import org.hawk.mondix.relations.HawkSlotMondixRelation;
import org.hawk.neo4j_v2.Neo4JDatabase;

import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IMondixView;

public class HawkSlotQueryInstance extends HawkQueryInstance implements
IMondixView {

	protected IGraphDatabase graph;
	private IMondixRelation baseRelation;
	protected List<String> selectedColumnNames = new LinkedList<String>();
	private Map<String, Object> filter = new HashMap<String, Object>();

	private HashSet<List<Object>> contents = null;

	String objectId = null;
	String name = null;
	Serializable value = null;
	Boolean isPrimitive = null;
	private static IAbstractConsole console = new DefaultConsole();

	public static void main(String[] _) throws Exception {

		IGraphDatabase d = new Neo4JDatabase();
		d.run(new File(
				"D:/workspace/_hawk_runtime_example/runtime_data"), console);
		HawkSlotQueryInstance h = new HawkSlotQueryInstance(
				new HawkSlotMondixRelation(new HawkMondixInstance(d)), d);

		// try (IGraphTransaction t = h.graph.beginTransaction()) {
		//
		console.println("start");

		h.objectId = "245";
		// h.name = "isStatic";
		h.isPrimitive = true;

		h.getCountOfTuples();
		// h.getAllTuples();

		console.println("done");

		// ((FileOutputConsole) console).flush();
	}

	public HawkSlotQueryInstance(IMondixRelation rel, IGraphDatabase graph) {

		super(rel, graph);
		baseRelation = rel;
		this.graph = graph;
		selectedColumnNames = baseRelation.getColumns();

	}

	public HawkSlotQueryInstance(IGraphDatabase graph, List<String> cols,
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

		objectId = (String) filter.get("objectId");
		name = (String) filter.get("name");
		value = (Serializable) filter.get("value");
		isPrimitive = (Boolean) filter.get("isPrimitive");

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

			// for (IGraphNode e : graph.allNodes())
			// if (e.getOutgoingWithType("returnType").iterator().hasNext()) {
			// System.err.print(e.getId() + " : ");
			// System.err.println(e.getOutgoingWithType("typeOf")
			// .iterator().next().getEndNode().getProperty("id"));
			// }

			// System.err.println(graph.getNodeById(2648)
			// .getOutgoingWithType("typeOf").iterator().next()
			// .getEndNode().getProperty("id"));

			if (objectId != null && name == null && value == null
					&& isPrimitive == null) {

				IGraphNode object = graph.getNodeById(objectId);

				for (String o : object.getPropertyKeys()) {
					if (!(o.equals("id") || o.equals("hashCode"))) {
						// System.err.print(o + " : ");
						// System.err.println("" + object.getProperty(o));
						addToContents(object.getId() + "", o,
								object.getProperty(o), true);
					}
				}
				// FIXME keep each reference in same tuple or different ones?
				for (IGraphEdge e : object.getOutgoing()) {
					if (!(e.getType().equals("typeOf")
							|| e.getType().equals("kindOf") || e.getType()
							.equals("file"))) {
						// System.err.println(e.getType());
						addToContents(object.getId() + "", e.getType(), e
								.getEndNode().getId() + "", false);
					}
				}

			} else if (objectId != null && name != null && value == null
					&& isPrimitive == null) {

				// System.err.println(objectId + " :: " + name);

				IGraphNode object = graph.getNodeById(objectId);

				for (String o : object.getPropertyKeys()) {
					if (!(o.equals("id") || o.equals("hashCode"))) {
						// System.err.print(o + " : ");
						// System.err.println("" + object.getProperty(o));
						if (o.equals(name))
							addToContents(object.getId() + "", o,
									object.getProperty(o), true);
					}
				}
				for (IGraphEdge e : object.getOutgoing()) {
					if (!(e.getType().equals("typeOf")
							|| e.getType().equals("kindOf") || e.getType()
							.equals("file"))) {
						// System.err.println(e.getType());
						if (e.getType().equals(name))
							addToContents(object.getId() + "", e.getType(), e
									.getEndNode().getId() + "", false);
					}
				}

			} else if (objectId != null && name == null && value == null
					&& isPrimitive != null) {

				IGraphNode object = graph.getNodeById(objectId);

				if (isPrimitive)
					for (String o : object.getPropertyKeys()) {
						if (!(o.equals("id") || o.equals("hashCode"))) {
							// System.err.print(o + " : ");
							// System.err.println("" + object.getProperty(o));
							addToContents(object.getId() + "", o,
									object.getProperty(o), true);
						}
					}
				else
					for (IGraphEdge e : object.getOutgoing()) {
						if (!(e.getType().equals("typeOf")
								|| e.getType().equals("kindOf") || e.getType()
								.equals("file"))) {
							// System.err.println(e.getType());
							addToContents(object.getId() + "", e.getType(), e
									.getEndNode().getId() + "", false);
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

	private void addToContents(Object id1, Object id2, Object id3, Object id4) {

		// Long id, String uri
		List<Object> list = new LinkedList<Object>();

		if (selectedColumnNames.contains("objectId"))
			list.add(id1);
		if (selectedColumnNames.contains("name"))
			list.add(id2);
		if (selectedColumnNames.contains("value"))
			list.add(id3);
		if (selectedColumnNames.contains("isPrimitive"))
			list.add(id4);

		console.println(list.toString());

		contents.add(list);

	}

}
