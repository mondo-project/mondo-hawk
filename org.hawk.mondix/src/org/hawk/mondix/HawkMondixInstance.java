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
package org.hawk.mondix;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.util.DefaultConsole;
import org.hawk.mondix.relations.HawkCatalogMondixRelation;
import org.hawk.mondix.relations.HawkContanmentMondixRelation;
import org.hawk.mondix.relations.HawkFileMondixRelation;
import org.hawk.mondix.relations.HawkMetamodelMondixRelation;
import org.hawk.mondix.relations.HawkObjectMondixRelation;
import org.hawk.mondix.relations.HawkSlotMondixRelation;
import org.hawk.mondix.relations.HawkTypeMondixRelation;
import org.hawk.neo4j_v2.Neo4JDatabase;

import eu.mondo.mondix.core.IMondixInstance;
import eu.mondo.mondix.core.IMondixRelation;
import eu.mondo.mondix.core.IUnaryView;

public class HawkMondixInstance implements IMondixInstance {

	private HashMap<String, IMondixRelation> baseRelations;
	private IGraphDatabase graph;

	private static IAbstractConsole console = new DefaultConsole();

	public static void main(String[] _a) {

		LinkedList<String> ret = new LinkedList<>();

		IGraphDatabase d = new Neo4JDatabase();
		d.run(new File(
				"../_hawk_runtime_example/runtime_data"), console);
		HawkMondixInstance i = new HawkMondixInstance(d);

		System.err.println(i.getCatalogRelation().openView()
				.getAllTuples());
		IMondixRelation mmrel = i.getBaseRelationByName("Metamodel");
		System.err.println(mmrel.openView().getAllTuples());
		IMondixRelation typerel = i.getBaseRelationByName("Type");
		Map<String, Object> filter = new HashMap<>();
		filter.put("metamodelId", "1");
		System.err.println(typerel.openView(null, filter)
				.getAllTuples());
		IMondixRelation objectrel = i.getBaseRelationByName("Object");
		filter = new HashMap<>();
		filter.put("typeId", "30");
		filter.put("direct", true);
		List<String> select = new LinkedList<>();
		select.add("id");
		Iterable<? extends List<?>> objects = objectrel.openView(
				select, filter).getAllTuples();
		// System.err.println(objects);
		// we got all instances of type declaration!
		// flatten
		HashSet<Object> typedeclarationids = new HashSet<>();
		for (Object o : objects)
			typedeclarationids.add(((List<Object>) o).get(0));

		// for each typedeclaration
		for (Object typedeclarationid : typedeclarationids) {

			IMondixRelation slotrel = i.getBaseRelationByName("Slot");

			// get name of the typedeclaration

			Iterable<? extends List<?>> tdstringname = null;

			// System.err.println(typedeclarationid);

			filter = new HashMap<>();
			filter.put("objectId", typedeclarationid);
			filter.put("name", "name");
			// give me all methoddeclarations
			Iterable<? extends List<?>> tdreturntypename = slotrel
					.openView(null, filter).getAllTuples();
			// System.err.println(">>" + tdreturntypename);
			if (tdreturntypename.iterator().hasNext()) {
				filter = new HashMap<>();
				filter.put("objectId", tdreturntypename.iterator().next()
						.get(2));
				filter.put("name", "fullyQualifiedName");
				// give me all methoddeclarations
				tdstringname = slotrel.openView(null, filter)
						.getAllTuples();
			}

			// give me all methoddeclarations

			filter = new HashMap<>();
			filter.put("objectId", typedeclarationid);
			filter.put("name", "bodyDeclarations");

			Iterable<? extends List<?>> bodyslots = slotrel.openView(
					null, filter).getAllTuples();
			// flatten
			HashSet<Object> methoddeclarationids = new HashSet<>();
			for (Object temp : bodyslots)
				methoddeclarationids.add(((List<Object>) temp).get(2));

			LinkedList<String> matchingmethods = new LinkedList<>();
			// get modifiers
			for (Object methoddeclarationid : methoddeclarationids) {
				filter = new HashMap<>();
				filter.put("objectId", methoddeclarationid);
				filter.put("name", "modifiers");
				// give me all modifiers of this method
				Iterable<? extends List<?>> modifierslots = slotrel
						.openView(null, filter).getAllTuples();

				// flatten
				HashSet<Object> modifierids = new HashSet<>();
				for (Object temp : modifierslots)
					modifierids.add(((List<Object>) temp).get(2));

				boolean isPublic = false;
				boolean isStatic = false;

				// get public and static modifiers
				for (Object modifierid : modifierids) {
					filter = new HashMap<>();
					filter.put("objectId", modifierid);
					filter.put("name", "static");
					Iterable<? extends List<?>> isstatic = slotrel
							.openView(null, filter).getAllTuples();

					for (List<?> s : isstatic)
						if (s.get(2).equals(true))
							isStatic = true;

					filter = new HashMap<>();
					filter.put("objectId", modifierid);
					filter.put("name", "public");
					Iterable<? extends List<?>> ispublic = slotrel
							.openView(null, filter).getAllTuples();

					for (List<?> p : ispublic)
						if (p.get(2).equals(true))
							isPublic = true;

				}

				// find return type if needed
				if (isStatic && isPublic) {

					// get name of the method

					Iterable<? extends List<?>> mdstringname = null;

					// System.err.println(r.get(2) + " :: " +
					// typedeclarationid + " : " + matchingmethods);

					filter = new HashMap<>();
					filter.put("objectId", methoddeclarationid);
					filter.put("name", "name");
					// give me all methoddeclarations
					Iterable<? extends List<?>> mdreturntypename = slotrel
							.openView(null, filter).getAllTuples();
					// System.err.println(">>" + tdreturntypename);
					if (mdreturntypename.iterator().hasNext()) {
						filter = new HashMap<>();
						filter.put("objectId", mdreturntypename.iterator()
								.next().get(2));
						filter.put("name", "fullyQualifiedName");
						// give me all methoddeclarations
						mdstringname = slotrel.openView(null, filter)
								.getAllTuples();
					}

					filter = new HashMap<>();
					filter.put("objectId", methoddeclarationid);
					filter.put("name", "returnType");
					// give me all methoddeclarations
					Iterable<? extends List<?>> returntypeslots = slotrel
							.openView(null, filter).getAllTuples();

					for (List<?> r : returntypeslots) {
						// System.err.println(r.get(2) + " :: " +
						// typedeclarationid + " : " + matchingmethods);

						filter = new HashMap<>();
						filter.put("objectId", r.get(2));
						filter.put("name", "name");
						// give me all methoddeclarations
						Iterable<? extends List<?>> returntypename = slotrel
								.openView(null, filter).getAllTuples();

						Iterable<? extends List<?>> returntypestringname = null;
						if (returntypename.iterator().hasNext()) {
							filter = new HashMap<>();
							filter.put("objectId", returntypename.iterator()
									.next().get(2));
							filter.put("name", "fullyQualifiedName");
							// give me all methoddeclarations
							returntypestringname = slotrel.openView(
									null, filter).getAllTuples();
						}

						if (returntypestringname != null
								&& tdstringname != null)
							if (returntypestringname
									.iterator()
									.next()
									.get(2)
									.equals(tdstringname.iterator().next()
											.get(2))) {
								if (mdstringname != null)
									matchingmethods.add((String) mdstringname
											.iterator().next().get(2));
								ret.add(returntypestringname.iterator().next()
										.get(2)
										+ " : " + matchingmethods);
							}
						// if (r.get(2).equals(typedeclarationid))
						// ret.add((String) typedeclarationid);
					}
				}

			}

		}

		// return singletons!
		System.err.println(ret);

	}

	public HawkMondixInstance(IGraphDatabase g) {

		graph = g;

		baseRelations = new HashMap<>();
		baseRelations.put("", new HawkCatalogMondixRelation(this));
		baseRelations.put("Slot", new HawkSlotMondixRelation(this));
		baseRelations.put("Object", new HawkObjectMondixRelation(this));
		baseRelations
				.put("Containment", new HawkContanmentMondixRelation(this));
		baseRelations.put("Type", new HawkTypeMondixRelation(this));
		baseRelations.put("Metamodel", new HawkMetamodelMondixRelation(this));
		baseRelations.put("File", new HawkFileMondixRelation(this));

	}

	public IMondixRelation getBaseRelationByName(String relationName) {
		return baseRelations.get(relationName);
	}

	public IMondixRelation getCatalogRelation() {
		return baseRelations.get("");
	}

	public IUnaryView getPublishedRelationNames() {
		IMondixRelation cat = getCatalogRelation();
		LinkedList<String> cols = new LinkedList<>();
		cols.add("name");
		return (IUnaryView) cat.openView(cols, null);
	}

	public IGraphDatabase getGraph() {
		return graph;
	}

}
