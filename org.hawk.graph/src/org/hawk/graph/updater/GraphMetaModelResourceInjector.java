/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.updater;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.graph.IGraphChange;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;

public class GraphMetaModelResourceInjector {

	// integer array containing the current number of added elements:
	// (element,((ofType)M->MM)reference,((ofKind)M->MM)reference,(unset(M->M))reference)
	private int objectCount = 0;
	private int unset;
	// private long resourcememory;

	// private HashSet<Resource> metamodelResources;

	private IGraphDatabase graph;
	IGraphNodeIndex epackagedictionary;
	IGraphNodeIndex filedictionary;

	LinkedList<IGraphChange> changes = new LinkedList<>();
	LinkedList<IGraphChange> tempchanges = new LinkedList<>();

	long startTime;
	private HashSet<IHawkPackage> addedepackages = new HashSet<>();

	public GraphMetaModelResourceInjector(IGraphDatabase database,
			Set<IHawkMetaModelResource> set) {

		// resourcememory = Runtime.getRuntime().totalMemory() -
		// Runtime.getRuntime().freeMemory();

		startTime = System.nanoTime();

		graph = database;

		// hash = new Hashtable<EObject, Long>(8192);

		// dictionary = index.forNodes("dictionary", MapUtil.stringMap(
		// IndexManager.PROVIDER, "lucene", "type", "exact"));

		try {

			System.out.println("ADDING METAMODELS: ");
			System.out.print("ADDING: ");
			System.out.println(parseResource(set) + " METAMODEL NODES! (took ~"
					+ (System.nanoTime() - startTime) / 1000000000 + "sec)");

		} catch (Exception e) {
			e.printStackTrace();
		}

		// graph.shutdown();

	}

	public GraphMetaModelResourceInjector(IGraphDatabase database) {
		graph = database;
	}

	public void removeMetamodels(Set<IHawkMetaModelResource> set) {

		try (IGraphTransaction t = graph.beginTransaction()) {

			epackagedictionary = graph.getMetamodelIndex();
			filedictionary = graph.getFileIndex();

			Set<IGraphNode> epns = new HashSet<>();

			for (IHawkMetaModelResource metamodelResource : set) {

				// if (resourceset == null)
				// resourceset = metamodelResource.getResourceSet();

				HashSet<IHawkObject> children = metamodelResource
						.getAllContents();

				for (IHawkObject child : children) {

					// if (child.eIsProxy()) {
					// throw new Exception("FAILED. PROXY UNRESOLVED: "
					// + ((InternalEObject) child).eProxyURI()
					// .fragment());
					// }

					// add the element
					if (child instanceof IHawkPackage) {

						Iterator<IGraphNode> it = epackagedictionary.get("id",
								((IHawkPackage) child).getNsURI()).iterator();

						if (!it.hasNext()) {

							System.err.println("Metamodel: "
									+ ((IHawkPackage) child).getName()
									+ " with uri: "
									+ ((IHawkPackage) child).getNsURI()
									+ " not indexed. Nothing happened.");

						} else {

							IGraphNode epn = it.next();

							System.err.println("Removing metamodel: "
									+ ((IHawkPackage) child).getName()
									+ " with uri: "
									+ ((IHawkPackage) child).getNsURI());

							epns.add(epn);

						}

					}
				}
			}

			removeAll(epns);

			t.success();

		} catch (Exception e1) {
			System.err
					.println("error in removing metamodels (ALL removal changes reverted):");
			e1.printStackTrace();
		}

	}

	private void removeAll(Set<IGraphNode> epns) throws Exception {

		DeletionUtils del = new DeletionUtils(graph);

		for (IGraphNode epn : epns)
			for (IGraphEdge rel : epn.getIncomingWithType("dependency")) {
				System.err.println("dependency from: "
						+ rel.getStartNode().getProperty("id") + " to: "
						+ epn.getProperty("id"));

				// delete all dependent metamodels and models
				IGraphNode depmm = rel.getStartNode();
				del.delete(rel);
				epns.add(depmm);

			}

		Set<IGraphNode> files = new HashSet<>();

		for (IGraphNode n : epns)
			files.addAll(remove(n));

		for (IGraphNode file : files)
			del.delete(file);

	}

	private Set<IGraphNode> remove(IGraphNode epn) throws Exception {

		long start = System.currentTimeMillis();

		HashSet<IGraphNode> fileNodes = new HashSet<IGraphNode>();

		try (IGraphTransaction transaction = graph.beginTransaction()) {

			System.out.println("deleting nodes from metamodel: "
					+ epn.getProperty("id"));

			HashSet<IGraphNode> metaModelElements = new HashSet<IGraphNode>();
			HashSet<IGraphNode> modelElements = new HashSet<IGraphNode>();

			DeletionUtils del = new DeletionUtils(graph);

			for (IGraphEdge rel : epn.getIncomingWithType("epackage")) {
				metaModelElements.add(rel.getStartNode());
				del.delete(rel);
			}
			epackagedictionary.remove(epn);

			for (IGraphEdge rel : epn.getOutgoingWithType("dependency")) {
				del.delete(rel);
			}

			del.delete(epn);

			for (IGraphNode metamodelelement : metaModelElements) {
				for (IGraphEdge rel : metamodelelement
						.getIncomingWithType("typeOf")) {
					modelElements.add(rel.getStartNode());
					del.delete(rel);
				}
			}
			for (IGraphNode metamodelelement : metaModelElements) {
				for (IGraphEdge rel : metamodelelement
						.getIncomingWithType("kindOf")) {
					modelElements.add(rel.getStartNode());
					del.delete(rel);
				}
			}

			for (IGraphNode metaModelElement : metaModelElements)
				del.delete(metaModelElement);

			//
			for (IGraphNode modelElement : modelElements) {
				Iterator<IGraphEdge> it = modelElement.getOutgoingWithType(
						"file").iterator();
				if (it.hasNext()) {
					IGraphEdge e = it.next();
					fileNodes.add(e.getEndNode());
					del.delete(e);

				}

				del.dereference(modelElement);
			}

			for (IGraphNode modelElement : modelElements)
				del.delete(modelElement);

			transaction.success();
		}

		System.out.println("deleted all, took: "
				+ (System.currentTimeMillis() - start) / 1000 + "s"
				+ (System.currentTimeMillis() - start) / 1000 + "ms");

		return fileNodes;

	}

	/**
	 * Adds the resource to the graph according to whether it is a model or a
	 * metamodel resource
	 * 
	 * @param parseOption
	 * @param metamodelResources
	 * @param graph
	 * @return
	 */
	private int parseResource(Set<IHawkMetaModelResource> metamodels)
			throws Exception {

		// metamodelResources = metamodels;

		// EObject[] contents = (EObject[]) resource.getContents().toArray();

		// for (EObject content : contents) {
		// List<EObject> children = new LinkedList<EObject>();
		// children.add(content);

		// while (!children.isEmpty()) {
		// EObject child = children.remove(0);

		// children.addAll(0, child.eContents());

		try (IGraphTransaction t = graph.beginTransaction()) {

			epackagedictionary = graph.getMetamodelIndex();
			filedictionary = graph.getFileIndex();

			for (IHawkMetaModelResource metamodelResource : metamodels) {

				// if (resourceset == null)
				// resourceset = metamodelResource.getResourceSet();

				HashSet<IHawkObject> children = metamodelResource
						.getAllContents();

				for (IHawkObject child : children) {

					// if (child.eIsProxy()) {
					// throw new Exception("FAILED. PROXY UNRESOLVED: "
					// + ((InternalEObject) child).eProxyURI()
					// .fragment());
					// }

					// add the element
					if (child instanceof IHawkPackage)
						addEPackage((IHawkPackage) child, metamodelResource);
					// )
					// break;

				}
			}

			t.success();

		}

		for (IHawkPackage epackage : addedepackages) {

			try (IGraphTransaction t = graph.beginTransaction()) {

				// TreeIterator<EObject> children =
				// metamodelResource.getAllContents();

				boolean success = true;

				// while (children.hasNext()) {

				// EObject child = children.next();

				// add the element
				// if (!
				// if (child instanceof EPackage)success =
				// addEClasses((EPackage) child);
				success = addEClasses(epackage);
				// )
				// break;

				// }

				if (success) {
					t.success();
					t.close();
					changes.addAll(tempchanges);
					tempchanges.clear();
				} else {
					tempchanges.clear();
					t.failure();
					t.close();
					try (IGraphTransaction t2 = graph.beginTransaction()) {
						IGraphNode ePackageNode = epackagedictionary
								.get("id", epackage.getNsURI()).iterator()
								.next();
						epackagedictionary.remove(ePackageNode);
						ePackageNode.delete();
						t.success();
						t.close();
					} catch (Exception e) {
						System.err.println("e1");
						e.printStackTrace();
					}
				}

			} catch (Exception e) {
				System.err.println("e2");
				e.printStackTrace();
			}

		}

		try (IGraphTransaction t = graph.beginTransaction()) {

			HashMap<IGraphNode, IHawkMetaModelResource> map = new HashMap<>();

			// for (Node epackagenode : epackagedictionary.query("id", "*")) {
			for (IHawkPackage ePackage : addedepackages) {

				IGraphNode epackagenode = ((IGraphIterable<IGraphNode>) epackagedictionary
						.get("id", ePackage.getNsURI())).getSingle();
				// EPackage ePackage = addedepackages.get(epackagenode
				// .getProperty("id").toString());

				// add resource to package
				IHawkMetaModelResource res = ePackage
						.getResource()
						.getMetaModelResourceFactory()
						.createMetamodelWithSinglePackage(
								"resource_from_epackage_" + ePackage.getNsURI(),
								ePackage);
				// System.err.println(ePackage.getNsURI());
				map.put(epackagenode, res);

			}

			for (IGraphNode epackagenode : map.keySet()) {

				OutputStream output = new OutputStream() {
					private StringBuilder string = new StringBuilder();

					@Override
					public void write(int b) throws IOException {
						this.string.append((char) b);
					}

					@Override
					public String toString() {
						return this.string.toString();
					}
				};

				IHawkMetaModelResource res = map.get(epackagenode);

				res.save(output, new HashMap<>());

				//
				String s = output.toString();
				//

				// System.out.println(s);

				epackagenode.setProperty("resource", s);
				output.close();
				res = null;

			}

			t.success();
		}

		return objectCount;
	}

	private boolean addEClasses(IHawkPackage ePackage) {

		boolean success = true;

		for (IHawkClass child : ePackage.getClasses()) {

			if (!success)
				break;

			if (child instanceof IHawkClass)
				success = success && addMetaClass(child);

		}

		return success;

	}

	private void addEPackage(IHawkPackage ePackage,
			IHawkMetaModelResource metamodelResource) throws IOException {

		String uri = ePackage.getNsURI();

		if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {

			Map<String, Object> map4 = new HashMap<>();
			map4.put("id", uri);
			map4.put("type", metamodelResource.getMetaModelResourceFactory()
					.getType());

			IGraphNode epackagenode = graph.createNode(
					new HashMap<String, Object>(), "epackage");

			tempchanges.add(new GraphChangeImpl(true, IGraphChange.METAMODEL,
					epackagenode.getId().toString(), null, true));

			for (String s : map4.keySet()) {
				epackagenode.setProperty(s, map4.get(s));
			}

			epackagedictionary.add(epackagenode, "id", uri);

			addedepackages.add(ePackage);
			// System.err.println("added epackage: "+((ENamedElement)
			// eClass).getName());

		} else {
			System.err
					.println("metamodel: "
							+ (ePackage).getName()
							+ " ("
							+ uri
							+ ") already in store, updating it instead (NYI) -- doing nothing!");
			// add to a list called changed epackages etc?
			// XXX IDEA: handle metamodel updates -- not breaking = migrate --
			// else
			// do not and warn users
		}

	}

	/**
	 * 
	 * @param eClass
	 * @return the URI ID of an eClass
	 */
	public String getEObjectId(IHawkClass eClass) {
		return eClass.getPackageNSURI() + "/" + eClass.getName();
	}

	/**
	 * Creates a node in the graph database with the given eClass's attributes
	 * in it.
	 * 
	 * @param eObject
	 * @param id
	 * @return the Node
	 */
	private boolean createEClassNode(IHawkClass eClass, String id) {

		boolean success = true;

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);

		IGraphNode node = graph.createNode(new HashMap<String, Object>(),
				"eclass");

		tempchanges.add(new GraphChangeImpl(true, IGraphChange.TYPE, node
				.getId().toString(), null, true));

		// hash.put(eClass, node);

		IGraphNode node2 = ((IGraphIterable<IGraphNode>) epackagedictionary.get(
				"id", eClass.getPackageNSURI())).getSingle();

		// System.out.println(new
		// ToString().toString(epackagedictionary.query("id","*")));

		graph.createRelationship(node, node2, "epackage");

		for (IHawkClass e : eClass.getSuperTypes()) {

			String uri = null;

			if (e.isProxy())
				uri = e.proxyURI()
				// ((InternalEObject) e)
				// .eProxyURI()
				// .toString()
						.substring(0, e.proxyURI()
						// ((InternalEObject) e).eProxyURI().toString()
								.indexOf("#"));

			else
				uri = e.getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err
						.println("EClass "
								+ eClass.getName()
								+ "has supertype "
								+ e.getName()
								+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
								+ uri + " first");
				return false;
			} else {

				// dependancy to package
				if (!uri.equals(eClass.getPackageNSURI())) {

					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary
							.get("id", uri)).getSingle();

					boolean alreadythere = false;

					for (IGraphEdge r : node2.getOutgoingWithType("dependency"))
						if (r.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator()
							.hasNext()
							|| !alreadythere) {

						System.err.println("supertype dependancy from "
								+ eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage,
								"dependency");

					}
				}

			}

		}

		// System.err.println(eClass.getName()+":");
		for (IHawkAttribute e : eClass.getAllAttributes()) {

			String uri = null;

			if (e.isProxy())

				uri = e.proxyURI()
				// ((InternalEObject) e)
				// .eProxyURI()
				// .toString()
						.substring(0,
						// ((InternalEObject) e).eProxyURI().toString()
								e.proxyURI().indexOf("#"));

			else

				try {
					uri = e.getType().getPackageNSURI();
				} catch (Exception ex) {
					// attribute does not have a type - derived - other errors
					uri = eClass.getPackageNSURI();
				}

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err
						.println("EAttribute "
								+ e.getName()
								+ " has type "
								+ e.getType().getName()
								+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
								+ uri + " first");
				return false;
			} else {

				// dependancy to package
				if (!uri.equals(eClass.getPackageNSURI())) {

					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary
							.get("id", uri)).getSingle();

					boolean alreadythere = false;

					for (IGraphEdge r : node2.getOutgoingWithType("dependency"))
						if (r.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator()
							.hasNext()
							|| !alreadythere) {

						System.err.println("attribute dependancy from "
								+ eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage,
								"dependency");

					}
				}

			}

			// System.err.println("a : " + e.getName());

			String[] metadata = new String[6];
			metadata[0] = "a";
			metadata[1] = (e.isMany() ? "t" : "f");
			metadata[2] = (e.isOrdered() ? "t" : "f");
			metadata[3] = (e.isUnique() ? "t" : "f");
			if (e.getType() != null) {
				if (e.getType().getName() != null) {
					// System.err.println(e.getType().getName());
					metadata[4] = e.getType().getName();
				} else {
					System.err
							.println("warning: unknown (null) type NAME found in metamodel parsing into db for attribute: "
									+ e.getName() + "of type: " + e.getType());
					metadata[4] = "unknown";
				}
			} else {
				System.err
						.println("warning: unknown (null) type found in metamodel parsing into db for attribute: "
								+ e.getName());
				metadata[4] = "unknown";
			}
			// isIndexed
			metadata[5] = "f";
			map.put(e.getName(), metadata);

			// "a."+(e.isMany()?"t":"f")+"."+(e.isOrdered()?"t":"f")+"."+(e.isUnique()?"t":"f")

		}

		for (IHawkReference r : eClass.getAllReferences()) {

			String uri = null;

			if (r.isProxy())
				uri = r.proxyURI()
				// ((InternalEObject) r)
				// .eProxyURI()
				// .toString()
						.substring(0, r.proxyURI()
						// ((InternalEObject) r).eProxyURI().toString()
								.indexOf("#"));

			else
				uri = r.getType().getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err
						.println("EReference "
								+ r.getName()
								+ " has type "
								+ r.getType().getName()
								+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
								+ uri + " first");
				return false;
			} else {

				// dependancy to package
				if (!uri.equals(eClass.getPackageNSURI())) {

					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary
							.get("id", uri)).getSingle();

					boolean alreadythere = false;

					for (IGraphEdge rr : node2
							.getOutgoingWithType("dependency"))
						if (rr.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator()
							.hasNext()
							|| !alreadythere) {

						System.err.println("reference dependancy from "
								+ eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage,
								"dependency");

					}
				}

			}

			// System.err.println("r : "+r.getName());

			String[] metadata = new String[5];
			metadata[0] = "r";
			metadata[1] = (r.isMany() ? "t" : "f");
			metadata[2] = (r.isOrdered() ? "t" : "f");
			metadata[3] = (r.isUnique() ? "t" : "f");
			if (r.getType() != null) {
				if (r.getType().getName() != null) {
					// System.err.println(e.getType().getName());
					metadata[4] = r.getType().getName();
				} else {
					System.err
							.println("warning: unknown (null) type NAME found in metamodel parsing into db for attribute: "
									+ r.getName() + "of type: " + r.getType());
					metadata[4] = "unknown";
				}
			} else {
				System.err
						.println("warning: unknown (null) type found in metamodel parsing into db for attribute: "
								+ r.getName());
				metadata[4] = "unknown";
			}
			// metadata[4] = r.getType().getName();

			map.put(r.getName(), metadata);

			// map.put(r.getName(),"r."+(r.isMany()?"t":"f")+"."+(r.isOrdered()?"t":"f")+"."+(r.isUnique()?"t":"f"));

		}

		// n.field("nUri", ((EClass)child).getEPackage()
		// .getNsURI());

		// System.err.println(">>>"+map.keySet());

		for (String s : map.keySet()) {
			node.setProperty(s, map.get(s));
		}

		// System.err.println(node.getPropertyKeys());

		return success;
	}

	/**
	 * Creates a node with the eClass parameters, adds it to the metatracker and
	 * to the metacdictionary index
	 * 
	 * @param eClass
	 */
	private boolean addMetaClass(IHawkClass eClass) {

		// System.out.println(child);

		String id = (eClass).getName();

		objectCount++;

		return createEClassNode(eClass, id);

		// metacdictionary.add(node, "id", id);

	}

	public int getUnset() {
		return unset;
	}

	public void setUnset(int unset) {
		this.unset = unset;
	}

	/**
	 * Adds a new derived attribute to a type. Returns whether propagation to
	 * current instances of this type is necessary.
	 * 
	 * @param metamodeluri
	 * @param typename
	 * @param attributename
	 * @param isMany
	 * @param isOrdered
	 * @param isUnique
	 * @param attributetype
	 * @param derivationlanguage
	 * @param derivationlogic
	 * @param graph
	 * @return
	 */
	public static boolean addDerivedAttribute(String metamodeluri,
			String typename, String attributename, boolean isMany,
			boolean isOrdered, boolean isUnique, String attributetype,
			String derivationlanguage, String derivationlogic,
			IGraphDatabase graph) {

		boolean requiresPropagationToInstances = false;

		try (IGraphTransaction t = graph.beginTransaction()) {

			IGraphIterable<IGraphNode> ep = graph.getMetamodelIndex().get("id",
					metamodeluri);

			IGraphNode packagenode = null;

			if (ep.size() == 1) {
				packagenode = ep.getSingle();
			} else {
				throw new Exception("metamodel not found:" + metamodeluri);
			}

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty("id").equals(typename)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				System.err
						.println("type: "
								+ typename
								+ " in: "
								+ metamodeluri
								+ " does not exist, aborting operation: addDerivedAttribute");
			} else {
				// at least one instance already present so derived attribute
				// needs
				// to be reconfigured for each element already present (whether
				// the
				// derived attribute is new or existed already and is being
				// updated)
				if (typenode.getIncomingWithType("typeOf").iterator().hasNext()
						|| typenode.getIncomingWithType("kindOf").iterator()
								.hasNext())
					requiresPropagationToInstances = true;

				String[] metadata = new String[7];
				metadata[0] = "d";
				metadata[1] = (isMany ? "t" : "f");
				metadata[2] = (isOrdered ? "t" : "f");
				metadata[3] = (isUnique ? "t" : "f");
				metadata[4] = attributetype;
				metadata[5] = derivationlanguage;
				metadata[6] = derivationlogic;

				if (typenode.getProperty(attributename) != null) {
					System.err
							.println("attribute already derived, nothing happened!");
					requiresPropagationToInstances = false;
				} else {
					typenode.setProperty(attributename, metadata);
					System.err.println("derived attribute added: "
							+ metamodeluri + ":" + typename + " "
							+ attributename + "(isMany=" + isMany
							+ "|isOrdered=" + isOrdered + "|isUnique="
							+ isUnique + "|type=" + attributetype + ") "
							+ derivationlanguage + " # " + derivationlogic);
				}
			}
			t.success();

		} catch (Exception e1) {
			System.err
					.println("error in adding a derived attribute to the metamodel");
			e1.printStackTrace();
		}

		return requiresPropagationToInstances;
	}

	/**
	 * Adds a new indexed attribute to a type. Returns whether propagation to
	 * current instances of this type is necessary.
	 * 
	 * @param metamodeluri
	 * @param typename
	 * @param attributename
	 * @param graph
	 * @return
	 */
	public static boolean addIndexedAttribute(String metamodeluri,
			String typename, String attributename, IGraphDatabase graph) {

		boolean requiresPropagationToInstances = false;

		try (IGraphTransaction t = graph.beginTransaction()) {

			IGraphNode packagenode = graph.getMetamodelIndex()
					.get("id", metamodeluri).getSingle();

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty("id").equals(typename)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				System.err
						.println("type: "
								+ typename
								+ " in: "
								+ metamodeluri
								+ " does not exist, aborting operation: addIndexedAttribute");
			} else {

				// at least one instance already present so indexed attribute
				// needs
				// to be reconfigured for each element already present (whether
				// the
				// indexed attribute is new or existed already and is being
				// updated)

				String[] metadata = (String[]) typenode
						.getProperty(attributename);

				if (metadata == null) {
					System.err
							.println("attribute: "
									+ attributename
									+ " in: "
									+ metamodeluri
									+ "#"
									+ typename
									+ " does not exist, aborting operation: addIndexedAttribute");
				} else if (!metadata[0].equals("a")) {
					// System.err.println(Arrays.toString(metadata));
					System.err
							.println(metamodeluri
									+ "#"
									+ typename
									+ " is a reference not an attribute, aborting operation: addIndexedAttribute");
				} else {

					if (typenode.getIncomingWithType("typeOf").iterator()
							.hasNext()
							|| typenode.getIncomingWithType("kindOf")
									.iterator().hasNext())
						requiresPropagationToInstances = true;

					if (metadata.length == 6) {
						if (metadata[5] == "t") {
							System.err
									.println("attribute already indexed, nothing happened!");
							requiresPropagationToInstances = false;
						} else {
							metadata[5] = "t";
							typenode.setProperty(attributename, metadata);
							System.err.println("indexed attribute added: "
									+ metamodeluri + ":" + typename + " "
									+ attributename);
						}
					} else if (metadata.length == 7)
						System.err
								.println("derived attributes are already indexed, nothing happened.");
					else
						System.err
								.println("unknown exception in addIndexedAttribute of GraphMetamodelResourceInjector");
				}
			}
			t.success();
		} catch (Exception e) {
			System.err.println("error in adding an indexed attribute:");
			e.printStackTrace();
		}

		return requiresPropagationToInstances;

	}

	public List<IGraphChange> getChanges() {
		return changes;
	}

	public void clearChanges() {
		changes.clear();
	}

}
